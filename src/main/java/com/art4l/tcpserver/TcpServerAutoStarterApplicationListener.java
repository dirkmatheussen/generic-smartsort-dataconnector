package com.art4l.tcpserver;


import com.art4l.tcpserver.starter.TcpServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class TcpServerAutoStarterApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private TcpServerProperties properties;

    @Autowired
    private Server server;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        boolean autoStart = properties.isAutoStart();
        if (autoStart){
            server.setPort(properties.getPort());
            server.start();
        }
    }
}
