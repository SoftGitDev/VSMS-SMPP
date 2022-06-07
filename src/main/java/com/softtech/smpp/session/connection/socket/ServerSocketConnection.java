package com.softtech.smpp.session.connection.socket;

import java.io.IOException;
import java.net.ServerSocket;

import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.session.connection.ServerConnection;

/**
 * @author SUTHAR
 *
 */
public class ServerSocketConnection implements ServerConnection {
    private final ServerSocket serverSocket;
    
    public ServerSocketConnection(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    
    public void setSoTimeout(int timeout) throws IOException {
        serverSocket.setSoTimeout(timeout);
    }
    
    public int getSoTimeout() throws IOException {
        return serverSocket.getSoTimeout();
    }
    
    public Connection accept() throws IOException {
        return new SocketConnection(serverSocket.accept());
    }
    
    public void close() throws IOException {
        serverSocket.close();
    }
}
