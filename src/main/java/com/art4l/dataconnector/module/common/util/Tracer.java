package com.art4l.dataconnector.module.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Created by jens on 21.11.17.
 */

public class Tracer {
    public static final String X_CORRELATION_ID = "X-Correlation-Id";

    private Tracer(){}

    public static void setCorrelationId(String correlationId){
        if(correlationId == null || correlationId.isEmpty()){
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(X_CORRELATION_ID, correlationId);
    }

    public static String getCorrelationId(){
        return MDC.get(X_CORRELATION_ID);
    }
}
