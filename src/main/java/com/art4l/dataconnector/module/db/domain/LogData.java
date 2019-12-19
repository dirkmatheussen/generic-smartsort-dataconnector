package com.art4l.dataconnector.module.db.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogData {

    private int id;
    private String timestamp;
    private String user;
    private String location;
    private String businessTask;
    private String eventAction;
    private String payload;
    private String value1;
    private String value2;

    public LogData() {
    }

}
