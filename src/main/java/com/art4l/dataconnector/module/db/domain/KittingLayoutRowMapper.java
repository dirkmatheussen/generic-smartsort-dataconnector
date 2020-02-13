package com.art4l.dataconnector.module.db.domain;

import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KittingLayoutRowMapper implements RowMapper<KittingLayout> {

    @Override
    public KittingLayout mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final KittingLayout kittingLayout = new KittingLayout();

        kittingLayout.setId(rs.getInt("ID"));
        kittingLayout.setLayoutName(rs.getString("LAYOUTNAME"));
        kittingLayout.setCols(rs.getInt("COLS"));
        kittingLayout.setRows(rs.getInt("ROWS"));
        kittingLayout.setStartArPosition(rs.getInt("STARTARPOSITION"));
        kittingLayout.setLeft2right(rs.getBoolean("LEFT2RIGHT"));

        return kittingLayout;
    }
}
