package de.tum.ei.lkn.eces.sbi;

import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Component holding the available VM IDs for a given host.
 *
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = SBISystem.class)
public class VMIdList extends Component {
    private List<VMId> availableIds;

    VMIdList(int numberOfVms) {
        this.availableIds = new LinkedList<>();
        for(int i = 1; i <= numberOfVms; i++)
            this.availableIds.add(new VMId(i));
    }

    public List<VMId> getAvailableIds() {
        return Collections.unmodifiableList(this.availableIds);
    }

    public boolean removeId(VMId id) {
        return availableIds.remove(id);
    }
}
