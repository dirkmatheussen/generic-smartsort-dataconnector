package com.art4l.dataconnector.module.dataconnector.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class GenericCommand {

    private String processInstanceId;
    private String replyTo;
    private String spanId;
    private String type;
    private String locationName;

    private Map<String, Object> processVariables;
}
