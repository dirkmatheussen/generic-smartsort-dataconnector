package com.art4l.dataconnector.module.vt100.domain.dto.screenxml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Input {
    private String value;
    @JsonProperty("valuetype")
    private String valueType;
    private Integer row;
    @JsonProperty("col")
    private Integer column;
    @JsonProperty("optionlist")
    private String optionList;
    private Boolean confirmation;
}
