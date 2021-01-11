package de.tum.ei.lkn.eces.sbi.openflow10.message.structure;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The OpenFlow match structure.
 *
 * @author Amaury Van Bemten
 */
public class OFMatchStructure extends OFStructure {
    private long wildcards;
    private int inPort;
    private byte[] dlSrc;
    private byte[] dlDst;
    private int dlVlan;
    private short dlPcp;
    private int dlType;
    private short nwTos;
    private short nwProto;
    private InetAddress nwSrc;
    private InetAddress nwDst;
    private int tpSrc;
    private int tpDst;

    public static OFMatchStructure getAllMatch() {
        // Wildcard all but DLVAN and DL_TYPE
        try {
            return new OFMatchStructure(0x003fffff,
                    0,
                    new byte[6],
                    new byte[6],
                    0,
                    (short) 0,
                    0x8100,
                    (short) 0,
                    (short) 0,
                    InetAddress.getByAddress(new byte[4]),
                    InetAddress.getByAddress(new byte[4]),
                    0,
                    0);
        } catch (UnknownHostException e) {
            // for sure never happens
            return null;
        }
    }

    public static OFMatchStructure getVLANMatch(int dlVlan) {
        // Wildcard all but DLVAN and DL_TYPE
        try {
//            return new OFMatchStructure(0x003fffff ^ 0x00000002 ^ 0x00000010,
            return new OFMatchStructure(0x003ffffd,
                    0,
                    new byte[6],
                    new byte[6],
                    dlVlan,
                    (short) 0,
                    //0x8100,
                    0,
                    (short) 0,
                    (short) 0,
                    InetAddress.getByAddress(new byte[4]),
                    InetAddress.getByAddress(new byte[4]),
                    0,
                    0);
        } catch (UnknownHostException e) {
            // for sure never happens
            return null;
        }
    }

    public OFMatchStructure(long wildcards, int inPort, byte[] dlSrc, byte[] dlDst, int dlVlan, short dlPcp, int dlType, short nwTos, short nwProto, InetAddress nwSrc, InetAddress nwDst, int tpSrc, int tpDst) {
        this.wildcards = wildcards;
        this.inPort = inPort;
        this.dlSrc = dlSrc;
        this.dlDst = dlDst;
        this.dlVlan = dlVlan;
        this.dlPcp = dlPcp;
        this.dlType = dlType;
        this.nwTos = nwTos;
        this.nwProto = nwProto;
        this.nwSrc = nwSrc;
        this.nwDst = nwDst;
        this.tpSrc = tpSrc;
        this.tpDst = tpDst;

        this.bytes = new byte[40];
        bytes[0] = (byte) (wildcards >>> 24);
        bytes[1] = (byte) (wildcards >>> 16);
        bytes[2] = (byte) (wildcards >>> 8);
        bytes[3] = (byte) (wildcards);
        bytes[4] = (byte) (inPort >>> 8);
        bytes[5] = (byte) (inPort);
        System.arraycopy(dlSrc, 0, bytes, 6, 6);
        System.arraycopy(dlDst, 0, bytes, 12, 6);
        bytes[18] = (byte) (dlVlan >>> 8);
        bytes[19] = (byte) (dlVlan);
        bytes[20] = (byte) (dlPcp);
        // 21 is padding
        bytes[22] = (byte) (dlType >>> 8);
        bytes[23] = (byte) (dlType);
        bytes[24] = (byte) (nwTos);
        bytes[25] = (byte) (nwProto);
        // 26 27 is padding
        System.arraycopy(nwSrc.getAddress(), 0, bytes, 28, 4);
        System.arraycopy(nwDst.getAddress(), 0, bytes, 32, 4);
        bytes[36] = (byte) (tpSrc >>> 8);
        bytes[37] = (byte) (tpSrc);
        bytes[38] = (byte) (tpDst >>> 8);
        bytes[39] = (byte) (tpDst);
    }
}
