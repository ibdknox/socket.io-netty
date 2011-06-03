package com.ibdknox.socket_io_netty;

import org.jboss.netty.channel.ChannelHandlerContext;

public interface INSIOClient {

    void send(String message);
    void sendUnencoded(String message);
    boolean heartbeat(int beat);
    void heartbeat();
    void disconnect();
    String getSessionID();
    ChannelHandlerContext getCTX();
}
