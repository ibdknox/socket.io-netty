package com.ibdknox.socket_io_netty;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import com.ibdknox.socket_io_netty.flashpolicy.FlashPolicyServer;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class NSIOServer {

    private ServerBootstrap bootstrap;
    private Channel serverChannel;
    private int port;
    private boolean running;
    private INSIOHandler handler;
    private WebSocketServerHandler socketHandler;

    public NSIOServer(INSIOHandler handler, int port) {
        this.port = port;
        this.handler = handler;
        this.running = false;
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
    }

    public boolean isRunning() {
        return this.running;
    }

    public void start() {
        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        socketHandler = new WebSocketServerHandler(handler);
        bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory(socketHandler));
        // Bind and start to accept incoming connections.
        this.serverChannel = bootstrap.bind(new InetSocketAddress(port));
        this.running = true;
        try {
            FlashPolicyServer.start();
        } catch (Exception e) { //TODO: this should not be exception
            System.out.println("You must run as sudo for flash policy server. X-Domain flash will not currently work.");
        }
        System.out.println("Server Started at port ["+ port + "]");
    }

    public void stop() {
        if(!this.running) return;

        System.out.println("Server shutting down.");
        this.socketHandler.prepShutDown();
        this.handler.OnShutdown();
        this.serverChannel.close();
        this.bootstrap.releaseExternalResources();
        System.out.println("**SHUTDOWN**");
        this.serverChannel = null;
        this.bootstrap = null;
        this.running = false;
    }

}
