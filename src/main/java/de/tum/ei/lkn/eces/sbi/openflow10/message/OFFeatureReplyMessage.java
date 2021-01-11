package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;
import de.tum.ei.lkn.eces.sbi.openflow10.exception.IncorrectOFFormatException;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.OFPortStructure;
import de.tum.ei.lkn.eces.sbi.openflow10.util.BytesUtils;

import java.util.Arrays;

/**
 * An OpenFlow FEATURE_REPLY message.
 *
 * @author Amaury Van Bemten
 */
public class OFFeatureReplyMessage extends OFMessage {
    protected long datapathId;
    protected int nBuffers;
    protected byte nTables;
    protected int capabilities;
    protected int actions;
    protected OFPortStructure[] ports;

    OFFeatureReplyMessage(long xid, byte[] payload) throws IncorrectOFFormatException {
        super(OFVersion.OF10, OFMessage.OF_FEATURE_REPLY, xid, payload);

        datapathId = BytesUtils.bytesToLong(payload, 0, 8);
        nBuffers = (int) BytesUtils.bytesToLong(payload, 8, 12);
        nTables = payload[12];
        capabilities = (int) BytesUtils.bytesToLong(payload, 16, 20);
        actions = (int) BytesUtils.bytesToLong(payload, 20, 24);

        // Getting number of ports from length
        int portsLength = length - 32;
        if(portsLength % 48 != 0)
            throw new IncorrectOFFormatException();

        ports = new OFPortStructure[portsLength / 48];

        for(int startIndex = 24; startIndex < payload.length; startIndex += 48)
            ports[(startIndex - 24) / 48] = new OFPortStructure(Arrays.copyOfRange(payload, startIndex, startIndex + 48));
    }

    public OFPortStructure[] getPorts() {
        return ports;
    }
}
