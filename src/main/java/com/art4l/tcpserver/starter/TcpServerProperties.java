package com.art4l.tcpserver.starter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "art4l.tcp-server")
public class TcpServerProperties {

    private int port;
    private boolean autoStart;

}
