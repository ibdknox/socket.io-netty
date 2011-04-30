package com.ibdknox.socket_io_netty;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import com.ibdknox.socket_io_netty.flashpolicy.FlashPolicyServer;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;


//TODO: turn this into an instanceable class so we can shut it down.
public class NSIOServer {

    private ServerBootstrap bootstrap;
    private Channel serverChannel;
    private int port;
    private INSIOHandler handler;

    public NSIOServer(INSIOHandler handler, int port) {
        this.port = port;
        this.handler = handler;

		bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        WebSocketServerHandler socketHandler = new WebSocketServerHandler(handler);
        bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory(socketHandler));
        
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
    }
    
	/**
	 * @param args
	 */
	public void start() {
        // Bind and start to accept incoming connections.
        this.serverChannel = bootstrap.bind(new InetSocketAddress(port));
        FlashPolicyServer.start();
        System.out.println("Server Started at port ["+ port + "]");
	}

    public void stop() {
        System.out.println("Server shutting down.");
        this.handler.OnShutdown();
        this.serverChannel.close();
        this.bootstrap.releaseExternalResources();
        System.out.println("**SHUTDOWN**");
    }

}
