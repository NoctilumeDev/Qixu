"""Independent public-packet reproduction. No Java code, database, identity or mutable seed lookup."""
from __future__ import annotations
import argparse
from datetime import datetime, timezone, timedelta
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time

VERSION = "qixu-max-cardinality-lottery-v2"
CHAIN = "52db9ba70e0cc0f6eaf7803dd07447a1f5477735fd3f661792ba94600c84e971"
VERIFIER = "drand-client/1.4.2:qixu-offline/0.1"
ROOT = Path(__file__).resolve().parents[1]


def digest(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def canonical(value: object) -> bytes:
    # This domain has only ASCII keys, integer numbers and Unicode strings.
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def reproduce(graph: dict, input_hash: str, randomness: str) -> dict:
    if graph.get("algorithm") != VERSION or not re.fullmatch("[0-9a-f]{64}", input_hash) or not re.fullmatch("[0-9a-f]{64}", randomness):
        raise ValueError("Unsupported algorithm or coordinates")
    seat_list, people = graph["seats"], graph["candidates"]
    if not isinstance(seat_list, list) or not isinstance(people, list) or len(seat_list) > 400 or len(people) > 200:
        raise ValueError("Unsupported domain bounds")
    seats = set(seat_list)
    if len(seats) != len(seat_list) or any(not isinstance(x, str) or not re.fullmatch("[A-Za-z0-9_-]{1,80}", x) for x in seats):
        raise ValueError("Invalid or duplicate resource")
    seed = digest((VERSION + "\0" + input_hash + "\0" + randomness).encode())
    edges = {}
    for person in people:
        who = person["id"]
        if not isinstance(who, str) or not re.fullmatch("[A-Za-z0-9_-]{1,80}", who) or who in edges:
            raise ValueError("Invalid or duplicate candidate")
        acceptable = person["acceptable"]
        if not isinstance(acceptable, list) or len({x["seat"] for x in acceptable}) != len(acceptable):
            raise ValueError("Invalid acceptable set")
        for edge in acceptable:
            if edge["seat"] not in seats or type(edge["rank"]) is not int or not 1 <= edge["rank"] <= 3:
                raise ValueError("Invalid hard constraint")
        edges[who] = sorted(acceptable, key=lambda e: (e["rank"], digest(("qixu-seat-order-v2\0" + seed + "\0" + who + "\0" + e["seat"]).encode()), e["seat"]))
    order = sorted(edges)
    counter, size = 0, 1 << 256
    for i in range(len(order) - 1, 0, -1):
        bound = i + 1
        while True:
            raw = b"qixu-permutation-v2\0" + bytes.fromhex(seed) + counter.to_bytes(8, "big")
            value = int.from_bytes(hashlib.sha256(raw).digest(), "big")
            counter += 1
            if value < size - size % bound:
                j = value % bound
                order[i], order[j] = order[j], order[i]
                break
    end = time.monotonic() + 20

    def maximum(candidates: list[str], available: set[str]) -> int:
        ownership: dict[str, str] = {}

        def augment(person: str, visited: set[str]) -> bool:
            if time.monotonic() >= end:
                raise TimeoutError("Independent compute budget exhausted")
            for edge in edges[person]:
                seat = edge["seat"]
                if seat in available and seat not in visited:
                    visited.add(seat)
                    if seat not in ownership or augment(ownership[seat], visited):
                        ownership[seat] = person
                        return True
            return False

        return sum(augment(person, set()) for person in candidates)

    available = set(seats)
    count = needed = maximum(order, available)
    assignments = []
    for i, person in enumerate(order):
        selected = None
        for edge in edges[person] if needed else []:
            seat = edge["seat"]
            if seat not in available:
                continue
            available.remove(seat)
            if 1 + maximum(order[i + 1:], available) >= needed:
                selected = edge
                needed -= 1
                break
            available.add(seat)
        if selected:
            assignments.append({"candidate": person, "seat": selected["seat"], "rank": selected["rank"], "reason": "PREFERRED_OFFER" if selected["rank"] == 1 else "AUTHORIZED_FALLBACK_OFFER"})
        else:
            if maximum(order[i + 1:], available) < needed:
                raise ValueError("Cardinality contradiction")
            assignments.append({"candidate": person, "seat": None, "rank": None, "reason": "FIXED_WAITLIST" if edges[person] else "NO_ACCEPTABLE_EDGE"})
    if needed:
        raise ValueError("Incomplete calculation")
    return {"algorithm": VERSION, "seedDigest": seed, "order": order, "maximum": count, "assignments": assignments}


def verify(packet: dict, node: str = "node") -> dict:
    packet = packet.get("data", packet)
    raw = packet["input_bytes"].encode("utf-8")
    if len(raw) > 4_194_304 or digest(raw) != packet["input_hash"]:
        raise ValueError("Input byte digest mismatch")
    snapshot = json.loads(raw)
    if canonical(snapshot) != raw or snapshot["schema"] != "qixu-allocation-input/2" or snapshot["batch"] != packet["batch"] or snapshot["algorithm"] != VERSION:
        raise ValueError("Unsupported or noncanonical frozen input")
    formal = packet["result"]
    if formal["input_hash"] != packet["input_hash"]:
        raise ValueError("Formal result belongs to another input")
    proof = formal["proof"]
    expected = snapshot["source"]["round"]
    if type(expected) is not int or not 1 <= expected <= 9_007_199_254_740_991 or snapshot["source"]["chain"] != CHAIN or proof["chain"] != CHAIN or proof["round"] != expected or proof["verifier"] != VERIFIER:
        raise ValueError("Proof coordinates mismatch")
    def instant(value: str) -> datetime:
        result = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if result.tzinfo is None:
            raise ValueError("Missing instant offset")
        return result.astimezone(timezone.utc)
    schedule = snapshot["schedule"]
    random_at = datetime.fromtimestamp(1692803367, timezone.utc) + timedelta(seconds=3 * (expected - 1))
    if not (instant(schedule["close"]) <= instant(packet["frozen_at"]) < instant(schedule["freezeDeadline"]) < random_at == instant(schedule["randomAt"]) <= instant(formal["published_at"]) < instant(schedule["resultDeadline"])):
        raise ValueError("Declared batch timeline violates the fixed future-round contract")
    message = {"chain": CHAIN, "round": expected, "beacon": {"round": expected, "signature": proof["signature"], "randomness": proof["randomness"]}}
    checked = subprocess.run([node, str(ROOT / "randomness/verify.mjs")], input=canonical(message), capture_output=True, timeout=8, check=True)
    checked_value = json.loads(checked.stdout) if len(checked.stdout) <= 4096 else {}
    if checked_value.get("verified") is not True or any(checked_value.get(key) != proof[key] for key in ("chain", "round", "signature", "randomness", "verifier")):
        raise ValueError("Official offline BLS verification did not succeed")
    output = reproduce(snapshot["graph"], packet["input_hash"], proof["randomness"])
    actual = canonical(output)
    if actual != formal["outputBytes"].encode("utf-8") or digest(actual) != formal["output_hash"]:
        raise ValueError("Independent result differs from the entire published result")
    if output["maximum"] != formal["maximum_count"] or len(output["assignments"]) != formal["candidate_count"]:
        raise ValueError("Public counts differ from the reproduction")
    return {"boundary": "ONE_PUBLIC_FROZEN_BATCH_NOT_PRODUCTION_OR_ALL_FAULTS", "batch": packet["batch"], "round": expected, "input_hash": packet["input_hash"], "output_hash": formal["output_hash"], "maximum": output["maximum"], "candidates": len(output["assignments"]), "signature_verified": True, "independent_bytes_equal": True}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("packet", type=Path)
    parser.add_argument("--node", default="node")
    args = parser.parse_args()
    if args.packet.stat().st_size > 5_242_880:
        raise ValueError("Packet exceeds size budget")
    print(json.dumps(verify(json.loads(args.packet.read_text(encoding="utf-8")), args.node), ensure_ascii=False))


if __name__ == "__main__":
    main()
