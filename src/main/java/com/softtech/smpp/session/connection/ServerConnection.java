package com.softtech.smpp.session.connection;

import java.io.IOException;

/**
 * @author SUTHAR
 *
 */
public interface ServerConnection {
    Connection accept() throws IOException;
    
    void setSoTimeout(int timeout) throws IOException;
    
    int getSoTimeout() throws IOException;
    
    void close() throws IOException;
}
