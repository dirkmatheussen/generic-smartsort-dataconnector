package com.art4l.dataconnector.module.dataconnector.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class CommandEvent {

    private String processInstanceId;
    private String spanId;
    private String replyTo;

    private CommandEventType eventType;

    private Map<String, Object> processVariables;
    private Map<CommandVariable, Object> commandVariables;

}
