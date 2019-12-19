package com.art4l.dataconnector.container.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix="backend")
public class BackendConfig {
    private String folder;
    private String ftpurl;
    private String ftpport;
    private String ftpusername;
    private String ftppassword;
}
