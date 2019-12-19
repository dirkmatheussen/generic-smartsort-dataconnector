package com.art4l.dataconnector.container.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by jens on 30.04.18.
 */

@Getter
@Setter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix="jwt")
public class JWTConfig {
    private String secret;
}
