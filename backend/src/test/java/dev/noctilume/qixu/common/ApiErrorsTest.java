package dev.noctilume.qixu.common;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;

class ApiErrorsTest {
    private final ApiErrors advice=new ApiErrors();
    private MockHttpServletRequest request() {
        var r=new MockHttpServletRequest();r.setAttribute("requestId","test-request");return r;
    }
    private void assertUnknown(org.springframework.http.ResponseEntity<Object> response) {
        assertEquals(503,response.getStatusCode().value());
        var body=(Map<?,?>)response.getBody();assertEquals("test-request",body.get("requestId"));
        var error=(Map<?,?>)body.get("error");assertEquals("DATABASE_UNAVAILABLE",error.get("code"));
        assertTrue(error.get("message").toString().contains("尚未确认"));
        assertFalse(body.toString().contains("private-db-detail"));
    }
    @Test void databaseRollbackWrapperKeepsUnknownReceiptSemanticsWithoutCauseLeak() {
        var wrapped=new TransactionSystemException("JDBC rollback failed",new SQLException("private-db-detail","08S01"));
        assertUnknown(advice.transaction(wrapped,request()));
    }
    @Test void retainedOriginalDatabaseExceptionIsNotLostToTransactionWrapper() {
        var wrapped=new TransactionSystemException("framework failure");
        wrapped.initApplicationException(new DataAccessResourceFailureException("private-db-detail"));
        assertUnknown(advice.transaction(wrapped,request()));
        assertUnknown(advice.database(new DataAccessResourceFailureException("private-db-detail"),request()));
    }
    @Test void nonDatabaseTransactionProgrammingFailureRemainsInternalError() {
        var response=advice.transaction(new TransactionSystemException("private-db-detail",new IllegalStateException("bug")),request());
        assertEquals(500,response.getStatusCode().value());
        var body=(Map<?,?>)response.getBody();assertEquals("test-request",body.get("requestId"));
        assertEquals("INTERNAL_ERROR",((Map<?,?>)body.get("error")).get("code"));
        assertFalse(body.toString().contains("private-db-detail"));
    }
}
