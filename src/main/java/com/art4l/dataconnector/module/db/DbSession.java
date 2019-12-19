package com.art4l.dataconnector.module.db;

import com.art4l.dataconnector.container.configuration.DbSettings;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

public class DbSession {


    public static DataSource MarturDataSource(DbSettings settings){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        dataSource.setUrl(settings.getUrl());
        dataSource.setUsername(settings.getUsername());
        dataSource.setPassword(settings.getPassword());


        return dataSource;
    }
}
