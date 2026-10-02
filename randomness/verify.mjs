import { fetchBeacon } from 'drand-client';
import { pathToFileURL } from 'node:url';

export const CHAIN = '52db9ba70e0cc0f6eaf7803dd07447a1f5477735fd3f661792ba94600c84e971';
export const VERIFIER = 'drand-client/1.4.2:qixu-offline/0.1';
const INFO = Object.freeze({
  public_key: '83cf0f2896adee7eb8b5f01fcad3912212c437e0073e911fb90022d3e760183c8c4b450b6a0a6c3ac6a5776a2d1064510d1fec758c921cc22b0e17e63aaf4bcb5ed66304de9cf809bd274ca73bab4af5a6e9c76a4bc09e76eae8991ef5ece45a',
  period: 3, genesis_time: 1692803367, hash: CHAIN,
  schemeID: 'bls-unchained-g1-rfc9380', metadata: Object.freeze({ beaconID: 'quicknet' }),
});

/** Offline only. Neither stdin nor a remote info endpoint may select the trust root. */
export async function verifyProof(input) {
  const round = input?.round, beacon = input?.beacon;
  if (!Number.isSafeInteger(round) || round < 1 || beacon?.round !== round
      || !/^[0-9a-f]{96}$/.test(beacon?.signature ?? '')
      || !/^[0-9a-f]{64}$/.test(beacon?.randomness ?? '')
      || beacon.previous_signature !== undefined || input.chain !== CHAIN)
    throw new Error('INVALID_PROOF');
  const client = {
    options: { disableBeaconVerification: false, noCache: true },
    chain: () => ({ baseUrl: 'offline:quicknet', info: async () => INFO }),
    get: async requested => { if (requested !== round) throw new Error('ROUND_MISMATCH'); return beacon; },
    latest: async () => { throw new Error('LATEST_FORBIDDEN'); },
  };
  const verified = await fetchBeacon(client, round);
  return { verified: true, verifier: VERIFIER, chain: CHAIN, round,
    signature: verified.signature, randomness: verified.randomness };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  // The dependency may log invalid public proofs; keep the process protocol bounded and stable.
  console.error = () => {};
  try {
    let length = 0; const chunks = [];
    for await (const chunk of process.stdin) {
      length += chunk.length;
      if (length > 12000) throw new Error('INPUT_LIMIT');
      chunks.push(chunk);
    }
    const result = await verifyProof(JSON.parse(Buffer.concat(chunks).toString('utf8')));
    process.stdout.write(JSON.stringify(result) + '\n');
  } catch {
    process.stdout.write('{"verified":false,"code":"INVALID_PROOF"}\n');
    process.exitCode = 1;
  }
}
