package com.ibdknox.socket_io_netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;


public interface INSIOHandler {
	void OnConnect(INSIOClient ws);
	void OnMessage(INSIOClient ws, String message);
	void OnDisconnect(INSIOClient ws);
    void OnShutdown();
}
