package com.art4l.dataconnector.module.vt100.domain.dto.screenxml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Screen {
    private String name;
    @JacksonXmlProperty(localName = "screenidentifier")
    @JacksonXmlElementWrapper(localName = "screenidentifiers")
    private ScreenIdentifier[] screenIdentifiers;
    @JacksonXmlProperty(localName = "error")
    @JacksonXmlElementWrapper(localName = "errors")
    private Error[] errors;
    @JacksonXmlProperty(localName = "field")
    @JacksonXmlElementWrapper(localName = "fields")
    private Field[] fields;
    @JacksonXmlProperty(localName = "key")
    @JacksonXmlElementWrapper(localName = "navigationkeys")
    private String[] navigationKeys;
    @JacksonXmlProperty(localName = "outputParameter")
    @JacksonXmlElementWrapper(localName = "outputParameters")
    private OutputParameter[] outputParameters;
    private Integer timeout;
}
