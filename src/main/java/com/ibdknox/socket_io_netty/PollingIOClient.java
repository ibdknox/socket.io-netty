package com.ibdknox.socket_io_netty;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;


public class PollingIOClient extends GenericIOClient {

    private List<String> queue;
    private HttpRequest req;
    private boolean connected;

    public PollingIOClient(ChannelHandlerContext ctx, String uID) {
        super(ctx, uID);
        queue = new LinkedList<String>();
    }

    public void Reconnect(ChannelHandlerContext ctx, HttpRequest req) {
        this.ctx = ctx;
        this.req = req;
        this.connected = true;
        _payload();
    }

    private void _payload() {
        if(!connected || queue.isEmpty()) return;
        //TODO: is this necessary to synchronize?
        synchronized(queue) {
            StringBuilder sb = new StringBuilder();
            for(String message : queue) {
                sb.append(message);
            }
            _write(sb.toString());
            queue.clear();
        }
    }

    private void _write(String message) {
        if(!this.open) return;

        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        res.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Credentials", "true");
        res.addHeader("Connection", "keep-alive");

        res.setContent(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));
        setContentLength(res, res.getContent().readableBytes());

        // Send the response and close the connection if necessary.
        Channel chan = ctx.getChannel();
        if(chan.isOpen()) {
            ChannelFuture f = chan.write(res);
            if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }

        this.connected = false;
    }

    @Override
    public void sendUnencoded(String message) {
        this.queue.add(message);
        _payload();
    }

    public void sendPulse() {
        if(connected) {
            _write("");
        }
    }

}
