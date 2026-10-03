package dev.noctilume.qixu.config;

import com.mysql.cj.conf.ConnectionUrl;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseBudgetsTest {
    private HikariDataSource pool(String url) {
        var pool=new HikariDataSource();pool.setJdbcUrl(url);
        var properties=new Properties();properties.setProperty("connectTimeout","3000");properties.setProperty("socketTimeout","30000");
        pool.setDataSourceProperties(properties);pool.setConnectionTimeout(3000);pool.setValidationTimeout(2000);
        pool.setConnectionInitSql("SET SESSION innodb_lock_wait_timeout=10");return pool;
    }
    @Test void genericUrlCannotDisableConfiguredDriverBudgets() {
        try(var pool=pool("jdbc:mysql://127.0.0.1:3306/qixu?socket%54imeout=0&connectTimeout=0")) {
            var effective=ConnectionUrl.getConnectionUrlInstance(pool.getJdbcUrl(),pool.getDataSourceProperties()).getMainHost().exposeAsProperties();
            assertEquals("30000",effective.getProperty("socketTimeout"));assertEquals("3000",effective.getProperty("connectTimeout"));
            assertDoesNotThrow(()->DatabaseBudgets.validate(pool));assertFalse(pool.isRunning());
        }
    }
    @Test void hostSpecificOverrideCannotBypassGlobalSocketBudget() {
        try(var pool=pool("jdbc:mysql://address=(host=127.0.0.1)(port=3306)(socketTimeout=0)/qixu")) {
            var effective=ConnectionUrl.getConnectionUrlInstance(pool.getJdbcUrl(),pool.getDataSourceProperties()).getMainHost().exposeAsProperties();
            assertEquals("0",effective.getProperty("socketTimeout"));assertThrows(IllegalStateException.class,()->DatabaseBudgets.validate(pool));assertFalse(pool.isRunning());
        }
    }
    @Test void zeroNegativeOrExtendedDriverBudgetsFailBeforeConnections() {
        for(var value:new String[]{"0","-1","60000"})try(var pool=pool("jdbc:mysql://127.0.0.1:3306/qixu")) {
            pool.addDataSourceProperty("socketTimeout",value);assertThrows(IllegalStateException.class,()->DatabaseBudgets.validate(pool));assertFalse(pool.isRunning());
        }
    }
    @Test void multipleHostsOrMissingLockInitializationCannotInventFiniteBudget() {
        try(var pool=pool("jdbc:mysql://127.0.0.1:3306,127.0.0.2:3306/qixu")) {assertThrows(IllegalStateException.class,()->DatabaseBudgets.validate(pool));}
        try(var pool=pool("jdbc:mysql://127.0.0.1:3306/qixu")) {pool.setConnectionInitSql(null);assertThrows(IllegalStateException.class,()->DatabaseBudgets.validate(pool));}
        try(var pool=pool("jdbc:mysql://127.0.0.1:3306/qixu")) {pool.setValidationTimeout(5000);assertThrows(IllegalStateException.class,()->DatabaseBudgets.validate(pool));}
    }
}
