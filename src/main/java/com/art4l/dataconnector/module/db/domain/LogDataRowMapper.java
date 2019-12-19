package com.art4l.dataconnector.module.db.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class LogDataRowMapper implements RowMapper<LogData> {

    @Override
    public LogData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final LogData logData = new LogData();

        logData.setId(rs.getInt("ID"));
        logData.setBusinessTask(rs.getString("BUSINESSTASK"));
        logData.setEventAction(rs.getString("EVENTACTION"));
        logData.setLocation(rs.getString("LOCATION"));
        logData.setUser(rs.getString("USER"));
        logData.setPayload(rs.getString("PAYLOAD"));
        logData.setValue1(rs.getString("VALUE1"));
        logData.setValue2(rs.getString("VALUE2"));
        Date timeStamp = rs.getTimestamp("TIMESTAMP");
        logData.setTimestamp(timeStamp.toString());
        return logData;
    }
}
