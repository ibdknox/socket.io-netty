package com.ibdknox.socket_io_netty;

import java.util.TimerTask;

public class HeartbeatTask extends TimerTask {

    private WebSocketServerHandler server;
    private int heartbeatNum = 0;

    public HeartbeatTask(WebSocketServerHandler server) {
        this.server = server;
    }

    @Override
    public void run() {
        if(server.clients.isEmpty() && server.pollingClients.isEmpty()) return;

        heartbeatNum++;
        String message = SocketIOUtils.encode("~h~" + heartbeatNum);
        for(INSIOClient client : server.clients.values()) {
            if(client.heartbeat(heartbeatNum)) {
                client.sendUnencoded(message);
            } else {
                server.disconnect(client);
            }
        }

        for(PollingIOClient client : server.pollingClients.values()) {
            if(client.heartbeat(heartbeatNum)) {
                client.sendPulse();
            } else {
                server.disconnect(client);
            }
        }
    }
}
