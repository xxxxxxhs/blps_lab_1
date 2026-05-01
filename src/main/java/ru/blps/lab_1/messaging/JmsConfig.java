package ru.blps.lab_1.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
@EnableJms
public class JmsConfig {

    @Bean
    public ActiveMQConnectionFactory jmsConnectionFactory(
        @Value("${activemq.broker-url:tcp://activemq:61616}") String brokerUrl
    ) {
        return new ActiveMQConnectionFactory(brokerUrl);
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
        ActiveMQConnectionFactory jmsConnectionFactory
    ) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(jmsConnectionFactory);
        factory.setSessionTransacted(true);
        factory.setConcurrency("1-5");
        factory.setRecoveryInterval(5_000L);
        return factory;
    }
}
