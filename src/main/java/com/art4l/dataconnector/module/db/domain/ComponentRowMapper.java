package com.art4l.dataconnector.module.db.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ComponentRowMapper implements RowMapper<Component> {

    @Override
    public Component mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final Component component = new Component();

        component.setId(rs.getInt("ID"));
        component.setComponentName(rs.getString("COMPONENTNAME"));
        component.setBarcodeId(rs.getString("BARCODEID"));

        return component;
    }
}
