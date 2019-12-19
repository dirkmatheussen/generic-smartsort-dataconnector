package com.art4l.dataconnector.module.dataconnector.domain.dto;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ResponseStatus {
    SUCCEEDED("succeeded"),
    FAILED_KEEPVAR("failedkeepvar"),
    FAILED("failed");

    private final String status;
}
