package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.util.UtilMethods;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates the status' for a workflow, this status are use on the showOn in order to determine if the
 * action should be render or not in some of the status.
 * @author jsanca
 */
public enum WorkflowState {

    NEW, LOCKED, UNLOCKED, PUBLISHED, UNPUBLISHED, ARCHIVED, LISTING, EDITING;

    private static final String DELIMITER = ",";

    /**
     * Convert to comma separated string a set of a WorkflowState
     * @param workflowStateSet Set WorkflowState
     * @return String
     */
    public static String toCommaSeparatedString(final Set<WorkflowState> workflowStateSet) {

        return workflowStateSet.stream().map(WorkflowState::name).collect(Collectors.joining(DELIMITER));
    } // toCommaSeparatedString.

    /**
     * If the value is not null, tries to separate by commas and them join/convert into a set of a {@link WorkflowState}
     * @param value Object
     * @return Set of {@link WorkflowState}
     */
    public static  Set<WorkflowState> toSet(final Object value) {

        if (null != value) {
            return toSet(value.toString().split(DELIMITER));
        }

        return Collections.emptySet();
    } // toSet.

    /**
     * If the value is not null, tries to separate by commas and them join/convert into a set of a {@link WorkflowState}
     * @param value Object
     * @return Set of {@link WorkflowState}
     */
    public static  Set<WorkflowState> toSet(final String[] values) {

        Set<WorkflowState> workflowStateSet = Collections.emptySet();

        if (null != values && UtilMethods.isSet(values)) {

            try {
                workflowStateSet = Stream
                        .<String>of(values)
                        .map(WorkflowState::valueOf)
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                workflowStateSet = Collections.emptySet();
            }
        }

        return workflowStateSet;
    } // toSet.

}
