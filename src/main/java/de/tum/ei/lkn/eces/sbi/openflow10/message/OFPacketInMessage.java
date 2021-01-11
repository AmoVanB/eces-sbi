package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;
import de.tum.ei.lkn.eces.sbi.openflow10.exception.IncorrectOFFormatException;
import de.tum.ei.lkn.eces.sbi.openflow10.util.BytesUtils;

/**
 * An OpenFlow PACKET_IN message.
 *
 * @author Amaury Van Bemten
 */
public class OFPacketInMessage extends OFMessage {
    protected int bufferId;
    protected int totalLength;
    protected int inPort;
    protected byte reason;
    protected byte[] data;

    OFPacketInMessage(long xid, byte[] payload) throws IncorrectOFFormatException {
        super(OFVersion.OF10, OFMessage.OF_PACKET_IN, xid, payload);
        if(payload.length < 10)
            throw new IncorrectOFFormatException("PACKET_IN is at least 10 bytes (+ packet bytes)");

        bufferId = (int) BytesUtils.bytesToLong(payload, 0, 4);
        totalLength = (int) BytesUtils.bytesToLong(payload, 4, 6);
        data = new byte[totalLength];
        inPort = (int) BytesUtils.bytesToLong(payload, 6, 8);
        reason = payload[8];
        // 9 is a padding byte
        if(payload.length != 10 + totalLength)
            throw new IncorrectOFFormatException("PACKET_IN length should be 10 + packet bytes size (" + (10 + totalLength) + ") and not " + payload.length);
        System.arraycopy(payload, 10, data, 0, totalLength);
    }

    public int getInPort() {
        return inPort;
    }

    public byte[] getData() {
        return data;
    }
}
