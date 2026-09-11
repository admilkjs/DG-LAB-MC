package dglabmc.device;

import dglabmc.DgLabMcMod;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class DeviceWebSocketServer {
    private final DeviceSessionManager sessionManager;
    private final String fallbackClientId;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private int boundPort = -1;
    private String boundHost = "";
    private String lastErrorMessage = "";

    public DeviceWebSocketServer(DeviceSessionManager sessionManager, String fallbackClientId) {
        this.sessionManager = sessionManager;
        this.fallbackClientId = fallbackClientId;
    }

    public synchronized void start(int port) {
        start("0.0.0.0", port);
    }

    public synchronized void start(String host, int port) {
        if (serverChannel != null) {
            return;
        }
        clearLastError();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        try {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketServerProtocolHandler("/", null, true, 65536, false, true));
                            pipeline.addLast(new SessionHandler());
                        } catch (Throwable throwable) {
                            recordError("DG-LAB WebSocket pipeline init failed", throwable);
                            throw throwable;
                        }
                    }
                });
            serverChannel = bootstrap.bind(host, port).syncUninterruptibly().channel();
            boundPort = port;
            boundHost = host == null ? "" : host;
            DgLabMcMod.LOGGER.info("DG-LAB WebSocket server started on {}:{}", host, Integer.valueOf(port));
        } catch (RuntimeException exception) {
            recordError("DG-LAB WebSocket server failed to start", exception);
            shutdownGroups();
            throw exception;
        }
    }

    public synchronized void stop() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        shutdownGroups();
        boundPort = -1;
        boundHost = "";
    }

    public synchronized int getBoundPort() {
        return boundPort;
    }

    public synchronized String getBoundHost() {
        return boundHost;
    }

    public synchronized boolean isRunning() {
        return serverChannel != null && boundPort > 0;
    }

    public synchronized String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private void shutdownGroups() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
    }

    private synchronized void clearLastError() {
        lastErrorMessage = "";
    }

    private synchronized void recordError(String prefix, Throwable throwable) {
        String detail = throwable == null ? "" : throwable.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            detail = throwable == null ? "" : throwable.getClass().getSimpleName();
        }
        if (detail == null || detail.trim().isEmpty()) {
            lastErrorMessage = prefix;
            return;
        }
        lastErrorMessage = prefix + ": " + detail.trim();
    }

    private final class SessionHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
            sessionManager.onMessage(context.channel(), frame.text());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
            if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                WebSocketServerProtocolHandler.HandshakeComplete handshake = (WebSocketServerProtocolHandler.HandshakeComplete) event;
                sessionManager.onHandshakeComplete(context.channel(), handshake.requestUri(), fallbackClientId);
                return;
            }
            super.userEventTriggered(context, event);
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) throws Exception {
            sessionManager.onDisconnect(context.channel());
            super.channelInactive(context);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
            recordError("DG-LAB socket error", throwable);
            DgLabMcMod.LOGGER.error("DG-LAB socket error", throwable);
            sessionManager.onDisconnect(context.channel());
            context.close();
        }
    }
}
