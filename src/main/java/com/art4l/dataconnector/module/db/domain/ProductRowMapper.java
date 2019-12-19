package com.art4l.dataconnector.module.db.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProductRowMapper implements RowMapper<Product> {
    private static int NUMBER_OF_COMPONENTS = 10;

    @Override
    public Product mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final Product product = new Product();


        product.setId(rs.getInt("ID"));
        product.setProductName(rs.getString("PRODUCTNAME"));
        List<Component> components = new ArrayList<>();
        Component component = new Component();

        for (int i=1; i<= NUMBER_OF_COMPONENTS;i++) {
            component.setId(rs.getInt("COMPONENT_"+i));
            component.setAmount(rs.getInt("AMOUNT_"+i));
            components.add(component);
            component = new Component();
        }

        product.setComponents(components);

        return product;
    }
}
