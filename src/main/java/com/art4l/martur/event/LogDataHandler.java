package com.art4l.martur.event;

import com.art4l.dataconnector.container.configuration.BackendConfig;
import com.art4l.dataconnector.module.dataconnector.domain.dto.CommandType;
import com.art4l.dataconnector.module.dataconnector.domain.dto.GenericResponse;
import com.art4l.dataconnector.module.dataconnector.domain.dto.ResponseStatus;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEvent;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEventType;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandVariable;
import com.art4l.dataconnector.module.dataconnector.domain.event.Subscriber;
import com.art4l.dataconnector.module.db.domain.Component;
import com.art4l.dataconnector.module.db.domain.LogData;
import com.art4l.dataconnector.module.db.domain.Order;
import com.art4l.dataconnector.module.db.domain.Product;
import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class LogDataHandler extends AbstractKittingHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final CommandType COMMAND_TYPE = CommandType.LOG_DATA;

    private static final CommandEventType ON_RECEIVE_EVENT = CommandEventType.ON_RECEIVE_LOG_DATA_COMMAND;
    private static final CommandEventType BEFORE_REPLY_EVENT = CommandEventType.BEFORE_REPLY_LOG_DATA_COMMAND;

    private final BackendConfig mBackendConfig;

    protected List<String> topics;

 	@Autowired
 	public LogDataHandler(BackendConfig backendConfig) {
 		super();
 		mBackendConfig = backendConfig;

 	}

    @PostConstruct
    public void init() {

    	this.topics = Arrays.asList("kitting");

        onReceiveSubscriber();
        beforeReplySubscriber();
    }

    private void onReceiveSubscriber() {
        eventEmitterService.getEventEmitter()
                .filter(commandEvent -> commandEvent.getEventType().equals(ON_RECEIVE_EVENT))
                .filter(commandEvent -> topics.contains((String) commandEvent.getProcessVariables().get("flow")))
                .subscribe(new Subscriber() {
                    @Override
                    public void handleEvent(CommandEvent commandEvent) throws Exception {

                        log.info("preparing command " + COMMAND_TYPE + " for flow: "+ commandEvent.getProcessVariables().get("flow") + " device: " + commandEvent.getProcessVariables().get("deviceId"));

						// Get default variables
						final String processInstanceId = commandEvent.getProcessInstanceId();
						final String spanId = commandEvent.getSpanId();

						// Prepare response
						final GenericResponse response = new GenericResponse();
						response.setProcessInstanceId(processInstanceId);
						response.setSpanId(spanId);

                        final List<LinkedHashMap<String, String>> logDataMap = (List<LinkedHashMap<String, String>>) commandEvent.getProcessVariables().get("logdata");


                        new Thread(() -> {
                            try {
                                ArrayList<LogData> logDataArrayList = convertIntoLogData(logDataMap);
                                dbConnector.writeLogs(processInstanceId, logDataArrayList);
                            } catch (Exception e){
                                //swallow the exception
                            }
                        }).start();

                        commandEvent.getCommandVariables().put(CommandVariable.DATACONNECTOR_RESPONSE, response);
                    }
                });
    }

    private void beforeReplySubscriber() {
        eventEmitterService.getEventEmitter()
                .filter(commandEvent -> commandEvent.getEventType().equals(BEFORE_REPLY_EVENT))
                .filter(commandEvent -> topics.contains((String) commandEvent.getProcessVariables().get("flow")))
                .subscribe(new Subscriber() {
                    @Override
                    public void handleEvent(CommandEvent commandEvent) throws Exception {
                        log.info("sending command " + COMMAND_TYPE);

                        // Get prepared response
                        final GenericResponse response = (GenericResponse) commandEvent.getCommandVariables().get(CommandVariable.DATACONNECTOR_RESPONSE);

                        // Send reply including authenticationToken
                        amqTopicSender.send(commandEvent.getReplyTo(), response);
                    }
                });
    }

    /**
     * Convert into LogData Array
     *
     * @param logDataMap
     * @return
     */

    private ArrayList<LogData> convertIntoLogData(List<LinkedHashMap<String, String>> logDataMap){
 	    ArrayList<LogData> logDataArrayList = new ArrayList<>();
        String json= new Gson().toJson(logDataArrayList);
        logDataArrayList = new Gson().fromJson(json,new TypeToken<List<LogData>>(){}.getType());
 	    return logDataArrayList;
    }

		 
}


