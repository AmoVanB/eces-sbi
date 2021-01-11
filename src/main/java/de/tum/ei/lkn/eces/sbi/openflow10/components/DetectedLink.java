package de.tum.ei.lkn.eces.sbi.openflow10.components;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.sbi.SBISystem;

import java.net.InetAddress;

/**
 * A directed link detected between two devices in the topology.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = SBISystem.class)
public class DetectedLink extends Component {
    private InetAddress source;
    private InetAddress destination;
    private int srcPortId;
    private int dstPortId;

    public DetectedLink(InetAddress source, InetAddress destination, int srcPortId, int dstPortId) {
        this.source = source;
        this.destination = destination;
        this.srcPortId = srcPortId;
        this.dstPortId = dstPortId;
    }

    public InetAddress getSource() {
        return source;
    }

    public InetAddress getDestination() {
        return destination;
    }

    public int getSrcPortId() {
        return srcPortId;
    }

    public int getDstPortId() {
        return dstPortId;
    }

    @Override
    public String toString() {
        return source.getCanonicalHostName() +
                ":" +
                srcPortId +
                "->" +
                destination.getCanonicalHostName() +
                ":" +
                dstPortId;
    }
}
