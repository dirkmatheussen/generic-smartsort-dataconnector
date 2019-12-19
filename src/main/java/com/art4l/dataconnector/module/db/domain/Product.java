package com.art4l.dataconnector.module.db.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class Product {
    private int id;
    private String productName;
    private List<Component> components;
    public Product() {
        components = new ArrayList<>();
    }
}
