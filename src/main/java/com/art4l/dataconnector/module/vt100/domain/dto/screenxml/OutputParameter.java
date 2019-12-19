package com.art4l.dataconnector.module.vt100.domain.dto.screenxml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutputParameter {
    private String key;
    private String regex;
}
