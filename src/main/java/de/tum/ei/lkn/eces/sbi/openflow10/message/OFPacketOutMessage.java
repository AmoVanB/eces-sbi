package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions.OFActionStructure;

/**
 * An OpenFlow PACKET_OUT message.
 *
 * @author Amaury Van Bemten
 */
public class OFPacketOutMessage extends OFMessage {
    protected long bufferId;
    protected int inPort;
    protected int actionsLen;
    protected OFActionStructure[] actions;
    protected byte[] data;

    OFPacketOutMessage(long xid, long bufferId, int inPort, OFActionStructure[] actions, byte[] data) {
        super(OFVersion.OF10, OFMessage.OF_PACKET_OUT, xid, new byte[0]);
        actionsLen = 0;
        for(OFActionStructure action : actions)
            actionsLen += action.getLength();
        this.length = 8 + 8 + actionsLen + data.length; // OF_HEADER + PACKET_OUT_HEADER + rest
        this.bufferId = bufferId;
        this.inPort = inPort;
        this.actions = actions;
        this.data = data;

        payload = new byte[this.length - 8];
        payload[0] = (byte) (bufferId >>> 24);
        payload[1] = (byte) (bufferId >>> 16);
        payload[2] = (byte) (bufferId >>> 8);
        payload[3] = (byte) (bufferId);
        payload[4] = (byte) (inPort >>> 8);
        payload[5] = (byte) (inPort);
        payload[6] = (byte) (actionsLen >>> 8);
        payload[7] = (byte) (actionsLen);
        int byteToFill = 8;
        // Fill actions
        for(OFActionStructure action : actions)
            for(byte b : action.toBytes())
                payload[byteToFill++] = b;

        // Fill bytes packet
        for(byte dataB : data)
            payload[byteToFill++] = dataB;
    }
}
