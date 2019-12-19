package com.art4l.dataconnector.module.dataconnector.service.event;

import com.art4l.dataconnector.module.dataconnector.domain.event.EventEmitter;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Getter
public class EventEmitterService {

    private EventEmitter eventEmitter;

    @Autowired
    public EventEmitterService() {
        this.eventEmitter = new EventEmitter();
    }
}
