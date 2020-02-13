package com.art4l.ftpconnector;

import com.art4l.dataconnector.container.configuration.BackendConfig;
import com.art4l.dataconnector.module.dataconnector.domain.dto.CommandType;
import com.art4l.dataconnector.module.dataconnector.domain.dto.GenericResponse;
import com.art4l.dataconnector.module.dataconnector.domain.dto.ResponseStatus;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEvent;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandEventType;
import com.art4l.dataconnector.module.dataconnector.domain.event.CommandVariable;
import com.art4l.dataconnector.module.dataconnector.domain.event.Subscriber;
import com.art4l.license.LicenseValidator;
import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class PutFileDataHandler extends AbstractFTPHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final CommandType COMMAND_TYPE = CommandType.PUT_DATAFILE;

    private static final CommandEventType ON_RECEIVE_EVENT = CommandEventType.ON_RECEIVE_PUT_DATAFILE_COMMAND;
    private static final CommandEventType BEFORE_REPLY_EVENT = CommandEventType.BEFORE_REPLY_PUT_DATAFILE_COMMAND;

    private final BackendConfig mBackendConfig;


 	@Autowired
 	public PutFileDataHandler(BackendConfig backendConfig) {
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

						//check runtime license value for each device
						String licenseKey = LicenseValidator.getLicenseKey(((String)commandEvent.getProcessVariables().get("locationName")).toLowerCase()
								+ ((String)commandEvent.getProcessVariables().get("deviceType")).toLowerCase()
								+ ((String)commandEvent.getProcessVariables().get("deviceId")).toLowerCase());

						// Get default variables
						final String processInstanceId = commandEvent.getProcessInstanceId();
						final String spanId = commandEvent.getSpanId();

						// Prepare response
						final GenericResponse response = new GenericResponse();
						response.setProcessInstanceId(processInstanceId);
						response.setSpanId(spanId);


						if (!((String)commandEvent.getProcessVariables().get("licenseKey")).equalsIgnoreCase(licenseKey)) {

							response.setStatus(ResponseStatus.FAILED.getStatus());
							response.getProcessVariables().put("applicationError", "Invalid license!");


						} else {



							// Get command variables
							final String fileName = (String) commandEvent.getProcessVariables().get("filename");
							final String pathName = (String) commandEvent.getProcessVariables().get("pathname");
							final String ftpUsername = (String) commandEvent.getProcessVariables().get("username");
							final String ftpPassword = (String) commandEvent.getProcessVariables().get("password");
							final List<LinkedHashMap<String, String>> getCSVList = (List<LinkedHashMap<String, String>>) commandEvent.getCommandVariables().get("putbackendData");


							// Put the data to the server
							try {
								if (putBackendData(pathName, fileName, ftpUsername, ftpPassword,getCSVList)){
									response.setStatus(ResponseStatus.SUCCEEDED.getStatus());

									log.info(COMMAND_TYPE + " success, datafile returned: ");
								} else {
									response.setStatus(ResponseStatus.FAILED.getStatus());
									// Add user error information to the response for user feedback
									response.getProcessVariables().put("userError", "Error writing CSV file");
								}
								// Store command to execute later
							} catch (Exception e) {
								response.setStatus(ResponseStatus.FAILED.getStatus()); // TODO: add info to error so camunda knows this is an application error
								// Add application error information to the response for user feedback
								response.getProcessVariables().put("applicationError", e.getMessage());

							}
						}
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
    

    
	 private boolean putBackendData(String pathName, String fileName, String ftpUsername, String ftpPassword, List<LinkedHashMap<String, String>> csvList){
		 	

 		StringBuilder csvStream = new StringBuilder();

 		//transform the List into a CSV file
		 // extract all headers
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

		 String folder = mBackendConfig.getFolder();

		 if (!folder.isEmpty()) {


		 } else {
		 	try {

				String ftpUrl = mBackendConfig.getFtpurl();
				String ftpPort = mBackendConfig.getFtpport();
				boolean isSFtp = mBackendConfig.isSftp();
				if (isSFtp) return putSFtpFile(ftpUrl, ftpPort, ftpUsername, ftpPassword, pathName, fileName, csvStream.toString());
				return  putFtpFile(ftpUrl, ftpPort, ftpUsername, ftpPassword, pathName, fileName, csvStream.toString());

			} catch(IOException ex){

			}
			 
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

	private boolean putFtpFile(String url, String port, String username, String password, String pathName, String fileName,String csvStream ) throws IOException{

		FTPClient ftp;
		int iPort = Integer.valueOf(port);

		ftp = new FTPClient();

		ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

		ftp.connect(url, iPort);
		int reply = ftp.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			throw new IOException("Exception in connecting to FTP Server");
		}
		ftp.enterLocalPassiveMode();

		ftp.login(username, password);
		ftp.setFileType(FTP.BINARY_FILE_TYPE);

//		fileName = pathName + fileName;
		ftp.changeWorkingDirectory(pathName);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(csvStream.getBytes(StandardCharsets.UTF_8));

//	        fileName = pathName + fileName;
		boolean success = ftp.storeUniqueFile(fileName,inputStream);
		if (success) {
			System.out.println("File "+ fileName +" has been uploaded successfully.");
		}
		inputStream.close();
		ftp.disconnect();

		return success;

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


