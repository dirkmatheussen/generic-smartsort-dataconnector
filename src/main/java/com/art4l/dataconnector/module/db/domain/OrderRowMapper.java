package com.art4l.dataconnector.module.db.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderRowMapper implements RowMapper<Order> {

    @Override
    public Order mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final Order order = new Order();

        order.setId(rs.getInt("ID"));
        order.setProductId(rs.getInt("PRODUCTID"));
        order.setOrderLabel(rs.getString("ORDERLABEL"));
        order.setSeqNumber(rs.getInt("SEQNUMBER"));
        return order;
    }
}
