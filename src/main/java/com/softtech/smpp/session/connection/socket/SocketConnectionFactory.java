package com.softtech.smpp.session.connection.socket;

import java.io.IOException;
import java.net.Socket;

import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.session.connection.ConnectionFactory;

/**
 * @author SUTHAR
 *
 */
public class SocketConnectionFactory implements ConnectionFactory {
    private static final SocketConnectionFactory connFactory = new SocketConnectionFactory();
    
    private SocketConnectionFactory() {
    }
    
    public static SocketConnectionFactory getInstance() {
        return connFactory;
    }

    @Override
    public Connection createConnection(String host, int port)
            throws IOException {
        return new SocketConnection(new Socket(host, port));
    }
}
