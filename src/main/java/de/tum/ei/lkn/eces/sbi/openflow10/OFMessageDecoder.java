package de.tum.ei.lkn.eces.sbi.openflow10;

import de.tum.ei.lkn.eces.sbi.openflow10.exception.IncorrectOFFormatException;
import de.tum.ei.lkn.eces.sbi.openflow10.exception.UnsupportedOFVersionException;
import de.tum.ei.lkn.eces.sbi.openflow10.message.OFMessage;
import de.tum.ei.lkn.eces.sbi.openflow10.message.OFMessageFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Netty decoder for creating an OpenFlow message out of the bytes received on the wire.
 *
 * @author Amaury Van Bemten
 */
public class OFMessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = Logger.getLogger(OFMessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        if (byteBuf.readableBytes() < 8)
            return;

        // We have at least the OF header, let's parse it
        byteBuf.markReaderIndex();
        ByteBuf ofHeader = byteBuf.readBytes(8);
        short version = ofHeader.readUnsignedByte();
        short type = ofHeader.readUnsignedByte();
        int length = ofHeader.readUnsignedShort();
        long xid = ofHeader.readUnsignedInt();

        // Waiting for the complete packet
        if (byteBuf.readableBytes() < length - 8) {
            byteBuf.resetReaderIndex();
            return;
        }

        // We have the complete packet
        OFMessage message = null;
        try {
            message = OFMessageFactory.getFactory().create(version, type, xid, byteBuf.readBytes(length - 8));
        } catch (UnsupportedOFVersionException e) {
            logger.warn(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress() + " - Version " + version + " unsupported - ignoring message");
            return;
        } catch (IncorrectOFFormatException e) {
            logger.warn(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress() + " - Incorrect OF format: " + e.getMessage() + " - ignoring message");
            return;
        }

        list.add(message);
    }
}
