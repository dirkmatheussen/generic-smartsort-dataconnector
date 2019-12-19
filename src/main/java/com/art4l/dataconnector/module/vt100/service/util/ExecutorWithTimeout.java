package com.art4l.dataconnector.module.vt100.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
    This class handles the boilerplate needed to execute a function with a timeout and handle errors
 */
public class ExecutorWithTimeout {
    private static final Logger log = LoggerFactory.getLogger(ExecutorWithTimeout.class);

    public static Object execute(Execution execution, Object onErrorResponse, Object onTimeoutResponse, int timeoutInMilliseconds){
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return execution.execute();
                } catch (Exception e) {
                    log.info("Execution failed: " + e.getMessage());
                    return onErrorResponse;
                }
            }).get(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e ){
            log.info("Execution timed out");
            return onTimeoutResponse;
        }
    }

    public static abstract class Execution {
        public abstract Object execute() throws Exception;
    }
}
