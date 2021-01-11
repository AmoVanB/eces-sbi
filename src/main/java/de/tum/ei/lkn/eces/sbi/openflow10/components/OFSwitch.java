package de.tum.ei.lkn.eces.sbi.openflow10.components;

import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.OFPortStructure;
import io.netty.channel.socket.SocketChannel;

/**
 * An OpenFlow switch with its port structures.
 *
 * @author Amaury Van Bemten
 */
public class OFSwitch extends DetectedSwitch {
    private OFPortStructure[] ports;

    public OFSwitch(SocketChannel connection, OFPortStructure[] ports) {
        this(connection, connection.remoteAddress().getAddress().getCanonicalHostName(), ports);
    }

    public OFSwitch(SocketChannel connection, String name, OFPortStructure[] ports) {
        super(connection, name);
        this.ports = ports;
    }

    public OFPortStructure[] getPorts() {
        return ports;
    }
}
