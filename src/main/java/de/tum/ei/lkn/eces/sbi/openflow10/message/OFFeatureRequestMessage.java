package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;

/**
 * An OpenFlow FEATURE_REQUEST message.
 *
 * @author Amaury Van Bemten
 */
public class OFFeatureRequestMessage extends OFMessage {
    OFFeatureRequestMessage(long xid, byte[] payload) {
        super(OFVersion.OF10, OFMessage.OF_FEATURE_REQUEST, xid, payload);
    }

    OFFeatureRequestMessage(long xid) {
        this(xid, new byte[0]);
    }
}