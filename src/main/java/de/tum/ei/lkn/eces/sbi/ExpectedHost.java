package de.tum.ei.lkn.eces.sbi;

import java.net.InetAddress;

/**
 * Class representing a host that the system expects to be available.
 * This information is used when communicating with the host (e.g., to send LLDP packets).
 *
 * @author Amaury Van Bemten
 */
public class ExpectedHost {
    private InetAddress address;
    private String ifcAddress;

    public ExpectedHost(InetAddress address, String ifcAddress) {
        this.address = address;
        this.ifcAddress = ifcAddress;
    }

    public InetAddress getAddress() {
        return address;
    }

    public String getIfcAddress() {
        return ifcAddress;
    }
}
