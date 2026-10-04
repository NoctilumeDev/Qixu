package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.Digests;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Pure frozen-graph calculation. Produces candidates, never database rights. */
public final class AllocationGraph {
  public static final String VERSION = "qixu-max-cardinality-lottery-v2";

  public record Edge(String seat, int rank) {}

  public record Candidate(String id, List<Edge> acceptable) {}

  public record Input(String algorithm, List<String> seats, List<Candidate> candidates) {}

  public record Assignment(String candidate, String seat, Integer rank, String reason) {}

  public record Result(
      String algorithm,
      String seedDigest,
      List<String> order,
      int maximum,
      List<Assignment> assignments) {}

  public static Result allocate(Input input, String inputHash, String randomness) {
    validate(input);
    if (inputHash == null
        || !inputHash.matches("[0-9a-f]{64}")
        || randomness == null
        || !randomness.matches("[0-9a-f]{64}"))
      throw new IllegalArgumentException("Invalid frozen coordinates");
    String seed = Digests.sha256(VERSION + "\0" + inputHash + "\0" + randomness);
    var order = new ArrayList<>(input.candidates().stream().map(Candidate::id).sorted().toList());
    var stream = new CounterStream(seed);
    for (int i = order.size() - 1; i > 0; i--) Collections.swap(order, i, stream.below(i + 1));
    return match(input, order, seed, System.nanoTime() + 20_000_000_000L);
  }

  static Result match(Input input, List<String> order, String seed, long budgetEnd) {
    validate(input);
    var candidates = new HashMap<String, Candidate>();
    input.candidates().forEach(c -> candidates.put(c.id(), c));
    if (order.size() != candidates.size() || !new HashSet<>(order).equals(candidates.keySet()))
      throw new IllegalArgumentException("Not a candidate permutation");
    var edges = new HashMap<String, List<Edge>>();
    candidates.forEach(
        (id, c) ->
            edges.put(
                id,
                c.acceptable().stream()
                    .sorted(
                        Comparator.comparingInt(Edge::rank)
                            .thenComparing(
                                e ->
                                    Digests.sha256(
                                        "qixu-seat-order-v2\0"
                                            + seed
                                            + "\0"
                                            + id
                                            + "\0"
                                            + e.seat()))
                            .thenComparing(Edge::seat))
                    .toList()));
    var remainingSeats = new TreeSet<>(input.seats());
    int maximum = maximum(order, remainingSeats, edges, budgetEnd), needed = maximum;
    var assignments = new ArrayList<Assignment>();
    for (int i = 0; i < order.size(); i++) {
      checkBudget(budgetEnd);
      String id = order.get(i);
      var later = order.subList(i + 1, order.size());
      Edge chosen = null;
      if (needed > 0)
        for (var edge : edges.get(id)) {
          if (!remainingSeats.remove(edge.seat())) continue;
          if (1 + maximum(later, remainingSeats, edges, budgetEnd) >= needed) {
            chosen = edge;
            break;
          }
          remainingSeats.add(edge.seat());
        }
      if (chosen != null) {
        needed--;
        assignments.add(
            new Assignment(
                id,
                chosen.seat(),
                chosen.rank(),
                chosen.rank() == 1 ? "PREFERRED_OFFER" : "AUTHORIZED_FALLBACK_OFFER"));
      } else {
        if (maximum(later, remainingSeats, edges, budgetEnd) < needed)
          throw new IllegalStateException("Cardinality invariant broken");
        assignments.add(
            new Assignment(
                id, null, null, edges.get(id).isEmpty() ? "NO_ACCEPTABLE_EDGE" : "FIXED_WAITLIST"));
      }
    }
    if (needed != 0) throw new IllegalStateException("Incomplete maximum result");
    return new Result(VERSION, seed, List.copyOf(order), maximum, List.copyOf(assignments));
  }

  private static int maximum(
      List<String> candidates, Set<String> seats, Map<String, List<Edge>> edges, long budgetEnd) {
    var owners = new HashMap<String, String>();
    int count = 0;
    for (String id : candidates)
      if (augment(id, seats, edges, owners, new HashSet<>(), budgetEnd)) count++;
    return count;
  }

  private static boolean augment(
      String id,
      Set<String> seats,
      Map<String, List<Edge>> edges,
      Map<String, String> owners,
      Set<String> visited,
      long budgetEnd) {
    checkBudget(budgetEnd);
    for (var edge : edges.get(id)) {
      if (!seats.contains(edge.seat()) || !visited.add(edge.seat())) continue;
      String owner = owners.get(edge.seat());
      if (owner == null || augment(owner, seats, edges, owners, visited, budgetEnd)) {
        owners.put(edge.seat(), id);
        return true;
      }
    }
    return false;
  }

  private static void checkBudget(long end) {
    if (System.nanoTime() - end >= 0) throw new IllegalStateException("ALLOCATION_COMPUTE_BUDGET");
  }

  private static void identifier(String id) {
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,80}"))
      throw new IllegalArgumentException("Invalid public identifier");
  }

  private static void validate(Input input) {
    if (input == null
        || !VERSION.equals(input.algorithm())
        || input.seats() == null
        || input.candidates() == null
        || input.seats().size() > 400
        || input.candidates().size() > 200) throw new IllegalArgumentException("Unsupported graph");
    var seats = new HashSet<String>();
    for (String id : input.seats()) {
      identifier(id);
      if (!seats.add(id)) throw new IllegalArgumentException("Duplicate seat");
    }
    var people = new HashSet<String>();
    for (var candidate : input.candidates()) {
      if (candidate == null) throw new IllegalArgumentException("Missing candidate");
      identifier(candidate.id());
      if (!people.add(candidate.id()) || candidate.acceptable() == null)
        throw new IllegalArgumentException("Duplicate candidate or absent edges");
      var accepted = new HashSet<String>();
      for (var edge : candidate.acceptable())
        if (edge == null
            || !seats.contains(edge.seat())
            || !accepted.add(edge.seat())
            || edge.rank() < 1
            || edge.rank() > 3) throw new IllegalArgumentException("Invalid acceptable edge");
    }
  }

  private static final class CounterStream {
    private final byte[] seed;
    private long counter;
    private static final BigInteger RANGE = BigInteger.ONE.shiftLeft(256);

    CounterStream(String seed) {
      this.seed = HexFormat.of().parseHex(seed);
    }

    int below(int bound) {
      var divisor = BigInteger.valueOf(bound);
      var limit = RANGE.subtract(RANGE.mod(divisor));
      try {
        for (; ; ) {
          var sha = MessageDigest.getInstance("SHA-256");
          sha.update("qixu-permutation-v2\0".getBytes(StandardCharsets.UTF_8));
          sha.update(seed);
          var value =
              new BigInteger(1, sha.digest(ByteBuffer.allocate(8).putLong(counter++).array()));
          if (value.compareTo(limit) < 0) return value.mod(divisor).intValueExact();
        }
      } catch (java.security.NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private AllocationGraph() {}
}
