package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;

/**
 * An OpenFlow ECHO_REPLY message.
 *
 * @author Amaury Van Bemten
 */
public class OFEchoReplyMessage extends OFMessage {
    OFEchoReplyMessage(long xid, byte[] payload) {
        super(OFVersion.OF10, OFMessage.OF_ECHO_REPLY, xid, payload);
    }

    OFEchoReplyMessage(long xid) {
        this(xid, new byte[0]);
    }
}
