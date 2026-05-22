package ru.blps.lab_1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@Configuration
public class TransactionConfig {

    @Bean
    public jakarta.transaction.UserTransaction userTransaction() throws NamingException {
        return InitialContext.doLookup("java:comp/UserTransaction");
    }

    @Bean
    public jakarta.transaction.TransactionManager jtaTransactionManager() throws NamingException {
        return InitialContext.doLookup("java:jboss/TransactionManager");
    }

    @Bean
    public JtaTransactionManager transactionManager(
        jakarta.transaction.UserTransaction userTransaction,
        jakarta.transaction.TransactionManager jtaTransactionManager
    ) {
        return new JtaTransactionManager(userTransaction, jtaTransactionManager);
    }
}
