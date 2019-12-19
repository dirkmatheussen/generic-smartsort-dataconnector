package com.art4l.dataconnector.module.dataconnector.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class AmqTopicSender {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final JmsTemplate jmsTemplate;

    @Autowired
    public AmqTopicSender(JmsTemplate jmsTemplate){
        this.jmsTemplate = jmsTemplate;
    }

    public void send(String topic, Object message){
        try {
            jmsTemplate.convertAndSend(new ActiveMQTopic(topic), new ObjectMapper().writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("Failed to send message with AMQ");
        }
    }
}
