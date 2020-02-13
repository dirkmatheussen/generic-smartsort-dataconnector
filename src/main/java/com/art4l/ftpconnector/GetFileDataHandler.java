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
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;

import javax.annotation.PostConstruct;

@Service
public class GetFileDataHandler extends AbstractFTPHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final CommandType COMMAND_TYPE = CommandType.GET_DATAFILE;

    private static final CommandEventType ON_RECEIVE_EVENT = CommandEventType.ON_RECEIVE_GET_DATAFILE_COMMAND;
    private static final CommandEventType BEFORE_REPLY_EVENT = CommandEventType.BEFORE_REPLY_GET_DATAFILE_COMMAND;
    
    private final BackendConfig mBackendConfig;

//    protected List<String> topics;

 	@Autowired
 	public GetFileDataHandler(BackendConfig backendConfig) {
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

						//TODO License check terugzetten
						licenseKey = (String)commandEvent.getProcessVariables().get("licenseKey");

						log.info("Compare: "+ licenseKey + " " + commandEvent.getProcessVariables().get("licenseKey") );

						if (!((String)commandEvent.getProcessVariables().get("licenseKey")).equalsIgnoreCase(licenseKey)) {

							response.setStatus(ResponseStatus.FAILED.getStatus());
							response.getProcessVariables().put("applicationError", "Invalid license!");


						} else {



							// Get command variables
							final String fileName = (String) commandEvent.getProcessVariables().get("filename");
							final String pathName = (String) commandEvent.getProcessVariables().get("pathname");
							final String ftpUsername = (String) commandEvent.getProcessVariables().get("username");
							final String ftpPassword = (String) commandEvent.getProcessVariables().get("password");


							// Get the data from the server
							try {
								Optional<BufferedReader> dataStream = getFileData(pathName, fileName, ftpUsername, ftpPassword);
								if (dataStream.isPresent()) {
									switch (getType(dataStream.get())){
										case "csv":
											List<LinkedHashMap<String, String>> backendData = getBackendCSVData(dataStream.get());
											if (backendData != null) {

												response.setStatus(ResponseStatus.SUCCEEDED.getStatus());
												response.getProcessVariables().put("backendData", backendData);

												log.info(COMMAND_TYPE + " success, datafile returned: ");
											} else {
												response.setStatus(ResponseStatus.FAILED.getStatus());
												// Add user error information to the response for user feedback
												response.getProcessVariables().put("userError", "Data not found");
											}
											break;
										case "json":
										case "xml":
											String backendRawData = dataStream.get().toString();
												response.setStatus(ResponseStatus.SUCCEEDED.getStatus());
												response.getProcessVariables().put("backendData", backendRawData);
												log.info(COMMAND_TYPE + " success, XML or Json datafile returned: ");
									}
								} else {
									response.setStatus(ResponseStatus.FAILED.getStatus());
									// Add user error information to the response for user feedback
									response.getProcessVariables().put("userError", "Data not found");
								}

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
    
	private Optional<BufferedReader> getFileData(String pathName, String fileName, String ftpUsername, String ftpPassword){
		File backendFile ;
		BufferedReader br = null;;

		String folder = mBackendConfig.getFolder();

		if (!folder.isEmpty()) {
			try {
				File dataFolder = new File(folder);
				backendFile= new File(dataFolder,fileName);
				br = new BufferedReader(new FileReader(backendFile));
			}catch (FileNotFoundException e) {

				e.printStackTrace();
				return Optional.empty();
			}
		} else {

			String ftpUrl = mBackendConfig.getFtpurl();
			String ftpPort = mBackendConfig.getFtpport();
			boolean isSftp = mBackendConfig.isSftp();

			try {
				if (isSftp) {
					br = new BufferedReader(new InputStreamReader(getSFtpFile(ftpUrl, ftpPort, ftpUsername, ftpPassword, pathName, fileName), StandardCharsets.UTF_8));
				} else {
					br = new BufferedReader(new InputStreamReader(getFtpFile(ftpUrl, ftpPort, ftpUsername, ftpPassword, pathName, fileName), StandardCharsets.UTF_8));
				}

				br.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return Optional.empty();
			}

		}

		return Optional.of(br);

	}


    private String getType(BufferedReader br){
 		String returnType = "csv";
 		if (br.toString().startsWith("<?xml")) {returnType = "xml";}
 		else if (isJsonValid(br.toString())) {returnType= "json";}
 		return returnType;
	}


	private static boolean isJsonValid(String jsonString){
 		try{
 			new Gson().fromJson(jsonString,Object.class);
		} catch (JsonSyntaxException ex){
 			return false;
		}
 		return true;
	}

	 private List<LinkedHashMap<String,String>> getBackendCSVData(BufferedReader br){
		 	
		 List<LinkedHashMap<String, String>> rows = new ArrayList<LinkedHashMap<String, String>>();

		 LinkedHashMap<String, String> row;

		 File backendFile ;

		 try {
		 	    String line = br.readLine();
	 	    	line = line.replace("\"","");			//remove all quotes
	 	    	String[] headerParsed = line.split("[;]");
	 	    	//first line is the header line
	 	    		 	    	
		 	    while ((line = br.readLine()) != null) {
		 	    	line = line.replace("\"","");			//remove all quotes
		 	    	String [] parsed = line.split("[;]");
	 	    		row = new LinkedHashMap<String, String>();
		 	    	
		 	    	for (int i = 0;i<parsed.length;i++) {
		 	    		row.put(headerParsed[i],parsed[i]);
		 	    		
		 	    	}
		 	    	
		 	    	rows.add(row);
		 	    }
		 	    br.close();
	 	} catch (IOException e) {
		 		return null;
		}
		   		 	
	 	return rows;
    
	}


	private InputStream getFtpFile(String url, String port, String username, String password, String pathName, String fileName ) throws IOException{
		
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
        
        fileName = pathName + fileName;
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();        
        boolean success = ftp.retrieveFile(fileName, outputStream);

		if (success) {
			System.out.println("File "+ fileName +" has been downloaded successfully.");
		}
        InputStream returnStream = new ByteArrayInputStream(outputStream.toByteArray());        

		returnStream.close();
		        
		ftp.disconnect();
		outputStream.close();
		return returnStream;
		
	}
	

	
	private InputStream getSFtpFile(String url, String port, String username, String password, String pathName, String fileName ) throws IOException{

		JSch jsch = new JSch();
	    Session session = null; 
	    Channel channel = null;
		InputStream returnStream = null;
		
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
	
	
	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); 
	        
	        fileName = pathName + fileName;
	        sftpChannel.get(fileName, outputStream);
	        
	        returnStream = new ByteArrayInputStream(outputStream.toByteArray());        
	
			returnStream.close();        
			outputStream.close();
			channel.disconnect();
			session.disconnect();
			
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
			throw new IOException("Wront portnumber");
			
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
		return returnStream;
		
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


