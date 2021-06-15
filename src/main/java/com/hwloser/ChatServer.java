package com.hwloser;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.net.InetSocketAddress;
import java.util.Objects;

public class ChatServer {

  private final ChannelGroup channelGroup = new DefaultChannelGroup(
      ImmediateEventExecutor.INSTANCE);
  private final EventLoopGroup eventLoop = new NioEventLoopGroup();
  private Channel channel;

  private ChannelFuture start(InetSocketAddress address) {
    ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoop)
     .channel(NioServerSocketChannel.class)
     .childHandler(new ChatServerInitializer(channelGroup));

    ChannelFuture channelFuture = b.bind(address);
    channelFuture.syncUninterruptibly();

    // persist channel
    channel = channelFuture.channel();

    return channelFuture;
  }

  private void destroy() {
    if (Objects.nonNull(channel)) {
      channel.close();
    }
    channelGroup.close();
    eventLoop.shutdownGracefully();
  }

  public static void main(String[] args) {
    final ChatServer s = new ChatServer();
    ChannelFuture channelFuture = s.start(new InetSocketAddress(1234));
    Runtime.getRuntime().addShutdownHook(new Thread(s::destroy));

    channelFuture.channel().closeFuture().syncUninterruptibly();
  }
}
