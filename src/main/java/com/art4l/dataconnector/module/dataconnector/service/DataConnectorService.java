package com.art4l.dataconnector.module.dataconnector.service;


import com.art4l.dataconnector.module.dataconnector.domain.dto.CommandType;
import com.art4l.dataconnector.module.dataconnector.domain.dto.GenericCommand;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEvent;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEventList;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEventType;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandVariable;
import com.art4l.dataconnector.module.dataconnector.service.event.EventEmitterService;
import com.art4l.martur.db.MarturDbConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class DataConnectorService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final MarturDbConnector marturDbConnector;

    private final EventEmitterService eventEmitterService;

    @Autowired
    public DataConnectorService(EventEmitterService eventEmitterService, MarturDbConnector marturDbConnector) {
        this.eventEmitterService = eventEmitterService;
        this.marturDbConnector = marturDbConnector;
    }

    // Subscribe to command topics
    @JmsListener(destination = "*.dataconnectors.*", containerFactory = "topicListenerFactory")
    public void handleCommand(String message, @Header(JmsHeaders.DESTINATION) String topic) {
        try {
            log.info("Received message on topic " + topic);
            Map<String, String> params = parseTopic(topic);

            // Check command type
            final String commandTypeName = params.get("type");
            checkNotNull(commandTypeName, "Command type should not be null");

            log.info("Received command with command type {}", commandTypeName);

            final CommandType commandType = CommandType.findByType(commandTypeName);
            checkNotNull(commandType, "Command should not be null");

            // Parse message
            GenericCommand command = new ObjectMapper().readValue(message, GenericCommand.class);


            // Get the command info and emit the events
            try {
                final String processInstanceId = command.getProcessInstanceId();
                final String spanId = command.getSpanId();
                final String replyTo = command.getReplyTo();
                final Map<String, Object> processVariables = command.getProcessVariables();
                final List<CommandEventType> events = CommandEventList.findByType(commandTypeName).getEvents();

                if (events.isEmpty()) {
                    log.warn("emitting events for an empty events list! commandType {}", commandType);
                }


                emitEvents(
                        processInstanceId,
                        spanId,
                        replyTo,
                        processVariables,
                        events
                );
            } catch (Exception e) {
                log.error("failed to emit events for commandType {} {}", commandType, e);
            }

        } catch (Exception e) {
            log.error("Failed to handle message: " + message + " Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @JmsListener(destination = "destroy", containerFactory = "topicListenerFactory")
    public void destroy(String message, @Header(JmsHeaders.DESTINATION) String topic) {
        try {
            log.info("Destroy message received, cleaning all sessions");
            marturDbConnector.cleanup();
        } catch (Exception e) {
            log.error("Failed to handle message: " + message + " Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @JmsListener(destination = "custom", containerFactory = "topicListenerFactory")
    public void custom(String message, @Header(JmsHeaders.DESTINATION) String topic) {
        try {
            log.info("Custom message received, executing on all sessions");
//            denHaagVt100Connector.custom(message);
        } catch (Exception e) {
            log.error("Failed to handle message: " + message + " Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private Map<String, String> parseTopic(String topic) {
        Map<String, String> resp = new HashMap<>();
        String[] parts = topic.replace("topic://", "").split("\\.");

        resp.put("locationName", parts[0]);
        resp.put("type", parts[2]);
        return resp;
    }

    private void emitEvents(
            String processInstanceId,
            String spanId,
            String replyTo,
            Map<String, Object> processVariables,
            List<CommandEventType> events
    ) throws Exception {
        Map<CommandVariable, Object> commandVariables = new HashMap<>();
        for (CommandEventType event : events) {
            emitEvent(event, processInstanceId, spanId, replyTo, processVariables, commandVariables);
        }
    }

    private void emitEvent(
            CommandEventType event,
            String processInstanceId,
            String spanId,
            String replyTo,
            Map<String, Object> processVariables,
            Map<CommandVariable, Object> commandVariables
    ) throws Exception {
        eventEmitterService.getEventEmitter().emitEvent(new CommandEvent(processInstanceId, spanId, replyTo, event, processVariables, commandVariables));
    }
}