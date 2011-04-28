package com.ibdknox.socket_io_netty;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import com.ibdknox.socket_io_netty.flashpolicy.FlashPolicyServer;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;



public class NSIOServer {
    
	/**
	 * @param args
	 */
	public static void start(INSIOHandler nsioHandler, int port) {
		// TODO Auto-generated method stub
		ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        WebSocketServerHandler handler = new WebSocketServerHandler(nsioHandler);
        bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory(handler));

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(port));
        FlashPolicyServer.start();
        System.out.println("Server Started at port ["+ port + "]");
        
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(nsioHandler));
        
	}

}
