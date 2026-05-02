package ru.blps.lab_1.config;

import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;

@Configuration
public class TransactionConfig {

    @Value("${spring.datasource.host:db}")
    private String dbHost;

    @Value("${spring.datasource.port:5432}")
    private int dbPort;

    @Value("${spring.datasource.dbname:postgres}")
    private String dbName;

    @Value("${spring.datasource.username:postgres}")
    private String dbUser;

    @Value("${spring.datasource.password:postgres}")
    private String dbPassword;

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
    @Primary
    public DataSource dataSource() {
        PGXADataSource xaDataSource = new PGXADataSource();
        xaDataSource.setServerNames(new String[]{dbHost});
        xaDataSource.setPortNumbers(new int[]{dbPort});
        xaDataSource.setDatabaseName(dbName);
        xaDataSource.setUser(dbUser);
        xaDataSource.setPassword(dbPassword);
        return new XaDataSourceWrapper(xaDataSource, narayanaTransactionManager());
    }
}
