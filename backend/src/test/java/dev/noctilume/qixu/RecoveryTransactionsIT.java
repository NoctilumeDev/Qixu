package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import dev.noctilume.qixu.recovery.RecoveryFence;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Actual MySQL and configured transaction manager; observer uses a separate connection. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("demo")
class RecoveryTransactionsIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource source;
    @Autowired JdbcTransactionManager manager;
    @Autowired RecoveryFence fence;
    private static final Map<String,Object> observations=new LinkedHashMap<>();
    private final Map<String,Object> measures=new LinkedHashMap<>();
    private String caseName;
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url=System.getenv("QIXU_TEST_DB_URL"),password=System.getenv("QIXU_TEST_DB_PASSWORD");
        if(url==null || !url.matches("jdbc:mysql://[^/]+/(qixu_test|qixu_ci)(\\?.*)?") || password==null || password.isBlank())throw new IllegalStateException("Dedicated real MySQL required");
        r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->System.getenv("QIXU_TEST_DB_USER"));r.add("spring.datasource.password",()->password);r.add("qixu.tasks-enabled",()->"false");
    }
    @BeforeEach void reset(TestInfo info) {
        caseName=info.getTestMethod().orElseThrow().getName();TestData.clearBusiness(jdbc);assertTrue(fence.ready());
    }
    @AfterEach void record() {observations.put(caseName,Map.of("requests",List.of(),"database",new LinkedHashMap<>(measures)));}
    @AfterAll static void retain() throws Exception {
        var path=Path.of("target/failsafe-reports/qixu-m7-transactions-observation.json");Files.createDirectories(path.getParent());Files.writeString(path,JsonMapper.builder().build().writeValueAsString(observations));
    }
    TransactionTemplate tx(int propagation) {
        var template=new TransactionTemplate(manager);template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);template.setPropagationBehavior(propagation);return template;
    }
    void favorite(long seat) {jdbc.update("INSERT INTO favorite_space(user_id,space_id,created_at) VALUES(1,?,UTC_TIMESTAMP(6))",seat);}
    Set<String> markers() {return new HashSet<>(jdbc.queryForList("SELECT transaction_id FROM recovery_marker",String.class));}
    Set<String> outsideMarkers() {
        try(var connection=source.getConnection();var query=connection.createStatement();var rows=query.executeQuery("SELECT transaction_id FROM recovery_marker")) {
            var values=new HashSet<String>();while(rows.next())values.add(rows.getString(1));return values;
        } catch(SQLException error) {throw new IllegalStateException("Independent marker query failed",error);}
    }
    int outsideFavorites(long seat) {
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT COUNT(*) FROM favorite_space WHERE user_id=1 AND space_id=?")) {
            query.setLong(1,seat);try(var rows=query.executeQuery()){if(!rows.next())throw new SQLException("Missing count row");return rows.getInt(1);}
        } catch(SQLException error) {throw new IllegalStateException("Independent business query failed",error);}
    }
    static int difference(Set<String> current,Set<String> before) {var changed=new HashSet<>(current);changed.removeAll(before);return changed.size();}
    static class ControlledRollback extends RuntimeException {}

    @Test void preparedMarkerAndBusinessShareTheRealCommitVisibilityBoundary() {
        var before=outsideMarkers();var listeners=new ArrayList<>(manager.getTransactionExecutionListeners());
        var observer=new TransactionExecutionListener() {
            @Override public void beforeCommit(TransactionExecution transaction) {
                if(!transaction.isNewTransaction() || transaction.isReadOnly())return;
                measures.put("insideMarkerDelta",difference(markers(),before));
                measures.put("insideBusinessRows",jdbc.queryForObject("SELECT COUNT(*) FROM favorite_space WHERE user_id=1 AND space_id=2000",Integer.class));
                measures.put("outsideMarkerDeltaBeforeCommit",difference(outsideMarkers(),before));
                measures.put("outsideBusinessRowsBeforeCommit",outsideFavorites(2000));
            }
        };
        manager.addListener(observer);
        try {tx(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status->favorite(2000));}
        finally {manager.setTransactionExecutionListeners(listeners);}
        measures.put("outsideMarkerDeltaAfterCommit",difference(outsideMarkers(),before));measures.put("outsideBusinessRowsAfterCommit",outsideFavorites(2000));
        assertEquals(1,measures.get("insideMarkerDelta"));assertEquals(1,measures.get("insideBusinessRows"));
        assertEquals(0,measures.get("outsideMarkerDeltaBeforeCommit"));assertEquals(0,measures.get("outsideBusinessRowsBeforeCommit"));
        assertEquals(1,measures.get("outsideMarkerDeltaAfterCommit"));assertEquals(1,measures.get("outsideBusinessRowsAfterCommit"));assertTrue(fence.ready());
    }
    @Test void requiresNewCommitSurvivesOuterRollbackWithoutJoinedPhantomMarkers() {
        var before=outsideMarkers();
        assertThrows(ControlledRollback.class,()->tx(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status->{
            favorite(2000);tx(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(joined->favorite(2001));
            tx(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(inner->favorite(2002));
            measures.put("visibleInnerBeforeOuterRollback",outsideFavorites(2002));measures.put("visibleOuterBeforeRollback",outsideFavorites(2000));
            throw new ControlledRollback();
        }));
        measures.put("outerRows",outsideFavorites(2000));measures.put("joinedRows",outsideFavorites(2001));measures.put("innerRows",outsideFavorites(2002));measures.put("committedMarkerDelta",difference(outsideMarkers(),before));
        assertEquals(1,measures.get("visibleInnerBeforeOuterRollback"));assertEquals(0,measures.get("visibleOuterBeforeRollback"));
        assertEquals(0,measures.get("outerRows"));assertEquals(0,measures.get("joinedRows"));assertEquals(1,measures.get("innerRows"));assertEquals(1,measures.get("committedMarkerDelta"));assertTrue(fence.ready());
    }
    @Test void ordinaryRollbackAndReadOnlyQueriesDoNotManufactureCommitMarkers() {
        var before=outsideMarkers();
        assertThrows(ControlledRollback.class,()->tx(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status->{favorite(2000);throw new ControlledRollback();}));
        var read=tx(TransactionDefinition.PROPAGATION_REQUIRED);read.setReadOnly(true);
        Integer observed=read.execute(status->jdbc.queryForObject("SELECT COUNT(*) FROM favorite_space WHERE user_id=1",Integer.class));
        measures.put("observedRows",observed);measures.put("committedMarkerDelta",difference(outsideMarkers(),before));measures.put("outsideBusinessRows",outsideFavorites(2000));
        assertEquals(0,observed);assertEquals(0,measures.get("committedMarkerDelta"));assertEquals(0,measures.get("outsideBusinessRows"));assertTrue(fence.ready());
    }
    @Test void rejectedSavepointCannotBecomeIndependentAuthorityAndNormalCommitStillWorks() {
        var before=outsideMarkers();
        tx(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status->{
            assertThrows(NestedTransactionNotSupportedException.class,()->tx(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(nested->favorite(2000)));
            favorite(2003);
        });
        measures.put("nestedRows",outsideFavorites(2000));measures.put("normalRows",outsideFavorites(2003));measures.put("committedMarkerDelta",difference(outsideMarkers(),before));
        assertEquals(0,measures.get("nestedRows"));assertEquals(1,measures.get("normalRows"));assertEquals(1,measures.get("committedMarkerDelta"));assertTrue(fence.ready());
    }
}
