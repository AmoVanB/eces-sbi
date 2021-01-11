package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;
import de.tum.ei.lkn.eces.sbi.openflow10.exception.IncorrectOFFormatException;
import de.tum.ei.lkn.eces.sbi.openflow10.exception.UnsupportedOFVersionException;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.OFMatchStructure;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions.OFActionStructure;
import io.netty.buffer.ByteBuf;

/**
 * Factory for creating OpenFlow messages.
 *
 * @author Amaury Van Bemten
 */
public class OFMessageFactory {
    private static OFMessageFactory factory;

    public OFMessageFactory() {
        factory = this;
    }

    // We ensure there's only one factory (singleton)
    public static OFMessageFactory getFactory() {
        if (factory == null)
            return new OFMessageFactory();
        return factory;
    }

    public OFMessage create(short version, short type, long xid, ByteBuf payloadBuf) throws UnsupportedOFVersionException, IncorrectOFFormatException {
        if (version != OFVersion.OF10)
            throw new UnsupportedOFVersionException(version);

        byte[] payload = new byte[payloadBuf.readableBytes()];
        payloadBuf.readBytes(payload);

        switch (type) {
            case OFMessage.OF_HELLO:
                return new OFHelloMessage(xid, payload);
            case OFMessage.OF_FEATURE_REQUEST:
                return new OFFeatureRequestMessage(xid);
            case OFMessage.OF_FEATURE_REPLY:
                return new OFFeatureReplyMessage(xid, payload);
            case OFMessage.OF_ERROR:
                return new OFErrorMessage(xid, payload);
            case OFMessage.OF_PACKET_IN:
                return new OFPacketInMessage(xid, payload);
            default:
                return new OFMessage(version, type, xid, payload);
        }
    }

    public OFHelloMessage createOFHelloMessage(long xid) {
        return new OFHelloMessage(xid);
    }

    public OFFeatureRequestMessage createOFFeatureRequestMessage(long xid) {
        return new OFFeatureRequestMessage(xid);
    }

    public OFEchoReplyMessage createOFEchoReplyMessage(long xid, byte[] payload) {
        return new OFEchoReplyMessage(xid, payload);
    }

    public OFPacketOutMessage createPacketOutMessage(long xid, long bufferId, int inPort, OFActionStructure[] actions, byte[] data) {
        return new OFPacketOutMessage(xid, bufferId, inPort, actions, data);
    }

    public OFFlowModMessage createFlowModAddMessage(long xid, OFMatchStructure match, OFActionStructure[] actions, int priority) {
        return new OFFlowModMessage(xid, match, 0x0000000000000000, OFFlowModMessage.FLOW_MOD_ADD, 0, 0, priority, 0xffffffff, 0xffff, 0x0000, actions);
    }

    public OFFlowModMessage createFlowModDeleteAllMessage(long xid) {
        return new OFFlowModMessage(xid, OFMatchStructure.getAllMatch(), 0x0000000000000000, OFFlowModMessage.FLOW_MOD_DELETE, 0, 0, 0, 0xffffffff, 0xffff, 0x0000, new OFActionStructure[0]);

    }
}