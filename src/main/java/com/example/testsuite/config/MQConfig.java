package com.example.testsuite.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import jakarta.jms.ConnectionFactory;

@Configuration
public class MQConfig {

    @Value("${ibm.mq.queue}")
    private String queueName;

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDefaultDestinationName(queueName);
        template.setSessionTransacted(false);
        // For IBM MQ in development mode, we don't need explicit message conversion
        return template;
    }
}