package com.art4l.dataconnector.module.dataconnector.domain.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Subscriber {

    private Integer priority = -1;

    public Subscriber() {
    }

    public Subscriber(int priority) {
        this.priority = priority;
    }

    public abstract void handleEvent(CommandEvent commandEvent) throws Exception;
}