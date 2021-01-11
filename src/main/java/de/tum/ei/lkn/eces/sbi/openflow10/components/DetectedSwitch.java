package de.tum.ei.lkn.eces.sbi.openflow10.components;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.sbi.SBISystem;
import io.netty.channel.socket.SocketChannel;

import java.net.InetAddress;

/**
 * A switch in the topology.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = SBISystem.class)
public class DetectedSwitch extends Component {
    protected String name;
    protected SocketChannel connection;

    public DetectedSwitch(SocketChannel connection) {
        this(connection, connection.remoteAddress().getAddress().getCanonicalHostName());
    }

    public DetectedSwitch(SocketChannel connection, String name) {
        this.connection = connection;
        this.name = name;
    }

    public SocketChannel getChannel() {
        return connection;
    }

    public InetAddress getAddress() {
        return getChannel().remoteAddress().getAddress();
    }

    @Override
    public String toString() {
        return name;
    }
}
