package com.art4l.dataconnector.container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author Jens
 */
@SpringBootApplication
@ComponentScan({"com.art4l"})
@EntityScan({"com.art4l"})
@EnableJms
@EnableRetry
public class DataConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataConnectorApplication.class, args);
    }

}
