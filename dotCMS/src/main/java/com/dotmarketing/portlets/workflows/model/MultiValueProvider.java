package com.dotmarketing.portlets.workflows.model;

import java.util.Collection;

/**
 * This interface basically provides a collection of values for a {@link MultiSelectionWorkflowActionletParameter}
 * @author jsanca
 */
@FunctionalInterface
public interface MultiValueProvider {

    /**
     * Returns the collection of values
     * @return T values
     */
    Collection<MultiKeyValue> values ();
}
