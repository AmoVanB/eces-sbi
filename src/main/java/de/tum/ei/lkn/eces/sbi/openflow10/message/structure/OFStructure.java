package de.tum.ei.lkn.eces.sbi.openflow10.message.structure;

/**
 * Abstract class for OpenFlow structures within OpenFlow messages (ports, flows, etc.)
 *
 * @author Amaury Van Bemten
 */
public abstract class OFStructure {
    protected byte[] bytes;

    public byte[] toBytes() {
        return bytes;
    }
}
