package com.art4l.martur.event;

import com.art4l.dataconnector.container.configuration.BackendConfig;
import com.art4l.dataconnector.module.dataconnector.domain.dto.CommandType;
import com.art4l.dataconnector.module.dataconnector.domain.dto.GenericResponse;
import com.art4l.dataconnector.module.dataconnector.domain.dto.ResponseStatus;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEvent;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEventType;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandVariable;
import com.art4l.dataconnector.module.dataconnector.domain.event.Subscriber;
import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Service
public class LoginKittingHandler extends AbstractKittingHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final CommandType COMMAND_TYPE = CommandType.LOGIN;

    private static final CommandEventType ON_RECEIVE_EVENT = CommandEventType.ON_RECEIVE_LOGIN_COMMAND;
    private static final CommandEventType BEFORE_REPLY_EVENT = CommandEventType.BEFORE_REPLY_LOGIN_COMMAND;

    private final BackendConfig mBackendConfig;

    protected List<String> topics;

 	@Autowired
 	public LoginKittingHandler(BackendConfig backendConfig) {
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

						try {

						    dbConnector.dbLogin(processInstanceId);


                        } catch (ApplicationException e) {
						    response.setStatus(ResponseStatus.FAILED.getStatus());                             // Add application error information to the response for user feedback
                            response.getProcessVariables().put("applicationError", e.getMessage());

						} catch (UserException e){
                            response.setStatus(ResponseStatus.FAILED.getStatus());                             // Add user error information to the response for user feedback
                            response.getProcessVariables().put("userError", e.getMessage());
                        }
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
    

    

		 
}


