package com.art4l.tcpserver.service;

import com.art4l.tcpserver.Connection;
import com.art4l.tcpserver.TcpController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@TcpController
public class TcpReceiverService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private Map<String,TcpListener> tcpListeners = new HashMap<>();

    @Autowired
    public TcpReceiverService() throws IOException {

    }

    /**
     * Receive data from the an FTP Client
     * Called by @TCPController
     *
     * @param connection
     * @param data
     */
    public void receiveData(Connection connection, byte[] data) {
        final String payload = new String(data).replace("\n", "");

        log.info("datareceived: " + payload);

        tcpListeners.forEach((keyValue,valueMap) ->{
            if (valueMap.acceptMessage(connection,payload)) valueMap.onTcpMessageReceived(payload);
        });
    }

    //set the TCPListener
    public void setTcpListener(String clientIdentifier,TcpListener tcpListener) {
        tcpListeners.put(clientIdentifier,tcpListener);
    }

    public void removeTcpListener(String clientIdentifier){
        if (tcpListeners.containsKey(clientIdentifier)) tcpListeners.remove(clientIdentifier);
    }

    @PreDestroy
    public void cleanup() {
        this.tcpListeners.clear();
    }
}