package com.dotmarketing.portlets.workflows.model;

/**
 * Says which is the state of a history change, usually it is NONE (non-state)
 */
public enum WorkflowHistoryState {

    NONE,  // non-state
    RESET  // means the row has been reset.
}
