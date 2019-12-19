package com.art4l.dataconnector.module.db.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ComponentPositionRowMapper implements RowMapper<ComponentPosition> {

    @Override
    public ComponentPosition mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final ComponentPosition componentPosition = new ComponentPosition();

        componentPosition.setId(rs.getInt("ID"));
        componentPosition.setComponentId(rs.getInt("COMPONENTID"));
        componentPosition.setLayoutId(rs.getInt("LAYOUTID"));
        componentPosition.setArPosition(rs.getInt("ARPOSITION"));
        componentPosition.setBarcodeId(rs.getString("BARCODEID"));
        return componentPosition;
    }
}
