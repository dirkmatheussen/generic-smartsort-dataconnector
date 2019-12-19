package com.art4l.dataconnector.module.db.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComponentPosition {
    private int layoutId;
    private int componentId;
    private int arPosition;
    private String layoutName;
    private String componentName;
    private String barcodeId;
    private int id;

    public ComponentPosition() {
    }
}
