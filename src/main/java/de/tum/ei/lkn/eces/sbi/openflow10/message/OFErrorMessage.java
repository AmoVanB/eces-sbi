package de.tum.ei.lkn.eces.sbi.openflow10.message;

import de.tum.ei.lkn.eces.sbi.openflow10.OFVersion;
import de.tum.ei.lkn.eces.sbi.openflow10.exception.IncorrectOFFormatException;
import de.tum.ei.lkn.eces.sbi.openflow10.util.BytesUtils;

/**
 * An OpenFlow ERROR message.
 *
 * @author Amaury Van Bemten
 */
public class OFErrorMessage extends OFMessage {
    private int errorType;
    private int errorCode;
    private byte[] data;

    OFErrorMessage(long xid, byte[] payload) throws IncorrectOFFormatException {
        super(OFVersion.OF10, OFMessage.OF_ERROR, xid, payload);
        if(payload.length < 4)
            throw new IncorrectOFFormatException("OF_ERROR should be at least 4 bytes long");

        errorType = (int) BytesUtils.bytesToLong(payload, 0, 2);
        errorCode = (int) BytesUtils.bytesToLong(payload, 2, 4);

        data = new byte[payload.length - 4];
        System.arraycopy(payload, 4, data, 0, payload.length - 4);
    }

    public int getErrorType() {
        return errorType;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorTypeString() {
        switch(errorType) {
            case 0x0000:
                return "hello failed";
            case 0x0001:
                return "bad request";
            case 0x0002:
                return "bad action";
            case 0x0003:
                return "flow mod failed";
            case 0x0004:
                return "port mod failed";
            case 0x0005:
                return "queue op failed";
            default:
                return "unknown error type";
        }
    }

    public String getErrorCodeString() {
        switch(errorType) {
            case 0x0000:
                switch(errorCode) {
                    case 0x0000:
                        return "incompatible";
                    case 0x0001:
                        return "eperm";
                    default:
                        return "unknown error code";
                }
            case 0x0001:
                switch(errorCode) {
                    case 0x0000:
                        return "bad version";
                    case 0x0001:
                        return "bad type";
                    case 0x0002:
                        return "bad stat";
                    case 0x0003:
                        return "bad vendor";
                    case 0x0004:
                        return "bad subtype";
                    case 0x0005:
                        return "eperm";
                    case 0x0006:
                        return "bad length";
                    case 0x0007:
                        return "buffer empty";
                    case 0x0008:
                        return "buffer unknown";
                    default:
                        return "unknown error code";
                }
            case 0x0002:
                switch(errorCode) {
                    case 0x0000:
                        return "bad type";
                    case 0x0001:
                        return "bad length";
                    case 0x0002:
                        return "bad vendor";
                    case 0x0003:
                        return "bad vendor type";
                    case 0x0004:
                        return "bad out port";
                    case 0x0005:
                        return "bad argument";
                    case 0x0006:
                        return "eperm";
                    case 0x0007:
                        return "too many";
                    case 0x0008:
                        return "bad queue";
                    default:
                        return "unknown error code";
                }
            case 0x0003:
                switch(errorCode) {
                    case 0x0000:
                        return "all tables full";
                    case 0x0002:
                        return "overlap";
                    case 0x0003:
                        return "eperm";
                    case 0x0004:
                        return "bad emerg timeout";
                    case 0x0005:
                        return "bad command";
                    case 0x0006:
                        return "unsupported";
                    default:
                        return "unknown error code";
                }
            case 0x0004:
                switch(errorCode) {
                    case 0x0000:
                        return "bad port";
                    case 0x0001:
                        return "bad hw address";
                    default:
                        return "unknown error code";
                }
            case 0x0005:
                switch(errorCode) {
                    case 0x0000:
                        return "bad port";
                    case 0x0001:
                        return "bad queue";
                    case 0x0002:
                        return "eperm";
                    default:
                        return "unknown error code";
                }            default:
                return "unknown error type";
        }
    }

    public byte[] getData() {
        return data;
    }
}
