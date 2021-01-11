package de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions;

import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.OFStructure;

/**
 * The OpenFlow action structure.
 *
 * @author Amaury Van Bemten
 */
public abstract class OFActionStructure extends OFStructure {
    protected static final int OUTPUT = 0x0000;
    protected static final int ENQUEUE = 0x000b;
    protected static final int STRIP_VLAN = 0x0003;

    protected int type;

    public OFActionStructure(int type) {
        this.type = type;
    }

    public int getLength() {
        return bytes.length;
    }
}
