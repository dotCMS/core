package com.dotmarketing.portlets.workflows.model;

/**
 * Encapsulates the status' for a workflow, this status are use on the showOn in order to determine if the
 * action should be render or not in some of the status.
 * @author jsanca
 */
public enum WorkflowStatus {

    LOCKED, UNLOCKED, PUBLISHED, UNPUBLISHED, ARCHIVED
}
