package ru.blps.lab_1.config;

import org.postgresql.xa.PGXADataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;

@Configuration
public class TransactionConfig {

    @Bean
    public jakarta.transaction.UserTransaction userTransaction() {
        return com.arjuna.ats.jta.UserTransaction.userTransaction();
    }

    @Bean
    public jakarta.transaction.TransactionManager narayanaTransactionManager() {
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    @Bean
    @Primary
    public JtaTransactionManager transactionManager(
        jakarta.transaction.UserTransaction userTransaction,
        jakarta.transaction.TransactionManager transactionManager
    ) {
        return new JtaTransactionManager(userTransaction, transactionManager);
    }

    @Bean
    public DataSource dataSource() {
        PGXADataSource xaDataSource = new PGXADataSource();
        xaDataSource.setServerNames(new String[]{"localhost"});
        xaDataSource.setPortNumbers(new int[]{5432});
        xaDataSource.setDatabaseName("lab_1");
        xaDataSource.setUser("postgres");
        xaDataSource.setPassword("postgres");
        return new XaDataSourceWrapper(xaDataSource, narayanaTransactionManager());
    }
}
