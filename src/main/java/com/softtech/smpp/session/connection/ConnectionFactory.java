package com.softtech.smpp.session.connection;

import java.io.IOException;

/**
 * @author SUTHAR
 *
 */
public interface ConnectionFactory {
    Connection createConnection(String host, int port) throws IOException;
}
