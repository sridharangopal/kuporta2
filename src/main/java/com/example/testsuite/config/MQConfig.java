package com.example.testsuite.config;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;

@Configuration
public class MQConfig {

    @Value("${ibm.mq.host}")
    private String host;

    @Value("${ibm.mq.port}")
    private Integer port;

    @Value("${ibm.mq.queue-manager}")
    private String queueManager;

    @Value("${ibm.mq.channel}")
    private String channel;

    @Value("${ibm.mq.username}")
    private String username;

    @Value("${ibm.mq.password}")
    private String password;

    @Bean
    public MQConnectionFactory mqConnectionFactory() throws JMSException {
        MQConnectionFactory factory;
        try {
            factory = new MQConnectionFactory();
            factory.setHostName(host);
            factory.setPort(port);
            factory.setQueueManager(queueManager);
            factory.setChannel(channel);
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setStringProperty(WMQConstants.USERID, username);
            factory.setStringProperty(WMQConstants.PASSWORD, password);
        } catch (Exception e) {
            JMSException jmsException = new JMSException("Failed to configure MQ connection factory");
            jmsException.initCause(e);
            throw jmsException;
        }
        return factory;
    }

    @Bean
    public ConnectionFactory cachingConnectionFactory(MQConnectionFactory mqConnectionFactory) throws JMSException {
        return new CachingConnectionFactory((ConnectionFactory) mqConnectionFactory);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory cachingConnectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(cachingConnectionFactory);
        jmsTemplate.setSessionTransacted(false);
        return jmsTemplate;
    }
}