package com.art4l.ftpconnector;

import com.art4l.dataconnector.container.configuration.BackendConfig;
import com.art4l.dataconnector.module.dataconnector.domain.dto.CommandType;
import com.art4l.dataconnector.module.dataconnector.domain.dto.GenericResponse;
import com.art4l.dataconnector.module.dataconnector.domain.dto.ResponseStatus;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEvent;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEventType;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandVariable;
import com.art4l.dataconnector.module.dataconnector.domain.event.Subscriber;
import com.art4l.dataconnector.module.db.domain.LogData;
import com.art4l.license.LicenseValidator;
import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class WriteLogDataHandler extends AbstractFTPHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final CommandType COMMAND_TYPE = CommandType.LOG_DATA;

	private static final CommandEventType ON_RECEIVE_EVENT = CommandEventType.ON_RECEIVE_LOG_DATA_COMMAND;
	private static final CommandEventType BEFORE_REPLY_EVENT = CommandEventType.BEFORE_REPLY_LOG_DATA_COMMAND;

    private final BackendConfig mBackendConfig;


 	@Autowired
 	public WriteLogDataHandler(BackendConfig backendConfig) {
 		super();
 		mBackendConfig = backendConfig;
 		
 	}

    @PostConstruct
    public void init() {
    	
//    	this.topics = Arrays.asList("smart_sort","distribution","dispersion");
    	
        onReceiveSubscriber();
        beforeReplySubscriber();
    }

    private void onReceiveSubscriber() {
        eventEmitterService.getEventEmitter()
                .filter(commandEvent -> commandEvent.getEventType().equals(ON_RECEIVE_EVENT))
				// no filtering, ON_RECEIVE_EVENTS of all flows are allowed
//                .filter(commandEvent -> topics.contains((String) commandEvent.getProcessVariables().get("flow")))
                .subscribe(new Subscriber() {
                    @Override
                    public void handleEvent(CommandEvent commandEvent) throws Exception {

                        log.info("preparing command " + COMMAND_TYPE + "for flow: "+ commandEvent.getProcessVariables().get("flow") + " device: " + commandEvent.getProcessVariables().get("deviceId"));


						// Get default variables
						final String processInstanceId = commandEvent.getProcessInstanceId();
						final String spanId = commandEvent.getSpanId();

						// Prepare response
						final GenericResponse response = new GenericResponse();
						response.setProcessInstanceId(processInstanceId);
						response.setSpanId(spanId);

						// Get command variables
						final String fileName = (String) commandEvent.getProcessVariables().get("filename");
						final String pathName = (String) commandEvent.getProcessVariables().get("pathname");
						final String ftpUsername = (String) commandEvent.getProcessVariables().get("username");
						final String ftpPassword = (String) commandEvent.getProcessVariables().get("password");
						final List<LinkedHashMap<String, String>> logDataArrayList = (List<LinkedHashMap<String, String>>) commandEvent.getProcessVariables().get("logdata");

						new Thread(()-> {
							// Put the data to the server
							try {
								if (putLogData(pathName, fileName, ftpUsername, ftpPassword, logDataArrayList)) {
									log.info(COMMAND_TYPE + " success, datafile returned: ");
								} else {
									log.info(COMMAND_TYPE + " failed to update ");
								}

							} catch (Exception e) {
								log.info(COMMAND_TYPE + " exception: " + e.getLocalizedMessage());
							}
						}).start();
						response.setStatus(ResponseStatus.SUCCEEDED.getStatus());
						// Store command to execute later
                        commandEvent.getCommandVariables().put(CommandVariable.DATACONNECTOR_RESPONSE, response);
                    }
                });
    }

    private void beforeReplySubscriber() {
        eventEmitterService.getEventEmitter()
                .filter(commandEvent -> commandEvent.getEventType().equals(BEFORE_REPLY_EVENT))
//                .filter(commandEvent -> topics.contains((String) commandEvent.getProcessVariables().get("flow")))
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
    

    
	 private boolean putLogData(String pathName, String fileName, String ftpUsername, String ftpPassword, List<LinkedHashMap<String, String>> csvList){
		 	

 		StringBuilder csvStream = new StringBuilder();

		 Map<String,String> firstLine = csvList.get(0);
		 SortedSet<String> keys = new TreeSet<>(firstLine.keySet());

		 for(Map<String, String> csvLine : csvList){
			 keys.addAll(csvLine.keySet());
		 }
		 csvStream.append(StringUtils.join(keys, ";")).append(System.getProperty("line.separator"));

		 //now get the data rows & append to StringBuilder.
		 for(Map<String, String> csvLine : csvList){
			 String line = getLineFromMap(csvLine, keys);
			 csvStream.append(line).append(System.getProperty("line.separator"));
		 }

		 try {

		 	String ftpUrl = mBackendConfig.getFtpurl();
		 	String ftpPort = mBackendConfig.getFtpport();

		 	return putSFtpFile(ftpUrl, ftpPort, ftpUsername, ftpPassword, pathName, fileName, csvStream.toString());

		 } catch(IOException ex){

		 }

		return false;
	}

	private static String getLineFromMap(Map<String, String> csvList, SortedSet<String> keys) {
		List<String> values = new ArrayList<>();
		for (String key : keys) {
			values.add(csvList.get(key) == null ? " " : csvList.get(key));
		}
		return StringUtils.join(values, ";");
	}


	private boolean putSFtpFile(String url, String port, String username, String password, String pathName, String fileName,String csvStream ) throws IOException{

		JSch jsch = new JSch();
	    Session session = null; 
	    Channel channel = null;

		try {
		 
	        session = jsch.getSession(username, url, Integer.valueOf(port));
	        
	        UserInfo ui = new MyUserInfo();
	        session.setUserInfo(ui);
	        session.setPassword(password);
	        session.setConfig("StrictHostKeyChecking", "no");
	
	        session.connect();
	
	        channel = session.openChannel("sftp");
	        channel.connect();
	        ChannelSftp sftpChannel = (ChannelSftp) channel;
	        //go to the pathName
			sftpChannel.cd(pathName);

	        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvStream.getBytes(StandardCharsets.UTF_8));
	        
//	        fileName = pathName + fileName;
	        sftpChannel.put(inputStream,fileName);

			inputStream.close();
			channel.disconnect();
			session.disconnect();
			
		} catch (JSchException ex) {
			session.disconnect();

			// TODO Auto-generated catch block
			ex.printStackTrace();
			throw new IOException(ex.getMessage());			
			
		} catch (SftpException ex) {
			channel.disconnect();
			session.disconnect();

			// TODO Auto-generated catch block
			ex.printStackTrace();
			throw new IOException(ex.getMessage());

		}		
		return true;
		
	}
	
	   public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {

	        @Override
	        public String getPassphrase() {
	            return null;
	        }
	        @Override
	        public String getPassword() {
	            return null;
	        }
	        @Override
	        public boolean promptPassphrase(String arg0) {
	            return false;
	        }
	        @Override
	        public boolean promptPassword(String arg0) {
	            return false;
	        }
	        @Override
	        public boolean promptYesNo(String arg0) {
	            return false;
	        }
	        @Override
	        public void showMessage(String arg0) {
	        }
	        @Override
	        public String[] promptKeyboardInteractive(String arg0, String arg1,
	                                                  String arg2, String[] arg3, boolean[] arg4) {
	            return null;
	        }
	    }

		 
}


