package de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions;

/**
 * An OpenFlow strip VLAN action.
 *
 * @author Amaury Van Bemten
 */
public class OFActionStripVlan extends OFActionStructure {
    public OFActionStripVlan() {
        super(OFActionStructure.STRIP_VLAN);
        this.bytes = new byte[8];
        bytes[0] = (byte) (type >>> 8);
        bytes[1] = (byte) (type);
        bytes[2] = 0x00; // length is 8
        bytes[3] = 0x08;
        // 4 remaining stuff is padding
    }

    @Override
    public byte[] toBytes() {
        return bytes;
    }
}
