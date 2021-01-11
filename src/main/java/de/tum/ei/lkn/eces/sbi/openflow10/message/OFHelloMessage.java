package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;

/**
 * An OpenFlow HELLO message.
 *
 * @author Amaury Van Bemten
 */
public class OFHelloMessage extends OFMessage {
    OFHelloMessage(long xid, byte[] payload) {
        super(OFVersion.OF10, OFMessage.OF_HELLO, xid, payload);
    }

    OFHelloMessage(long xid) {
        this(xid, new byte[0]);
    }
}
