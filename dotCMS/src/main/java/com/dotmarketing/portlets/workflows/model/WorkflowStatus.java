package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.util.Logger;
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
public enum WorkflowStatus {

    LOCKED, UNLOCKED, PUBLISHED, UNPUBLISHED, ARCHIVED;

    private static final String DELIMITER = ",";

    /**
     * Convert to comma separated string a set of a WorkflowStatus
     * @param workflowStatusSet Set WorkflowStatus
     * @return String
     */
    public static String toCommaSeparatedString(final Set<WorkflowStatus> workflowStatusSet) {

        return workflowStatusSet.stream().map(WorkflowStatus::name).collect(Collectors.joining(DELIMITER));
    } // toCommaSeparatedString.

    /**
     * If the value is not null, tries to separate by commas and them join/convert into a set of a {@link WorkflowStatus}
     * @param value Object
     * @return Set of {@link WorkflowStatus}
     */
    public static  Set<WorkflowStatus> toSet(final Object value) {

        Set<WorkflowStatus> workflowStatusSet = Collections.emptySet();

        if (null != value && UtilMethods.isSet(value.toString())) {

            try {
                workflowStatusSet = Stream
                        .<String>of(value.toString().split(DELIMITER))
                        .map(WorkflowStatus::valueOf)
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                Logger.error(WorkflowStatus.class, "No show On data on workflow action record, bad data?");
                workflowStatusSet = Collections.emptySet();
            }
        }

        return workflowStatusSet;
    } // toSet.

}
