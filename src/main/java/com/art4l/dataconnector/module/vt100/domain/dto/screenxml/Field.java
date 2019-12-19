package com.art4l.dataconnector.module.vt100.domain.dto.screenxml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Field {
    private String name;
    private Label label;
    private Input input;
    @JacksonXmlElementWrapper(localName = "navigationkeys")
    private Map<String, String> navigationKeys;
}
