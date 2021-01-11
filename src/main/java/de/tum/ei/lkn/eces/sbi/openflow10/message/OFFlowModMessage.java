package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.OFMatchStructure;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions.OFActionStructure;

/**
 * An OpenFlow FLOW_MOD message.
 *
 * @author Amaury Van Bemten
 */
public class OFFlowModMessage extends OFMessage {
    public final static int FLOW_MOD_ADD = 0x0000;
    public final static int FLOW_MOD_MODIFY = 0x0001;
    public final static int FLOW_MOD_MODIFY_STRICT = 0x0002;
    public final static int FLOW_MOD_DELETE = 0x0003;
    public final static int FLOW_MOD_DELETE_STRICT = 0x0004;
    public final static int FLAG_SEND_FLOW_REMOVED = 0x0001;
    public final static int FLAG_CHECK_OVERLAP = 0x0002;
    public final static int FLAG_EMERG = 0x0003;

    protected OFMatchStructure match;
    protected long cookie;
    protected int command;
    protected int idleTimeout;
    protected int hardTimeout;
    protected int priority;
    protected long bufferId;
    protected int outPort;
    protected int flags;
    protected OFActionStructure[] actions;

    OFFlowModMessage(long xid, OFMatchStructure match, long cookie, int command, int idleTimeout, int hardTimeout, int priority, long bufferId, int outPort, int flags, OFActionStructure[] actions) {
        super(OFVersion.OF10, OFMessage.OF_FLOW_MOD, xid, new byte[0]);
        int actionsLen = 0;
        for(OFActionStructure action : actions)
            actionsLen += action.getLength();
        this.length = 8 + 24 + 40 + actionsLen; // OF_HEADER + FLOW_MOD_HEADER + MATCH_SIZE + rest
        this.match = match;
        this.cookie = cookie;
        this.command = command;
        this.idleTimeout = idleTimeout;
        this.hardTimeout = hardTimeout;
        this.priority = priority;
        this.bufferId = bufferId;
        this.outPort = outPort;
        this.flags = flags;
        this.actions = actions;

        payload = new byte[this.length - 8];
        System.arraycopy(match.toBytes(), 0, payload, 0, 40);
        int byteNr = 40;
        payload[byteNr++] = (byte) (cookie >>> 56);
        payload[byteNr++] = (byte) (cookie >>> 48);
        payload[byteNr++] = (byte) (cookie >>> 40);
        payload[byteNr++] = (byte) (cookie >>> 32);
        payload[byteNr++] = (byte) (cookie >>> 24);
        payload[byteNr++] = (byte) (cookie >>> 16);
        payload[byteNr++] = (byte) (cookie >>> 8);
        payload[byteNr++] = (byte) (cookie);
        payload[byteNr++] = (byte) (command >>> 8);
        payload[byteNr++] = (byte) (command);
        payload[byteNr++] = (byte) (idleTimeout >>> 8);
        payload[byteNr++] = (byte) (idleTimeout);
        payload[byteNr++] = (byte) (hardTimeout >>> 8);
        payload[byteNr++] = (byte) (hardTimeout);
        payload[byteNr++] = (byte) (priority >>> 8);
        payload[byteNr++] = (byte) (priority);
        payload[byteNr++] = (byte) (bufferId >>> 24);
        payload[byteNr++] = (byte) (bufferId >>> 16);
        payload[byteNr++] = (byte) (bufferId >>> 8);
        payload[byteNr++] = (byte) (bufferId);
        payload[byteNr++] = (byte) (outPort >>> 8);
        payload[byteNr++] = (byte) (outPort);
        payload[byteNr++] = (byte) (flags >>> 8);
        payload[byteNr++] = (byte) (flags);
        // Fill actions
        for(OFActionStructure action : actions)
            for(byte b : action.toBytes())
                payload[byteNr++] = b;
    }
}
