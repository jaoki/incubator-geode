package com.gemstone.org.jgroups.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public interface SockCreator {

  boolean useSSL();

  Socket connect(InetAddress ipAddress, int port, int connectTimeout,
      ConnectionWatcher watcher, boolean clientToServer, int timeout, boolean useSSL) throws IOException;

  boolean isHostReachable(InetAddress ipAddress);

  Socket connect(InetAddress ipAddress, int port, int timeout,
      ConnectionWatcher watcher, boolean clientToServer
      ) throws IOException;

}
