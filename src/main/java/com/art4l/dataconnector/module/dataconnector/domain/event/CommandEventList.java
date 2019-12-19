package com.art4l.dataconnector.module.dataconnector.domain.event;

import com.art4l.dataconnector.module.dataconnector.domain.dto.CommandType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.art4l.dataconnector.module.dataconnector.domain.event.CommandEventType.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CommandEventList {

    // Event List Name
    GET_DATA_FILE(
            // CommandType the Event List is for
            CommandType.GET_DATAFILE.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(
                    
                    BEFORE_RECEIVE_GET_DATAFILE_COMMAND,
                    ON_RECEIVE_GET_DATAFILE_COMMAND,
                    AFTER_RECEIVE_GET_DATAFILE_COMMAND,
                    BEFORE_REPLY_GET_DATAFILE_COMMAND,
                    ON_REPLY_GET_DATAFILE_COMMAND,
                    AFTER_REPLY_GET_DATAFILE_COMMAND
            )
    ),
    PUT_DATA_FILE(
            // CommandType the Event List is for
            CommandType.PUT_DATAFILE.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(
                    
                    BEFORE_RECEIVE_PUT_DATAFILE_COMMAND,
                    ON_RECEIVE_PUT_DATAFILE_COMMAND,
                    AFTER_RECEIVE_PUT_DATAFILE_COMMAND,
                    BEFORE_REPLY_PUT_DATAFILE_COMMAND,
                    ON_REPLY_PUT_DATAFILE_COMMAND,
                    AFTER_REPLY_PUT_DATAFILE_COMMAND
            )
    ),
    LOGIN(
            // CommandType the Event List is for
            CommandType.LOGIN.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(

                    BEFORE_RECEIVE_LOGIN_COMMAND,
                    ON_RECEIVE_LOGIN_COMMAND,
                    AFTER_RECEIVE_LOGIN_COMMAND,
                    BEFORE_REPLY_LOGIN_COMMAND,
                    ON_REPLY_LOGIN_COMMAND,
                    AFTER_REPLY_LOGIN_COMMAND
            )
    ),
    EXIT(
            // CommandType the Event List is for
            CommandType.EXIT.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(

                    BEFORE_RECEIVE_EXIT_COMMAND,
                    ON_RECEIVE_EXIT_COMMAND,
                    AFTER_RECEIVE_EXIT_COMMAND,
                    BEFORE_REPLY_EXIT_COMMAND,
                    ON_REPLY_EXIT_COMMAND,
                    AFTER_REPLY_EXIT_COMMAND
            )
    ),
    KITTING_AREA(
            // CommandType the Event List is for
            CommandType.KITTING_AREA.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(
                    BEFORE_RECEIVE_KITTING_AREA_COMMAND,
                    ON_RECEIVE_KITTING_AREA_COMMAND,
                    AFTER_RECEIVE_KITTING_AREA_COMMAND,
                    BEFORE_REPLY_KITTING_AREA_COMMAND,
                    ON_REPLY_KITTING_AREA_COMMAND,
                    AFTER_REPLY_KITTING_AREA_COMMAND
            )
    ),
    SCAN_ORDER(
            // CommandType the Event List is for
            CommandType.SCAN_ORDER.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(
                    BEFORE_RECEIVE_SCAN_ORDER_COMMAND,
                    ON_RECEIVE_SCAN_ORDER_COMMAND,
                    AFTER_RECEIVE_SCAN_ORDER_COMMAND,
                    BEFORE_REPLY_SCAN_ORDER_COMMAND,
                    ON_REPLY_SCAN_ORDER_COMMAND,
                    AFTER_REPLY_SCAN_ORDER_COMMAND
            )
    ),
    LOG_DATA(
            // CommandType the Event List is for
            CommandType.LOG_DATA.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(

                    BEFORE_RECEIVE_LOG_DATA_COMMAND,
                    ON_RECEIVE_LOG_DATA_COMMAND,
                    AFTER_RECEIVE_LOG_DATA_COMMAND,
                    BEFORE_REPLY_LOG_DATA_COMMAND,
                    ON_REPLY_LOG_DATA_COMMAND,
                    AFTER_REPLY_LOG_DATA_COMMAND
            )


    ),
    CLEANUP(
            // CommandType the Event List is for
            CommandType.CLEANUP.getType(),
            // List of events to be fired for this commandtype, in order of execution
            Arrays.asList(

                    BEFORE_RECEIVE_CLEANUP_COMMAND,
                    ON_RECEIVE_CLEANUP_COMMAND,
                    AFTER_RECEIVE_CLEANUP_COMMAND,
                    BEFORE_REPLY_CLEANUP_COMMAND,
                    ON_REPLY_CLEANUP_COMMAND,
                    AFTER_REPLY_CLEANUP_COMMAND
            )


    );



    private final String type;
    private final List<CommandEventType> events;

    private static final Map<String, CommandEventList> LOOKUP_MAP;

    static {
        LOOKUP_MAP = new HashMap<>();
        for (CommandEventList v : CommandEventList.values()) {
            LOOKUP_MAP.put(v.type, v);
        }
    }

    public static CommandEventList findByType(final String type) {
        return LOOKUP_MAP.get(type);
    }
}
