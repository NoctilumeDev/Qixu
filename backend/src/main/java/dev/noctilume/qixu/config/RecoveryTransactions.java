package dev.noctilume.qixu.config;

import dev.noctilume.qixu.recovery.RecoveryFence;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration(proxyBeanMethods=false)
public class RecoveryTransactions {
    @Bean JdbcTransactionManager transactionManager(DataSource dataSource,RecoveryFence fence) {
        var manager=new JdbcTransactionManager(dataSource);
        manager.setNestedTransactionAllowed(false);
        manager.addListener(fence);
        return manager;
    }
}
