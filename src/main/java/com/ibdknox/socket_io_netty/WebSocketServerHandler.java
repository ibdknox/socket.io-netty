package com.ibdknox.socket_io_netty;

import static org.jboss.netty.handler.codec.http.HttpHeaders.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.*;
import static org.jboss.netty.handler.codec.http.HttpMethod.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder;
import org.jboss.netty.util.CharsetUtil;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

public class WebSocketServerHandler extends SimpleChannelUpstreamHandler {

    private static final long HEARTBEAT_RATE = 10000;
    private static final String WEBSOCKET_PATH = "/socket.io/websocket";
    private static final String POLLING_PATH = "/socket.io/xhr-polling";
    private static final String FLASHSOCKET_PATH = "/socket.io/flashsocket";


    private INSIOHandler handler;
    public ConcurrentHashMap<ChannelHandlerContext, INSIOClient> clients;
    private Timer heartbeatTimer;
    ConcurrentHashMap<String, PollingIOClient> pollingClients;

    public WebSocketServerHandler(INSIOHandler handler) {
        super();
        this.clients = new ConcurrentHashMap<ChannelHandlerContext, INSIOClient>(20000, 0.75f, 2);
        this.pollingClients = new ConcurrentHashMap<String, PollingIOClient>(20000, 0.75f, 2);
        this.handler = handler;
        this.heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new HeartbeatTask(this), 1000, HEARTBEAT_RATE);
    }


    private String getUniqueID() {
        return UUID.randomUUID().toString();
    }

    private INSIOClient getClientByCTX(ChannelHandlerContext ctx) {
        return clients.get(ctx);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, org.jboss.netty.channel.ChannelStateEvent e) throws Exception {
        INSIOClient client = getClientByCTX(ctx);
        if(client != null) {
            this.disconnect(client);
        }
    };

    public void disconnect(INSIOClient client) {
        client.disconnect();
        if(this.clients.remove(client.getCTX()) == null) {
            this.pollingClients.remove(client.getSessionID());
        }
        handler.OnDisconnect(client);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {

        String reqURI = req.getUri();
        if(reqURI.contains(POLLING_PATH)) {
            String[] parts = reqURI.split("/");
            String ID = parts.length > 3 ? parts[3] : "";
            PollingIOClient client = (PollingIOClient) this.pollingClients.get(ID);

            if(client == null) {
                //new client
                client = connectPoller(ctx);
                client.Reconnect(ctx, req);
                return;
            }

            if(req.getMethod() == GET) {
                client.heartbeat();
                client.Reconnect(ctx, req);
            } else {
                //we got a message
                QueryStringDecoder decoder = new QueryStringDecoder("/?" + req.getContent().toString(CharsetUtil.UTF_8));
                String message = decoder.getParameters().get("data").get(0);
                handleMessage(client, message);

                //make sure the connection is closed once we send a response
                setKeepAlive(req, false);

                //send a response that allows for cross domain access
                HttpResponse resp = new DefaultHttpResponse(HTTP_1_1, OK);
                resp.addHeader("Access-Control-Allow-Origin", "*");
                sendHttpResponse(ctx, req, resp);
            }
            return;
        }

        // Serve the WebSocket handshake request.
        String location = "";
        if(reqURI.equals(WEBSOCKET_PATH)) {
            location = getWebSocketLocation(req);
        } else if(reqURI.equals(FLASHSOCKET_PATH)) {
            location = getFlashSocketLocation(req);
        }
        if (location != "" && 
                Values.UPGRADE.equalsIgnoreCase(req.getHeader(CONNECTION)) &&
                WEBSOCKET.equalsIgnoreCase(req.getHeader(Names.UPGRADE))) {

            // Create the WebSocket handshake response.
            HttpResponse res = new DefaultHttpResponse(
                    HTTP_1_1,
                    new HttpResponseStatus(101, "Web Socket Protocol Handshake"));
            res.addHeader(Names.UPGRADE, WEBSOCKET);
            res.addHeader(CONNECTION, Values.UPGRADE);

            // Fill in the headers and contents depending on handshake method.
            if (req.containsHeader(SEC_WEBSOCKET_KEY1) &&
                    req.containsHeader(SEC_WEBSOCKET_KEY2)) {
                // New handshake method with a challenge:
                res.addHeader(SEC_WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
                res.addHeader(SEC_WEBSOCKET_LOCATION, getWebSocketLocation(req));
                String protocol = req.getHeader(SEC_WEBSOCKET_PROTOCOL);
                if (protocol != null) {
                    res.addHeader(SEC_WEBSOCKET_PROTOCOL, protocol);
                }

                // Calculate the answer of the challenge.
                String key1 = req.getHeader(SEC_WEBSOCKET_KEY1);
                String key2 = req.getHeader(SEC_WEBSOCKET_KEY2);
                int a = (int) (Long.parseLong(key1.replaceAll("[^0-9]", "")) / key1.replaceAll("[^ ]", "").length());
                int b = (int) (Long.parseLong(key2.replaceAll("[^0-9]", "")) / key2.replaceAll("[^ ]", "").length());
                long c = req.getContent().readLong();
                ChannelBuffer input = ChannelBuffers.buffer(16);
                input.writeInt(a);
                input.writeInt(b);
                input.writeLong(c);
                ChannelBuffer output = ChannelBuffers.wrappedBuffer(
                        MessageDigest.getInstance("MD5").digest(input.array()));
                res.setContent(output);
            } else {
                // Old handshake method with no challenge:
                res.addHeader(WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
                res.addHeader(WEBSOCKET_LOCATION, getWebSocketLocation(req));
                String protocol = req.getHeader(WEBSOCKET_PROTOCOL);
                if (protocol != null) {
                    res.addHeader(WEBSOCKET_PROTOCOL, protocol);
                }
            }

            // Upgrade the connection and send the handshake response.
            ChannelPipeline p = ctx.getChannel().getPipeline();
            p.remove("aggregator");
            p.replace("decoder", "wsdecoder", new WebSocketFrameDecoder());

            ctx.getChannel().write(res);

            p.replace("encoder", "wsencoder", new WebSocketFrameEncoder());

            connectSocket(ctx);
            return;
                }

        // Send an error page otherwise.
        sendHttpResponse(
                ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
    }

    private PollingIOClient connectPoller(ChannelHandlerContext ctx) {
        String uID = getUniqueID();
        PollingIOClient client = new PollingIOClient(ctx, uID);
        pollingClients.put(uID, client);
        client.send(uID);
        handler.OnConnect(client);
        return client;
    }

    private void connectSocket(ChannelHandlerContext ctx) {
        String uID = getUniqueID();
        WebSocketIOClient ws = new WebSocketIOClient(ctx, uID);
        clients.put(ctx, ws);
        ws.send(uID);
        handler.OnConnect(ws);
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        INSIOClient client = getClientByCTX(ctx);
        handleMessage(client, frame.getTextData());
    }

    private void handleMessage(INSIOClient client, String message) {
        String decoded = SocketIOUtils.decode(message);
        if(decoded.substring(0, 3).equals("~h~")) {
            client.heartbeat();
        } else {
            handler.OnMessage(client, decoded);
        }
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        // Generate an error page if response status code is not OK (200).
        if (res.getStatus().getCode() != 200) {
            res.setContent(
                    ChannelBuffers.copiedBuffer(
                        res.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(res, res.getContent().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.getChannel().write(res);
        if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void prepShutDown() {
        this.heartbeatTimer.cancel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
        throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }

    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    }

    private String getFlashSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + FLASHSOCKET_PATH;
    }
}
