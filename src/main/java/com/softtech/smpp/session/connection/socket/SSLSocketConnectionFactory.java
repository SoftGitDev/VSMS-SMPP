package com.softtech.smpp.session.connection.socket;

import java.io.IOException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.session.connection.ConnectionFactory;

/**
 * @author SUTHAR
 */
public class SSLSocketConnectionFactory implements ConnectionFactory {
  private static final SSLSocketConnectionFactory connFactory = new SSLSocketConnectionFactory();
  private static final SocketFactory socketFactory = SSLSocketFactory.getDefault();

  private SSLSocketConnectionFactory() {
  }

  public static SSLSocketConnectionFactory getInstance() {
    return connFactory;
  }

  @Override
  public Connection createConnection(String host, int port)
      throws IOException {
    return new SocketConnection(socketFactory.createSocket(host, port));
  }
}