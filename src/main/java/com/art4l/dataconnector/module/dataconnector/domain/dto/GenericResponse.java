package com.art4l.dataconnector.module.dataconnector.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class GenericResponse {

    private String processInstanceId;
    private String spanId;
    private String status;

    private Map<String, Object> processVariables = new HashMap<>();
}
