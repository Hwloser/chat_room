package com.hwloser;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final ChannelGroup group;

  public TextWebSocketFrameHandler(ChannelGroup group) {
    this.group = group;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    //增加消息的引用计数（保留消息），并将他写到 ChannelGroup 中所有已经连接的客户端
    Channel channel = ctx.channel();
    //自己发送的消息不返回给自己
    group.remove(channel);
    group.writeAndFlush(msg.retain());
    group.add(channel);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    // 是否握手成功，升级为 Websocket 协议
    if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
      // 握手成功，移除 HttpRequestHandler，因此将不会接收到任何消息
      // 并把握手成功的 Channel 加入到 ChannelGroup 中
      ctx.pipeline().remove(HttpRequestHandler.class);
      group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined"));
      group.add(ctx.channel());
    } else if (evt instanceof IdleStateEvent) {
      IdleStateEvent stateEvent = (IdleStateEvent) evt;
      if (stateEvent.state() == IdleState.READER_IDLE) {
        group.remove(ctx.channel());
        ctx.writeAndFlush(new TextWebSocketFrame("由于您长时间不在线，系统已自动把你踢下线！")).addListener(ChannelFutureListener.CLOSE);
      }
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }
}
