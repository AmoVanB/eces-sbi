package de.tum.ei.lkn.eces.sbi.openflow10;

import de.tum.ei.lkn.eces.core.Controller;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.util.Set;

/**
 * OpenFlow Controller based on Netty.
 *
 * @author Amaury Van Bemten
 */
public class OFController implements Runnable {
    private static final Logger logger = Logger.getLogger(OFController.class);
    private final int PORT_NUMBER;
    private Controller ecesController;
    private Set<InetAddress> expectedSwitches;

    public OFController(Controller ecesController, int port, Set<InetAddress> expectedSwitches) {
        this.ecesController = ecesController;
        this.PORT_NUMBER = port;
        this.expectedSwitches = expectedSwitches;
    }

    @Override
    public void run() {
        OFController ofController = this;
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // accepts incoming connections
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // handles traffic of accepted connections
        try {
            ServerBootstrap b = new ServerBootstrap(); // sets up the server

            b.group(bossGroup, workerGroup)
                    // Use the NioServerSocketChannel class which is used to instantiate a new Channel to accept incoming connections
                    .channel(NioServerSocketChannel.class)
                    // Add handlers  (decoder, encoder & msg handler) to the pipeline of each created channel
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new OFMessageDecoder(),
                                    new OFMessageEncoder(),
                                    new OFSwitchConnection(ecesController, ofController));
                        }
                    })
                    // some options (tcp no delay?) for the main channel
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // options for each channel
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(PORT_NUMBER).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Impossible to open socket " + e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public Set<InetAddress> getExpectedSwitches() {
        return expectedSwitches;
    }
}