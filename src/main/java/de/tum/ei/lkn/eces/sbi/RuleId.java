package de.tum.ei.lkn.eces.sbi;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;

/**
 * Component holding the ID of a rule for a VM.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = SBISystem.class)
public class RuleId extends Component {
    private int id;

    RuleId(int id) {
        this.id = id;
    }

    public int getIntegerId() {
        return id;
    }
}
