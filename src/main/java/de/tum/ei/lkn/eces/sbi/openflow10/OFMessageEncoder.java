package de.tum.ei.lkn.eces.sbi.openflow10;

import de.tum.ei.lkn.eces.sbi.openflow10.message.OFMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty encoder for writing an OpenFlow message to the wire.
 *
 * @author Amaury Van Bemten
 */
public class OFMessageEncoder extends MessageToByteEncoder<OFMessage> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, OFMessage ofMessage, ByteBuf byteBuf) {
        byteBuf.writeBytes(ofMessage.toBytes());
    }
}
