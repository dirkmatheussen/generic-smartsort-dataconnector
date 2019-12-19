package com.art4l.dataconnector.container.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class VT100Settings {
    private String host;
    private Map<String, String> properties;
}
