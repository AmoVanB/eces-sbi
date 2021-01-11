package de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions;

/**
 * An OpenFlow output action.
 *
 * @author Amaury Van Bemten
 */
public class OFActionOutput extends OFActionStructure {
    public final static int CONTROLLER = 0xfffd;
    private int port;
    private int maxLen;

    public OFActionOutput(int port) {
        this(port, 0xffff);
    }

    public OFActionOutput(int port, int maxLen) {
        super(OFActionStructure.OUTPUT);
        this.port = port;
        this.maxLen = maxLen;
        this.bytes = new byte[8];
        bytes[0] = (byte) (type >>> 8);
        bytes[1] = (byte) (type);
        bytes[2] = (byte) (this.getLength() >>> 8);
        bytes[3] = (byte) (this.getLength());
        bytes[4] = (byte) (port >>> 8);
        bytes[5] = (byte) (port);
        bytes[6] = (byte) (maxLen >>> 8);
        bytes[7] = (byte) (maxLen);
    }

    @Override
    public byte[] toBytes() {
        return bytes;
    }
}
