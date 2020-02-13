package com.art4l.tcpserver.service;

import com.art4l.tcpserver.Connection;

public interface TcpListener {

    public void onTcpMessageReceived(String message);

    public boolean acceptMessage(Connection connection, String message);
}
