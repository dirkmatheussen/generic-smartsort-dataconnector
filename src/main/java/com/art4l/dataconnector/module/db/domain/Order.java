package com.art4l.dataconnector.module.db.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order {
    private int seqNumber;
    private int productId;
    private int id;
    private String orderLabel;

    public Order() {
    }
}
