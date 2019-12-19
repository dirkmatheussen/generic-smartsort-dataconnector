package com.art4l.ftpconnector;

import com.art4l.dataconnector.module.dataconnector.service.AmqTopicSender;
import com.art4l.dataconnector.module.dataconnector.service.event.EventEmitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public abstract class AbstractFTPHandler {

    @Autowired
    protected EventEmitterService eventEmitterService;

    @Autowired
    protected AmqTopicSender amqTopicSender;

    public EventEmitterService getEventEmitterService() {
        return eventEmitterService;
    }

    public void setEventEmitterService(EventEmitterService eventEmitterService) {
        this.eventEmitterService = eventEmitterService;
    }

    public AmqTopicSender getAmqTopicSender() {
        return amqTopicSender;
    }

    public void setAmqTopicSender(AmqTopicSender amqTopicSender) {
        this.amqTopicSender = amqTopicSender;
    }

}
