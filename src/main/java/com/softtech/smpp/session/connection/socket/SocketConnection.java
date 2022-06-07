package com.softtech.smpp.session.connection.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.util.StrictBufferedInputStream;

/**
 * @author SUTHAR
 *
 */
public class SocketConnection implements Connection {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new StrictBufferedInputStream(socket.getInputStream(), 65536);// 64 KB buffer
        this.out = socket.getOutputStream();
    }

    public void setSoTimeout(int timeout) throws IOException {
        socket.setSoTimeout(timeout);
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }
}
