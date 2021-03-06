/*
   Copyright (c) 2017 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncSharedPoolImpl;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolLifecycle;
import com.linkedin.r2.transport.http.client.NoopRateLimiter;
import com.linkedin.r2.transport.http.client.stream.http.HttpNettyStreamClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * It generates Pools of Channels for {@link HttpNettyStreamClient}
 */
public class Http2NettyStreamChannelPoolFactory implements ChannelPoolFactory
{
  private final Bootstrap _bootstrap;
  private final long _idleTimeout;
  private final int _maxPoolWaiterSize;
  private final boolean _tcpNoDelay;
  private final ChannelGroup _allChannels;
  private final ScheduledExecutorService _scheduler;

  public Http2NettyStreamChannelPoolFactory(
    long idleTimeout,
    int maxPoolWaiterSize,
    boolean tcpNoDelay,
    ScheduledExecutorService scheduler,
    SSLContext sslContext,
    SSLParameters sslParameters,
    int gracefulShutdownTimeout,
    int maxHeaderSize,
    int maxChunkSize,
    long maxResponseSize,
    EventLoopGroup eventLoopGroup,
    ChannelGroup channelGroup)
  {
    ChannelInitializer<NioSocketChannel> initializer = new Http2ClientPipelineInitializer(
      sslContext, sslParameters, maxHeaderSize, maxChunkSize, maxResponseSize, gracefulShutdownTimeout);

    _bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class).handler(initializer);
    _idleTimeout = idleTimeout;
    _maxPoolWaiterSize = maxPoolWaiterSize;
    _tcpNoDelay = tcpNoDelay;
    _allChannels = channelGroup;
    _scheduler = scheduler;
  }

  @Override
  public AsyncPool<Channel> getPool(SocketAddress address)
  {
    return new AsyncSharedPoolImpl<>(
      address.toString() + " HTTP connection pool",
      new ChannelPoolLifecycle(
        address,
        _bootstrap,
        _allChannels,
        _tcpNoDelay),
      _scheduler,
      new NoopRateLimiter(),
      _idleTimeout,
      _maxPoolWaiterSize);
  }
}
