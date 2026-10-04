package dev.noctilume.qixu.recovery;

import dev.noctilume.qixu.common.Digests;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Independent local durability witness. No business bodies or credentials. */
public final class RecoveryJournal implements AutoCloseable {
  static final long MAX_BYTES = 32L * 1024 * 1024;
  static final int MAX_EVENTS = 200_000;
  private final Path path;
  private final FileChannel channel;
  private final FileLock lock;
  private final Object fileKey;
  private final Map<String, String> transactions = new LinkedHashMap<>();
  private String generation, baseline, chain = "0".repeat(64);
  private long size;
  private int sequence;

  public RecoveryJournal(Path configured, boolean create) throws IOException {
    path = configured.toAbsolutePath().normalize();
    for (Path part = path; part != null; part = part.getParent())
      if (Files.isSymbolicLink(part)) throw new IOException("Journal path traverses symbolic link");
    if (create) Files.createDirectories(path.getParent());
    Set<OpenOption> options =
        new HashSet<>(
            Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS));
    if (create) options.add(StandardOpenOption.CREATE_NEW);
    channel = FileChannel.open(path, options);
    FileLock acquired = null;
    try {
      acquired = channel.tryLock();
      if (acquired == null) throw new IOException("Journal already owned");
      fileKey =
          Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
              .fileKey();
      size = channel.size();
      if (size > MAX_BYTES) throw new IOException("Journal budget exhausted");
      ByteBuffer buffer = ByteBuffer.allocate((int) size);
      while (buffer.hasRemaining())
        if (channel.read(buffer) < 0) throw new IOException("Journal truncated during read");
      if (size > 0) parse(new String(buffer.array(), StandardCharsets.US_ASCII));
      lock = acquired;
    } catch (Exception error) {
      if (acquired != null) acquired.close();
      channel.close();
      if (error instanceof IOException io) throw io;
      throw new IOException("Journal invalid or locked", error);
    }
  }

  private void parse(String raw) throws IOException {
    if (!raw.endsWith("\n") || !raw.matches("[A-Za-z0-9_|\\-\\n]+"))
      throw new IOException("Invalid journal bytes");
    String[] lines = raw.split("\n", -1);
    for (int i = 0; i < lines.length - 1; i++) {
      if (i >= MAX_EVENTS) throw new IOException("Journal event budget exhausted");
      String[] fields = lines[i].split("\\|", -1);
      if (fields.length != 6
          || !fields[0].equals(Integer.toString(i))
          || !fields[4].equals(chain)
          || !fields[5].equals(Digests.sha256(String.join("|", Arrays.copyOf(fields, 5)))))
        throw new IOException("Journal chain mismatch");
      if (i == 0) {
        if (!fields[1].equals("QXJ1") || !uuid(fields[2]) || !fields[3].matches("[a-f0-9]{64}"))
          throw new IOException("Invalid journal header");
        generation = fields[2];
        baseline = fields[3];
      } else {
        if (!fields[3].equals(generation) || !uuid(fields[2]))
          throw new IOException("Invalid transaction coordinate");
        transition(fields[1], fields[2]);
      }
      chain = fields[5];
      sequence = i + 1;
    }
  }

  private static boolean uuid(String s) {
    return s.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
  }

  private void transition(String event, String id) throws IOException {
    if (event.equals("PREPARE")) {
      if (transactions.putIfAbsent(id, "PREPARE") != null)
        throw new IOException("Duplicate prepare");
    } else {
      if (!Set.of("COMMIT", "ROLLBACK", "RECOVER_COMMIT").contains(event)
          || !"PREPARE".equals(transactions.get(id)))
        throw new IOException("Invalid transaction terminal");
      transactions.put(id, event.equals("RECOVER_COMMIT") ? "COMMIT" : event);
    }
  }

  public synchronized void initialize(String generation, String baseline) throws IOException {
    if (size != 0
        || this.generation != null
        || !uuid(generation)
        || !baseline.matches("[a-f0-9]{64}"))
      throw new IOException("Journal cannot be initialized");
    append("QXJ1", generation, baseline);
    this.generation = generation;
    this.baseline = baseline;
  }

  public synchronized void event(String event, String id) throws IOException {
    if (generation == null || !uuid(id)) throw new IOException("Journal has no generation");
    // Validate before writing; update memory only after the durable write.
    String previous = transactions.get(id);
    if (event.equals("PREPARE")
        ? previous != null
        : !Set.of("COMMIT", "ROLLBACK", "RECOVER_COMMIT").contains(event)
            || !"PREPARE".equals(previous)) throw new IOException("Invalid event transition");
    append(event, id, generation);
    transition(event, id);
  }

  private void append(String event, String coordinate, String scope) throws IOException {
    assertCurrent();
    if (sequence >= MAX_EVENTS) throw new IOException("Journal event budget exhausted");
    String body = sequence + "|" + event + "|" + coordinate + "|" + scope + "|" + chain;
    String next = Digests.sha256(body);
    byte[] bytes = (body + "|" + next + "\n").getBytes(StandardCharsets.US_ASCII);
    if (size + bytes.length > MAX_BYTES) throw new IOException("Journal byte budget exhausted");
    channel.position(size);
    ByteBuffer b = ByteBuffer.wrap(bytes);
    while (b.hasRemaining()) channel.write(b);
    channel.force(true);
    size += bytes.length;
    sequence++;
    chain = next;
  }

  public synchronized void assertCurrent() throws IOException {
    var current = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!lock.isValid()
        || !current.isRegularFile()
        || Files.isSymbolicLink(path)
        || !Objects.equals(fileKey, current.fileKey())
        || current.size() != size
        || channel.size() != size) throw new IOException("Journal coordinate changed");
  }

  public String generation() {
    return generation;
  }

  public String baseline() {
    return baseline;
  }

  public synchronized Map<String, String> transactions() {
    return Map.copyOf(transactions);
  }

  public synchronized long bytes() {
    return size;
  }

  @Override
  public synchronized void close() throws IOException {
    if (channel.isOpen())
      try {
        lock.close();
      } finally {
        channel.close();
      }
  }
}
