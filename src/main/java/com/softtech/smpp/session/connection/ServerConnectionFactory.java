package com.softtech.smpp.session.connection;

import java.io.IOException;

/**
 * @author SUTHAR
 *
 */
public interface ServerConnectionFactory {
    ServerConnection listen(int port) throws IOException;
    ServerConnection listen(int port, int timeout) throws IOException;
    ServerConnection listen(int port, int timeout, int backlog) throws IOException;
}
