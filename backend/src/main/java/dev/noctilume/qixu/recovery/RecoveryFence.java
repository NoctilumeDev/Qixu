package dev.noctilume.qixu.recovery;

import dev.noctilume.qixu.common.Digests;
import dev.noctilume.qixu.common.DomainException;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;

/** DB restore cannot restore authority without the independent transaction witness. */
@Component
@DependsOnDatabaseInitialization
public class RecoveryFence implements TransactionExecutionListener, AutoCloseable {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(RecoveryFence.class);
  private final JdbcTemplate jdbc;
  private final DataSource source;

  private record Prepared(String id, boolean commitAdmitted) {}

  private final Map<TransactionExecution, Prepared> pending =
      Collections.synchronizedMap(new IdentityHashMap<>());
  private volatile boolean ready;
  private volatile String reason = "INITIALIZING";
  private RecoveryJournal journal;

  public RecoveryFence(
      JdbcTemplate jdbc,
      DataSource source,
      @Value("${qixu.recovery.journal}") String configured,
      @Value("${qixu.recovery.baseline:}") String adoption) {
    this.jdbc = jdbc;
    this.source = source;
    try {
      initialize(Path.of(configured), adoption);
      ready = true;
      reason = "CONSISTENT";
    } catch (Exception error) {
      quarantine("STARTUP_RECONCILIATION_FAILED", error);
    }
  }

  private void initialize(Path path, String adoption) throws Exception {
    var rows =
        jdbc.queryForList("SELECT generation,baseline_hash FROM recovery_generation WHERE id=1");
    boolean existing = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    if (rows.isEmpty()) {
      if (existing || jdbc.queryForObject("SELECT COUNT(*) FROM recovery_marker", Long.class) != 0)
        throw new IOException("Independent history without database generation");
      boolean nonempty = hasExistingFacts();
      if (nonempty && !adoption.equals("ADOPT_PRE_V10_ONCE"))
        throw new IOException("Explicit baseline adoption required");
      String baseline = baselineDigest(), generation = UUID.randomUUID().toString();
      journal = new RecoveryJournal(path, true);
      journal.initialize(generation, baseline);
      // Initialization is an explicit baseline, not a fabricated pre-existing transaction history.
      try (var connection = source.getConnection()) {
        connection.setAutoCommit(false);
        try (var statement =
            connection.prepareStatement(
                "INSERT INTO recovery_generation(id,generation,baseline_hash,adopted_existing) VALUES(1,?,?,?)")) {
          statement.setString(1, generation);
          statement.setString(2, baseline);
          statement.setBoolean(3, nonempty);
          statement.executeUpdate();
          connection.commit();
        } catch (Exception error) {
          try {
            connection.rollback();
          } catch (Exception suppressed) {
            error.addSuppressed(suppressed);
          }
          throw error;
        }
      }
    } else {
      if (!existing) throw new IOException("Independent journal missing");
      journal = new RecoveryJournal(path, false);
      if (!rows.get(0).get("generation").equals(journal.generation())
          || !rows.get(0).get("baseline_hash").equals(journal.baseline()))
        throw new IOException("Recovery generation mismatch");
    }
    var markers =
        jdbc.query(
            "SELECT transaction_id FROM recovery_marker WHERE generation=? ORDER BY transaction_id LIMIT 200001",
            (rs, n) -> rs.getString(1),
            journal.generation());
    if (markers.size() > RecoveryJournal.MAX_EVENTS)
      throw new IOException("Database recovery budget exhausted");
    Set<String> committed = new HashSet<>(markers);
    Map<String, String> states = journal.transactions();
    for (String id : committed)
      if (!Set.of("PREPARE", "COMMIT").contains(states.getOrDefault(id, "ABSENT")))
        throw new IOException("Database marker lacks legal journal history");
    for (var entry : states.entrySet()) {
      if (entry.getValue().equals("ROLLBACK")) {
        if (committed.contains(entry.getKey()))
          throw new IOException("Rollback contradicts database marker");
      } else if (!committed.contains(entry.getKey()))
        throw new IOException("Committed or uncertain transaction missing after restore");
      else if (entry.getValue().equals("PREPARE")) journal.event("RECOVER_COMMIT", entry.getKey());
    }
    journal.assertCurrent();
  }

  private List<String> tables() {
    return jdbc.query(
        "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name NOT IN ('flyway_schema_history','recovery_generation','recovery_marker') ORDER BY table_name",
        (rs, n) -> rs.getString(1));
  }

  private static String identifier(String value) {
    if (!value.matches("[a-zA-Z0-9_]+"))
      throw new IllegalArgumentException("Unexpected database identifier");
    return "`" + value + "`";
  }

  private boolean hasExistingFacts() {
    for (String table : tables())
      if (jdbc.queryForObject(
          "SELECT EXISTS(SELECT 1 FROM " + identifier(table) + " LIMIT 1)", Boolean.class))
        return true;
    return false;
  }

  private String baselineDigest() throws Exception {
    var hash = MessageDigest.getInstance("SHA-256");
    long[] rows = {0};
    for (String table : tables()) {
      hash.update((table + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
      var keys =
          jdbc.query(
              "SELECT column_name FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name=? AND constraint_name='PRIMARY' ORDER BY ordinal_position",
              (rs, n) -> rs.getString(1),
              table);
      if (keys.isEmpty()) throw new IOException("Baseline requires a stable row order");
      jdbc.query(
          "SELECT * FROM "
              + identifier(table)
              + " ORDER BY "
              + String.join(",", keys.stream().map(RecoveryFence::identifier).toList()),
          rs -> {
            if (++rows[0] > 200_000)
              throw new IllegalStateException("Baseline row budget exhausted");
            var metadata = rs.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
              Object v = rs.getObject(i);
              String canonical =
                  v == null
                      ? "NULL"
                      : v instanceof byte[] b
                          ? "BYTES:" + java.util.HexFormat.of().formatHex(b)
                          : v.getClass().getName() + ":" + v;
              byte[] bytes = canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8);
              hash.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
              hash.update(bytes);
            }
          });
    }
    return java.util.HexFormat.of().formatHex(hash.digest());
  }

  public boolean ready() {
    if (ready)
      try {
        journal.assertCurrent();
      } catch (IOException error) {
        quarantine("JOURNAL_COORDINATE_CHANGED", error);
      }
    return ready;
  }

  public String reason() {
    return reason;
  }

  public void requireReady() {
    if (!ready()) throw unavailable();
  }

  public static DomainException unavailable() {
    return new DomainException(503, "NOT_RECONCILED", "恢复记录尚未对账，业务操作已暂停。原回执与请求标识请保留，等待管理人员核实。");
  }

  private void quarantine(String reason, Throwable error) {
    ready = false;
    this.reason = reason;
    LOG.error("Recovery authority quarantined: {} ({})", reason, error.getClass().getSimpleName());
  }

  @Override
  public void beforeBegin(TransactionExecution transaction) {
    requireReady();
  }

  @Override
  public void beforeCommit(TransactionExecution transaction) {
    requireReady();
    if (!transaction.isNewTransaction() || transaction.isReadOnly()) return;
    String id = UUID.randomUUID().toString();
    try {
      journal.event("PREPARE", id);
      pending.put(transaction, new Prepared(id, false));
    } catch (IOException error) {
      quarantine("PREPARE_DURABILITY_FAILED", error);
      throw unavailable();
    }
    try {
      if (jdbc.update(
              "INSERT INTO recovery_marker(transaction_id,generation) VALUES(?,?)",
              id,
              journal.generation())
          != 1) throw new IllegalStateException("Recovery marker was not inserted");
      pending.put(transaction, new Prepared(id, true));
    } catch (RuntimeException error) {
      quarantine("MARKER_WRITE_FAILED", error);
      throw error;
    }
  }

  @Override
  public void afterCommit(TransactionExecution transaction, Throwable failure) {
    finish(transaction, failure == null ? "COMMIT" : null);
  }

  @Override
  public void afterRollback(TransactionExecution transaction, Throwable failure) {
    var prepared = pending.get(transaction);
    finish(
        transaction,
        failure == null && prepared != null && !prepared.commitAdmitted() ? "ROLLBACK" : null);
  }

  private void finish(TransactionExecution transaction, String terminal) {
    var prepared = pending.remove(transaction);
    if (prepared == null) return;
    String id = prepared.id();
    if (terminal == null) {
      quarantine("TRANSACTION_OUTCOME_UNKNOWN", new IOException("Outcome unknown"));
      return;
    }
    try {
      journal.event(terminal, id);
    } catch (IOException error) {
      quarantine("TERMINAL_DURABILITY_FAILED", error);
    }
    // A callback IO failure cannot undo an already committed database fact.
  }

  @jakarta.annotation.PreDestroy
  @Override
  public void close() throws IOException {
    ready = false;
    if (journal != null) journal.close();
  }
}
