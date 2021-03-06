package com.softtech.smpp.session.connection.socket;

import java.io.IOException;
import java.net.ServerSocket;

import com.softtech.smpp.session.connection.ServerConnection;
import com.softtech.smpp.session.connection.ServerConnectionFactory;

/**
 * @author SUTHAR
 *
 */
public class ServerSocketConnectionFactory implements ServerConnectionFactory {
    
    public ServerConnection listen(int port) throws IOException {
        return new ServerSocketConnection(new ServerSocket(port));
    }
    
    public ServerConnection listen(int port, int timeout) throws IOException {
        ServerSocket ss = new ServerSocket(port);
        ss.setSoTimeout(timeout);
        return new ServerSocketConnection(ss);
    }
    
    public ServerConnection listen(int port, int timeout, int backlog) throws IOException {
        ServerSocket ss = new ServerSocket(port, backlog);
        ss.setSoTimeout(timeout);
        return new ServerSocketConnection(ss);
    }
}
