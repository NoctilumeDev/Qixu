package dev.noctilume.qixu.recovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import dev.noctilume.qixu.common.DomainException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionExecution;

/** Actual file IO; database view is controlled here. Physical MySQL proof is the F12 probe. */
class RecoveryFenceTest {
    @TempDir Path root;
    private final String generation=UUID.randomUUID().toString(),id=UUID.randomUUID().toString();
    private Path path(){return root.resolve("journal");}
    private void history(String terminal) throws Exception {
        try(var j=new RecoveryJournal(path(),true)){j.initialize(generation,"a".repeat(64));j.event("PREPARE",id);if(terminal!=null)j.event(terminal,id);}
    }
    private JdbcTemplate database(List<String> markers) {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForList("SELECT generation,baseline_hash FROM recovery_generation WHERE id=1")).thenReturn(List.of(Map.of("generation",generation,"baseline_hash","a".repeat(64))));
        doReturn(markers).when(jdbc).query(startsWith("SELECT transaction_id FROM recovery_marker"),any(RowMapper.class),eq(generation));
        when(jdbc.update(startsWith("INSERT INTO recovery_marker"),any(Object[].class))).thenReturn(1);return jdbc;
    }
    private RecoveryFence open(JdbcTemplate jdbc){return new RecoveryFence(jdbc,null,path().toString(),"ADOPT_PRE_V10_ONCE");}
    private TransactionExecution transaction(){return new TransactionExecution(){@Override public boolean isNewTransaction(){return true;}};}
    @Test void oldDatabaseMissingKnownCommitMarkerQuarantinesAllAdmission() throws Exception {
        history("COMMIT");try(var f=open(database(List.of()))) {assertFalse(f.ready());var error=assertThrows(DomainException.class,()->f.beforeBegin(transaction()));assertEquals(503,error.status());assertEquals("NOT_RECONCILED",error.code());}
    }
    @Test void incompletePrepareWithRealMarkerRecoversSameCommitWithoutNewGeneration() throws Exception {
        history(null);try(var f=open(database(List.of(id)))){assertTrue(f.ready());}
        try(var j=new RecoveryJournal(path(),false)){assertEquals(generation,j.generation());assertEquals("COMMIT",j.transactions().get(id));}
        assertTrue(Files.readString(path()).contains("RECOVER_COMMIT"));
    }
    @Test void unknownPrepareWithoutMarkerIsNotInventedRollback() throws Exception {
        history(null);byte[] before=Files.readAllBytes(path());try(var f=open(database(List.of()))){assertFalse(f.ready());}assertArrayEquals(before,Files.readAllBytes(path()));
    }
    @Test void knownRollbackWithoutMarkerCanReopenNormally() throws Exception {
        history("ROLLBACK");try(var f=open(database(List.of()))){assertTrue(f.ready());}
    }
    @Test void missingJournalCannotBeBypassedByBaselineAdoption() throws Exception {
        try(var f=open(database(List.of(id)))){assertFalse(f.ready());assertFalse(Files.exists(path()));}
    }
    @Test void databaseMarkerWithoutPrepareCannotObtainAuthority() throws Exception {
        try(var j=new RecoveryJournal(path(),true)){j.initialize(generation,"a".repeat(64));}
        try(var f=open(database(List.of(id)))){assertFalse(f.ready());}
    }
    @Test void markerWriteFailureKnownRollbackRetainsPreparedCoordinateAndCanReconcile() throws Exception {
        try(var j=new RecoveryJournal(path(),true)){j.initialize(generation,"a".repeat(64));}
        var jdbc=database(List.of());when(jdbc.update(startsWith("INSERT INTO recovery_marker"),any(Object[].class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("controlled marker failure"));
        try(var f=open(jdbc)) {
            var tx=transaction();assertThrows(org.springframework.dao.DataAccessException.class,()->f.beforeCommit(tx));assertFalse(f.ready());f.afterRollback(tx,null);
        }
        try(var j=new RecoveryJournal(path(),false)){assertEquals(List.of("ROLLBACK"),new ArrayList<>(j.transactions().values()));}
        try(var f=open(database(List.of()))){assertTrue(f.ready());}
    }
    @Test void unknownCommitOrTerminalIoFailureNeverRewritesCommitAsRollback() throws Exception {
        for(boolean io:new boolean[]{false,true}) {
            Path p=root.resolve(io?"io":"unknown");
            try(var j=new RecoveryJournal(p,true)){j.initialize(generation,"a".repeat(64));}
            var jdbc=database(List.of());
            try(var f=new RecoveryFence(jdbc,null,p.toString(),"")) {
                var tx=transaction();f.beforeCommit(tx);
                var field=RecoveryFence.class.getDeclaredField("journal");field.setAccessible(true);var journal=(RecoveryJournal)field.get(f);
                if(io)RecoveryJournalTest.ownedChannel(journal).close();
                f.afterCommit(tx,io?null:new java.sql.SQLException("controlled unknown commit"));assertFalse(f.ready());
                String raw=new String(io?Files.readAllBytes(p):RecoveryJournalTest.ownedBytes(journal),java.nio.charset.StandardCharsets.US_ASCII);
                assertFalse(raw.contains("|ROLLBACK|"));assertFalse(raw.contains("|COMMIT|"));
                assertThrows(DomainException.class,f::requireReady);
            }
        }
    }
    @Test void commitAttemptFailureFollowedBySuccessfulRollbackRemainsUnknown() throws Exception {
        try(var j=new RecoveryJournal(path(),true)){j.initialize(generation,"a".repeat(64));}
        var jdbc=database(List.of());var dataSource=mock(javax.sql.DataSource.class);var connection=mock(java.sql.Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);when(connection.getAutoCommit()).thenReturn(true);
        try(var fence=open(jdbc)) {
            // Executes pinned Spring processCommit, including rollback-on-runtime-exception callback.
            var manager=new org.springframework.jdbc.support.JdbcTransactionManager(dataSource) {
                @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {
                    throw new org.springframework.dao.DataAccessResourceFailureException("controlled commit acknowledgement loss");
                }
            };
            manager.addListener(fence);
            assertThrows(org.springframework.dao.DataAccessException.class,()->new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(status->{}));
            verify(connection).rollback();assertFalse(fence.ready());
        }
        assertFalse(Files.readString(path()).contains("|ROLLBACK|"));
        try(var j=new RecoveryJournal(path(),false)){assertEquals(List.of("PREPARE"),new ArrayList<>(j.transactions().values()));}
    }
}
