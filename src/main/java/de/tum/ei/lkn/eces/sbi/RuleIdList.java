package de.tum.ei.lkn.eces.sbi;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Component holding the available rule IDs for a given VM.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = SBISystem.class)
public class RuleIdList extends Component {
    private List<RuleId> availableIds;

    RuleIdList(int numberOfRules) {
        this.availableIds = new LinkedList<>();
        for(int i = 0; i < numberOfRules; i++)
            this.availableIds.add(new RuleId(i));
    }

    public List<RuleId> getAvailableIds() {
        return Collections.unmodifiableList(this.availableIds);
    }

    public boolean removeId(RuleId id) {
        return availableIds.remove(id);
    }
}
