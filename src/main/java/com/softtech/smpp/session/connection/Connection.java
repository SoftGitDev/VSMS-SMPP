package com.softtech.smpp.session.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

/**
 * Connection object.
 * 
 * @author SUTHAR
 *
 */
public interface Connection {
    boolean isOpen();
    InetAddress getInetAddress();
    InputStream getInputStream();
    OutputStream getOutputStream();
    void setSoTimeout(int timeout) throws IOException;
    void close() throws IOException;
}
