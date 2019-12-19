package com.art4l.dataconnector.module.db.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Component {
    private String componentName;
    private String barcodeId;
    private int arPosition;
    private int amount;
    private int id;

    public Component() {
    }
}
