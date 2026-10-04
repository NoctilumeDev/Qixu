package dev.noctilume.qixu.config;

import com.mysql.cj.conf.ConnectionUrl;
import com.mysql.cj.conf.DefaultPropertySet;
import com.mysql.cj.conf.PropertyKey;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Validate the driver's effective single-host properties before pool initialization. */
@Configuration(proxyBeanMethods = false)
public class DatabaseBudgets {
  @Bean
  static BeanPostProcessor boundedDatabaseConnections() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String name) {
        if (bean instanceof HikariDataSource pool) validate(pool);
        return bean;
      }
    };
  }

  static void validate(HikariDataSource pool) {
    boolean valid = false;
    try {
      var url =
          ConnectionUrl.getConnectionUrlInstance(pool.getJdbcUrl(), pool.getDataSourceProperties());
      if (url.getType() != ConnectionUrl.Type.SINGLE_CONNECTION)
        throw new IllegalArgumentException();
      var properties = new DefaultPropertySet();
      properties.initializeProperties(url.getMainHost().exposeAsProperties());
      valid =
          properties.getIntegerProperty(PropertyKey.connectTimeout).getValue() == 3000
              && properties.getIntegerProperty(PropertyKey.socketTimeout).getValue() == 30000
              && pool.getConnectionTimeout() == 3000
              && pool.getValidationTimeout() == 2000
              && "SET SESSION innodb_lock_wait_timeout=10"
                  .equalsIgnoreCase(pool.getConnectionInitSql());
    } catch (RuntimeException ignored) {
      // JDBC URLs may contain credentials. Do not propagate parser diagnostics.
    }
    if (!valid)
      throw new IllegalStateException(
          "Database budgets require single-host MySQL, connect/acquisition 3000ms, validation 2000ms, socket 30000ms and session lock wait10s.");
  }
}
