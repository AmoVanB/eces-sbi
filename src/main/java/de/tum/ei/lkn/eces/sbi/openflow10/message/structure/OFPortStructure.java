package de.tum.ei.lkn.eces.sbi.openflow10.message.structure;

import de.tum.ei.lkn.eces.sbi.openflow10.exception.IncorrectOFFormatException;
import de.tum.ei.lkn.eces.sbi.openflow10.util.BytesUtils;

import java.util.Arrays;

/**
 * The OpenFlow port structure.
 *
 * @author Amaury Van Bemten
 */
public class OFPortStructure extends OFStructure {
    private int portId;
    private byte[] macAddress;
    private String name;
    private int config;
    private int state;
    private int curr;
    private int advertised;
    private int supported;
    private int peer;

    public OFPortStructure(byte[] data) throws IncorrectOFFormatException {
        if(data.length != 48)
            throw new IncorrectOFFormatException();

        portId = (int) BytesUtils.bytesToLong(data, 0, 2);
        macAddress = Arrays.copyOfRange(data, 2, 8);
        name = new String(Arrays.copyOfRange(data, 8, 24));
        config = (int) BytesUtils.bytesToLong(data, 24, 28);
        state = (int) BytesUtils.bytesToLong(data, 28, 32);
        curr = (int) BytesUtils.bytesToLong(data, 32, 36);
        advertised = (int) BytesUtils.bytesToLong(data, 36, 40);
        supported = (int) BytesUtils.bytesToLong(data, 40, 44);
        peer = (int) BytesUtils.bytesToLong(data, 44, 48);
        this.bytes = data;
    }


    public int getId() {
        return portId;
    }

    public String getName() {
        return name;
    }

    public byte[] getMacAddress() {
        return macAddress;
    }
}
