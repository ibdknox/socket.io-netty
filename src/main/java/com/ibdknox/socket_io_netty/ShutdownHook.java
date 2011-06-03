package com.ibdknox.socket_io_netty;

public class ShutdownHook extends java.lang.Thread {

    private NSIOServer server;

    public ShutdownHook(NSIOServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        server.stop();
    }


}
