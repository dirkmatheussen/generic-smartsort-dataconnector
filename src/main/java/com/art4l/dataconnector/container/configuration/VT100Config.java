package com.art4l.dataconnector.container.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix="vt100")
public class VT100Config {
    private Map<String, VT100Settings> settings;
}
