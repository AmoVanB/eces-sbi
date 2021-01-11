package de.tum.ei.lkn.eces.sbi.openflow10;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.sbi.openflow10.components.DetectedLink;
import de.tum.ei.lkn.eces.sbi.openflow10.components.OFSwitch;
import de.tum.ei.lkn.eces.sbi.openflow10.mappers.DetectedLinkMapper;
import de.tum.ei.lkn.eces.sbi.openflow10.mappers.OFSwitchMapper;
import de.tum.ei.lkn.eces.sbi.openflow10.message.*;
import de.tum.ei.lkn.eces.sbi.openflow10.util.BytesUtils;
import de.tum.ei.lkn.eces.sbi.openflow10.util.LLDPUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * OpenFlow connection to a particular switch.
 *
 * @author Amaury Van Bemten
 */
public class OFSwitchConnection extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(OFSwitchConnection.class);
    private Controller ecesController;
    private OFSwitchMapper ofSwitchMapper;
    private DetectedLinkMapper detectedLinkMapper;
    private OFConnectionState ofConnectionState;
    private OFController ofController;

    public OFSwitchConnection(Controller ecesController, OFController ofController) {
        this.ecesController = ecesController;
        this.ofController = ofController;
        this.ofSwitchMapper = new OFSwitchMapper(ecesController);
        this.detectedLinkMapper = new DetectedLinkMapper(ecesController);
        this.ofConnectionState = OFConnectionState.WAITING_HELLO;
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        String msg = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getCanonicalHostName() + " - connection lost!";
        if(ofController.getExpectedSwitches().contains(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress()))
            logger.error(msg);
        else
            logger.debug(msg);
    }

    // handling a message
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        OFMessage messageToHandle = (OFMessage) msg;

        if(!ofController.getExpectedSwitches().contains(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress())) {
            logger.debug("Received connection from " + ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getCanonicalHostName() + " but it is not in the list of switches, ignoring!");
            ctx.close();
            ReferenceCountUtil.release(msg);
            return;
        }

        try {
            switch (messageToHandle.getType()) {
                case OFMessage.OF_HELLO:
                    if(ofConnectionState != OFConnectionState.WAITING_HELLO) {
                        logger.error("Received HELLO while connection in " + ofConnectionState + " state");
                        break;
                    }

                    ctx.writeAndFlush(OFMessageFactory.getFactory().createOFHelloMessage(messageToHandle.getXid()));
                    ctx.writeAndFlush(OFMessageFactory.getFactory().createOFFeatureRequestMessage(messageToHandle.getXid() + 1));
                    ofConnectionState = OFConnectionState.WAITING_FEATURE_REPLY;
                    break;

                case OFMessage.OF_FEATURE_REPLY:
                    if(ofConnectionState != OFConnectionState.WAITING_FEATURE_REPLY) {
                        logger.error("Received FEATURE_REPLY while connection in " + ofConnectionState + " state");
                        break;
                    }

                    OFSwitch ofSwitch = new OFSwitch(((SocketChannel) ctx.channel()), ((OFFeatureReplyMessage) messageToHandle).getPorts());
                    ofConnectionState = OFConnectionState.ESTABLISHED;

                    // Run attachment and its listeners in a new thread to avoid blocking the OF connection (can lead to connection loss)
                    new Thread(() -> ofSwitchMapper.attachComponent(ecesController.createEntity(), ofSwitch)).start();

                    break;

                case OFMessage.OF_ECHO_REQUEST:
                    ctx.writeAndFlush(OFMessageFactory.getFactory().createOFEchoReplyMessage(messageToHandle.getXid(), messageToHandle.getPayload()));
                    break;

                case OFMessage.OF_ERROR:
                    String errorType = ((OFErrorMessage) messageToHandle).getErrorTypeString();
                    String errorCode = ((OFErrorMessage) messageToHandle).getErrorCodeString();
                    byte[] data = ((OFErrorMessage) messageToHandle).getData();
                    logger.error(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getCanonicalHostName() + " - error received - " + errorType + "/" + errorCode + ": " + Arrays.toString(data));
                    break;

                case OFMessage.OF_PACKET_IN:
                    if(ofConnectionState != OFConnectionState.ESTABLISHED)  {
                        logger.error("Received PACKET_IN while connection in " + ofConnectionState + " state");
                        break;
                    }

                    byte[] dpPacket = ((OFPacketInMessage) messageToHandle).getData();
                    if(!LLDPUtils.isLLDPPacket(dpPacket)) {
                        logger.error(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getCanonicalHostName() + " - PACKET_IN from port " + ((OFPacketInMessage) messageToHandle).getInPort() + " with non-LLDP packet: type is 0x" + BytesUtils.bytesToHex(new byte[]{dpPacket[12], dpPacket[13]}) + " - this is unexpected");
                        break;
                    }
                    InetAddress sourceIP = LLDPUtils.getIP(dpPacket);
                    int sourcePort = LLDPUtils.getPortNumber(dpPacket);
                    if(sourceIP == null || sourcePort == -1) {
                        logger.error(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getCanonicalHostName() + " - LLDP PACKET_IN from port " + ((OFPacketInMessage) messageToHandle).getInPort() + " has unexpected format: " + BytesUtils.bytesToHex(dpPacket));
                        break;
                    }

                    // Run attachment and its listeners in a new thread to avoid blocking the OF connection (can lead to connection loss)
                    new Thread(() -> detectedLinkMapper.attachComponent(ecesController.createEntity(),
                            new DetectedLink(sourceIP, ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress(),
                                    sourcePort, ((OFPacketInMessage) messageToHandle).getInPort()))).start();
                    break;

                default:
                    logger.error(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getCanonicalHostName() + " - received unsupported message type: " + messageToHandle.getType() + " (" + getOF10TypeName(messageToHandle.getType()) + ")");
                    break;
            }
        }
        finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        logger.error(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress() + " - error: " + cause);
        logger.error(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress() + " - closing!");
        ctx.close();
    }

    private String getOF10TypeName(int type) {
        switch(type) {
            case 0:
                return "OF_HELLO";
            case 1:
                return "OF_ERROR";
            case 2:
                return "OF_ECHO_REQUEST";
            case 3:
                return "OF_ECHO_REPLY";
            case 4:
                return "OF_VENDOR";
            case 5:
                return "OF_FEATURE_REQUEST";
            case 6:
                return "OF_FEATURE_REPLY";
            case 7:
                return "OF_GET_CONFIG_REQUEST";
            case 8:
                return "OF_GET_CONFIG_REPLY";
            case 9:
                return "OF_SET_CONFIG";
            case 10:
                return "OF_PACKET_IN";
            case 11:
                return "OF_FLOW_REMOVED";
            case 12:
                return "OF_PORT_STATUS";
            case 13:
                return "OF_PACKET_OUT";
            case 14:
                return "OF_FLOW_MOD";
            case 15:
                return "OF_PORT_MOD";
            case 16:
                return "OF_STATS_REQUEST";
            case 17:
                return "OF_STATS_REPLY";
            case 18:
                return "OF_BARRIER_REQUEST";
            case 19:
                return "OF_BARRIER_REPLY";
            case 20:
                return "OF_QUEUE_GET_CONFIG_REQUEST";
            case 21:
                return "OF_QUEUE_GET_CONFIG_REPLY";
            default:
                return "unknown";
        }
    }
}

enum OFConnectionState {
    WAITING_HELLO,
    WAITING_FEATURE_REPLY,
    ESTABLISHED,
}