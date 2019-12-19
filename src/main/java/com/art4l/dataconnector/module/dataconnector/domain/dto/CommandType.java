package com.art4l.dataconnector.module.dataconnector.domain.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CommandType {

	GET_DATAFILE("getDataFile"),
	PUT_DATAFILE("putDataFile"),

    LOGIN("login"),
    EXIT("exit"),
    CLEANUP("cleanup"),

    //Martur events
    KITTING_AREA("kittingArea"),
    SCAN_ORDER("scanOrder"),
    LOG_DATA("logData"),


    GENERIC_SET("setData"),
    GENERIC_GET("getData");

    private final String type;

    private static final Map<String, CommandType> LOOKUP_MAP;

    static {
        LOOKUP_MAP = new HashMap<>();
        for (CommandType v : CommandType.values()) {
            LOOKUP_MAP.put(v.type, v);
        }
    }

    public static CommandType findByType(final String type) {
        return LOOKUP_MAP.get(type);
    }
}
