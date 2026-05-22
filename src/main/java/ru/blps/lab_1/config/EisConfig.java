package ru.blps.lab_1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.blps.lab_1.eis.OnlyOfficeConnectionFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@Configuration
public class EisConfig {

    @Bean
    public OnlyOfficeConnectionFactory onlyOfficeConnectionFactory() throws NamingException {
        return (OnlyOfficeConnectionFactory) new InitialContext().lookup("java:/eis/OnlyOfficeConnectionFactory");
    }
}