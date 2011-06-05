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

    @Override
    public void send(String message) {
        sendUnencoded(SocketIOUtils.encode(message));
    }


    @Override
    public void heartbeat() {
        if(this.beat > 0) {
            this.beat++;
        }
    }

    @Override
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

    @Override
    public ChannelHandlerContext getCTX() {
        return this.ctx;
    }

    @Override
    public String getSessionID() {
        return this.uID;
    }

    @Override
    public void disconnect() {
        Channel chan = ctx.getChannel();
        if(chan.isOpen()) {
           chan.disconnect();
        }
        this.open = false;
    }

    @Override
    public abstract void sendUnencoded(String message);
}
