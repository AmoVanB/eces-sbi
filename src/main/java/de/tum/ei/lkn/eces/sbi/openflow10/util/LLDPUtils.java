package de.tum.ei.lkn.eces.sbi.openflow10.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Some helpers for LLDP packets.
 *
 * @author Amaury Van Bemten
 */
public class LLDPUtils {
    public static byte[] generateTopologyDiscoveryPacket(InetAddress srcIP, byte[] srcMac, int srcPort) {
        byte[] ipAddress = srcIP.getAddress();
        byte[] data = new byte[17 + ipAddress.length + 13];
        // Src mac: LLDP multicast
        data[0] = (byte) 0x01;
        data[1] = (byte) 0x80;
        data[2] = (byte) 0xc2;
        data[3] = (byte) 0x00;
        data[4] = (byte) 0x00;
        data[5] = (byte) 0x0e;
        // Dst mac
        System.arraycopy(srcMac, 0, data, 6, 6);
        // Protocol type: LLDP
        data[12] = (byte) 0x88;
        data[13] = (byte) 0xcc;
        // First TLV: chassis ID (IP address)
        data[14] = (byte) 0x02; // TLV type: chassis id (1), TLV length: IP address + 1 for subtype
        data[15] = (byte) (ipAddress.length + 1);
        data[16] = (byte) 0x05; // Subtype = network address (5)
        // SRC MAC
        System.arraycopy(ipAddress, 0, data, 17, ipAddress.length);
        // Second TLV: port subtype
        data[17 + ipAddress.length + 0] = (byte) 0x04; // TLV type: port id (2), TLV length: 20
        data[17 + ipAddress.length + 1] = (byte) 0x03;
        data[17 + ipAddress.length + 2] = (byte) 0x07; // subtype: locally assigned
        data[17 + ipAddress.length + 3] = (byte) (srcPort >>> 8);
        data[17 + ipAddress.length + 4] = (byte) (srcPort);
        // Third TLV: TTL
        data[17 + ipAddress.length + 5] = (byte) 0x06; // TLV type: TTL (3), TLV length: 2
        data[17 + ipAddress.length + 6] = (byte) 0x02;
        data[17 + ipAddress.length + 7] = (byte) 0x00; // TTL =
        data[17 + ipAddress.length + 8] = (byte) 0x78; // 120s
        // TTL end of LLDPDU
        // 4 bytes to 0
        data[17 + ipAddress.length + 9] = (byte) 0x00;
        data[17 + ipAddress.length + 10] = (byte) 0x00;
        data[17 + ipAddress.length + 11] = (byte) 0x00;
        data[17 + ipAddress.length + 12] = (byte) 0x00;

        return data;
    }

    public static boolean isLLDPPacket(byte[] packet) {
        // We assume no VLAN
        return packet[12] == (byte) 0x88 && packet[13] == (byte) 0xcc;
    }

    public static InetAddress getIP(byte[] lldpPacket) {
        if(!isLLDPPacket(lldpPacket))
            return null;

        // First TLV starts in 14
        int tlvStart = 14;
        while((lldpPacket[tlvStart] >>> 1) != 0x01) {
            int tlvLength = ((lldpPacket[tlvStart] & 0x01) << 8) + lldpPacket[tlvStart + 1];
            tlvStart += 2 + tlvLength; // 2 for T & L of TLV

            if(tlvStart > lldpPacket.length)
                return null;
        }

        // We reached the TLV
        int tlvLength = ((lldpPacket[tlvStart] & 0x01) << 8) + lldpPacket[tlvStart + 1];
        byte[] ipAddress = new byte[tlvLength - 1];
        System.arraycopy(lldpPacket, tlvStart + 3, ipAddress, 0, ipAddress.length);
        try {
            return InetAddress.getByAddress(ipAddress);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static int getPortNumber(byte[] lldpPacket) {
        if(!isLLDPPacket(lldpPacket))
            return -1;

        // First TLV starts in 14
        int tlvStart = 14;
        while((lldpPacket[tlvStart] >>> 1) != 0x02) {
            int tlvLength = ((lldpPacket[tlvStart] & 0x01) << 8) + lldpPacket[tlvStart + 1];
            tlvStart += 2 + tlvLength; // 2 for T & L of TLV

            if(tlvStart > lldpPacket.length)
                return -1;
        }

        // We reached the TLV
        int tlvLength = ((lldpPacket[tlvStart] & 0x01) << 8) + lldpPacket[tlvStart + 1];
        if(tlvLength != 3)
            return -1;
        return (int) BytesUtils.bytesToLong(lldpPacket, tlvStart + 3, tlvStart + 5);
    }
}
