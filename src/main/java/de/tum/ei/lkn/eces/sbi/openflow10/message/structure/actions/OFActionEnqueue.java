package de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions;

/**
 * An OpenFlow enqueue action.
 *
 * @author Amaury Van Bemten
 */
public class OFActionEnqueue extends OFActionStructure {
    private int port;
    private long queueId;

    public OFActionEnqueue(int port, long queueId) {
        super(OFActionStructure.ENQUEUE);

        this.port = port;
        this.queueId = queueId;
        this.bytes = new byte[16];
        bytes[0] = (byte) (type >>> 8);
        bytes[1] = (byte) (type);
        bytes[2] = (byte) (this.getLength() >>> 8);
        bytes[3] = (byte) (this.getLength());
        bytes[4] = (byte) (port >>> 8);
        bytes[5] = (byte) (port);
        // 6 -> 12[: padding
        bytes[12] = (byte) (queueId >>> 24);
        bytes[13] = (byte) (queueId >>> 16);
        bytes[14] = (byte) (queueId >>> 8);
        bytes[15] = (byte) (queueId);
    }

    @Override
    public byte[] toBytes() {
        return bytes;
    }
}
