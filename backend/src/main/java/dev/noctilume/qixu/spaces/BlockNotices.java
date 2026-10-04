package dev.noctilume.qixu.spaces;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.preparation.FrozenJson;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Immutable original recipients plus current applicants; delivery projections grant no authority. */
@Component
public class BlockNotices {
  private final Business b;
  private final FrozenJson canonical;

  public BlockNotices(Business b, JsonMapper json) {
    this.b = b;
    this.canonical = new FrozenJson(json);
  }

  private Set<Long> ids(String sql, String column, Object... args) {
    var rows = b.jdbc.queryForList(sql, args);
    if (rows.size() > 2000)
      throw Business.conflict("IMPACT_RECIPIENT_LIMIT", "关联集合超过2000项，未截断通知或协调对象。");
    var result = new TreeSet<Long>();
    for (var r : rows) result.add(Business.number(r, column));
    return result;
  }

  private void limit(Collection<Long> ids) {
    if (ids.size() > 2000) throw Business.conflict("IMPACT_RECIPIENT_LIMIT", "相关用户超过通知协调上限，未截断。");
  }

  public Set<Long> batches(long block) {
    return ids(
        "SELECT batch_id FROM block_batch WHERE block_id=? ORDER BY batch_id LIMIT 2001",
        "batch_id",
        block);
  }

  public Set<Long> venueBatches(long venue) {
    return ids(
        "SELECT DISTINCT t.batch_id FROM block_batch t JOIN space_block k ON t.block_id=k.id WHERE k.venue_request_id=? AND k.status='ACTIVE' ORDER BY t.batch_id LIMIT 2001",
        "batch_id",
        venue);
  }

  public void lockBatches(Collection<Long> batches) {
    limit(batches);
    for (long batch : new TreeSet<>(batches))
      if (b.jdbc
          .queryForList("SELECT id FROM preparation_batch WHERE id=? FOR UPDATE", batch)
          .isEmpty()) throw DomainException.missing();
  }

  public void checkVenueBatches(long venue, Collection<Long> locked) {
    if (!locked.containsAll(venueBatches(venue)))
      throw Business.conflict("IMPACT_COORDINATES_CHANGED", "场地关联批次已变化，请重试，未逆序追加锁。");
  }

  public Set<Long> users(long block) {
    return ids(
        "SELECT user_id FROM block_recipient WHERE block_id=? UNION SELECT a.user_id FROM preparation_application a JOIN block_batch t ON a.batch_id=t.batch_id WHERE t.block_id=? AND a.status='SUBMITTED' ORDER BY user_id LIMIT 2001",
        "user_id",
        block,
        block);
  }

  public Set<Long> venueUsers(long venue) {
    var result = new TreeSet<Long>();
    for (var row :
        b.jdbc.queryForList(
            "SELECT id FROM space_block WHERE venue_request_id=? AND status='ACTIVE'", venue)) {
      result.addAll(users(Business.number(row, "id")));
      limit(result);
    }
    return result;
  }

  public void retain(long block, Collection<Long> batches, Collection<Long> users) {
    limit(batches);
    limit(users);
    for (long batch : new TreeSet<>(batches))
      b.jdbc.update(
          "INSERT INTO block_batch(block_id,batch_id,created_at) VALUES(?,?,?)",
          block,
          batch,
          b.now());
    for (long user : new TreeSet<>(users))
      b.jdbc.update(
          "INSERT INTO block_recipient(block_id,user_id,created_at) VALUES(?,?,?)",
          block,
          user,
          b.now());
  }

  public String currentHash(long block) {
    var rows = b.jdbc.queryForList("SELECT * FROM space_block WHERE id=?", block);
    if (rows.isEmpty()) throw DomainException.missing();
    var evidence = new TreeMap<String, Object>();
    evidence.put("block", b.view(rows.get(0)));
    evidence.put("users", new ArrayList<>(users(block)));
    evidence.put("batches", new ArrayList<>(batches(block)));
    evidence.put(
        "impacts",
        b
            .jdbc
            .queryForList("SELECT * FROM block_impact WHERE block_id=? ORDER BY id", block)
            .stream()
            .map(b::view)
            .toList());
    evidence.put(
        "arrangements",
        b
            .jdbc
            .queryForList(
                "SELECT * FROM long_temporary_arrangement WHERE block_id=? ORDER BY id", block)
            .stream()
            .map(b::view)
            .toList());
    evidence.put(
        "applicants",
        b
            .jdbc
            .queryForList(
                "SELECT a.id,a.batch_id,a.user_id,a.status,a.current_version FROM preparation_application a JOIN block_batch t ON a.batch_id=t.batch_id WHERE t.block_id=? ORDER BY a.id",
                block)
            .stream()
            .map(b::view)
            .toList());
    return Digests.sha256(canonical.encode(evidence));
  }
}
