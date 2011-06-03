package com.ibdknox.socket_io_netty.flashpolicy;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class FlashPolicyServer {

    public static Channel serverChannel;
    public static ServerBootstrap bootstrap;

    public static void start() {
        // Configure the server.
        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new FlashPolicyServerPipelineFactory());

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        // Bind and start to accept incoming connections.
        serverChannel = bootstrap.bind(new InetSocketAddress(843));
    }

    public static void stop() {
        serverChannel.close();
        bootstrap.releaseExternalResources();
    }

}
