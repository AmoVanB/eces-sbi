package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;

/**
 * An OpenFlow ECHO_REQUEST message.
 *
 * @author Amaury Van Bemten
 */
public class OFEchoRequestMessage extends OFMessage {
    OFEchoRequestMessage(long xid, byte[] payload) {
        super(OFVersion.OF10, OFMessage.OF_ECHO_REQUEST, xid, payload);
    }

    OFEchoRequestMessage(long xid) {
        this(xid, new byte[0]);
    }
}
