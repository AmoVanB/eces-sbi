package de.tum.ei.lkn.eces.sbi.openflow10.message;

import java.util.Arrays;

/**
 * An OpenFlow message.
 *
 * @author Amaury Van Bemten
 */
public class OFMessage {
    public static final byte OF_HELLO = 0;
    public static final byte OF_ERROR = 1;
    public static final byte OF_ECHO_REQUEST = 2;
    public static final byte OF_ECHO_REPLY = 3;
    public static final byte OF_FEATURE_REQUEST = 5;
    public static final byte OF_FEATURE_REPLY = 6;
    public static final byte OF_PACKET_IN = 10;
    public static final byte OF_PACKET_OUT = 13;
    public static final byte OF_FLOW_MOD = 14;

    protected short version;
    protected short type;
    protected int length;
    protected long xid;
    protected byte[] payload;

    public OFMessage(short version, short type, long xid, byte[] payload) {
        this.version = version;
        this.type = type;
        this.length = payload.length + 8;
        this.xid = xid;
        this.payload = payload;
    }

    public short getType() {
        return type;
    }

    public long getXid() {
        return xid;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] toBytes() {
        byte[] message = new byte[length];
        message[0] = (byte) version;
        message[1] = (byte) type;
        message[2] = (byte) (length >>> 8);
        message[3] = (byte) (length);
        message[4] = (byte) (xid >>> 24);
        message[5] = (byte) (xid >>> 16);
        message[6] = (byte) (xid >>> 8);
        message[7] = (byte) (xid);
        for(int i = 8; i < length; i++) {
            message[i] = payload[i - 8];
        }

        return message;
    }

    public String toString() {
        return "V: " + (int) version + " T: " + (int) type + " L: " + length + " XID: " + xid + " P: " + Arrays.toString(payload);
    }
}
