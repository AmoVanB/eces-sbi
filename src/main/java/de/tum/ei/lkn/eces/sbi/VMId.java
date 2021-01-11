package de.tum.ei.lkn.eces.sbi;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;

/**
 * Component holding the ID of a VM on the host (vagrant ID).
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = SBISystem.class)
public class VMId extends Component {
    private int id;

    VMId(int id) {
        this.id = id;
    }

    public int getIntegerId() {
        return id;
    }
}
