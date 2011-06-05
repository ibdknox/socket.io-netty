package com.ibdknox.socket_io_netty;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

public abstract class GenericIOClient implements INSIOClient {

    protected ChannelHandlerContext ctx;
    protected int beat;
    protected String uID;
    protected boolean open = false;

    public GenericIOClient(ChannelHandlerContext ctx, String uID) {
        this.ctx = ctx;
        this.uID = uID;
        this.open = true;
    }

    public void send(String message) {
        sendUnencoded(SocketIOUtils.encode(message));
    }

    public void heartbeat() {
        if(this.beat > 0) {
            this.beat++;
        }
    }

    public boolean heartbeat(int beat) {
        if(!this.open) return false;

        int lastBeat = beat - 1;
        if(this.beat == 0 || this.beat > beat) {
            this.beat = beat;
        } else if(this.beat < lastBeat) {
            //we're 2 beats behind..
            return false;
        }
        return true;
    }

    public ChannelHandlerContext getCTX() {
        return this.ctx;
    }

    public String getSessionID() {
        return this.uID;
    }

    public void disconnect() {
        Channel chan = ctx.getChannel();
        if(chan.isOpen()) {
           chan.close();
        }
        this.open = false;
    }

    public abstract void sendUnencoded(String message);
}
