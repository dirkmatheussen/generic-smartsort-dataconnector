package com.art4l.dataconnector.module.db.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KittingLayout {

    private String layoutName;
    private int id;
    private int cols;
    private int rows;
    private int startArPosition;
    private boolean left2right;

    public KittingLayout() {
    }
}
