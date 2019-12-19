package com.art4l.dataconnector.container.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Component
@ConfigurationProperties(prefix="picklist-info")
public class PicklistInfoConfig {
    private String folder;
}
