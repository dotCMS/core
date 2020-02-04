package com.dotmarketing.portlets.workflows.model;

/**
 * Represent the type of the history, it could be just a comment or approval
 *
 * Approval means a comment history which is actually an approval
 * Comment is basically anything else
 *
 * @author jsanca
 */
public enum WorkflowHistoryType {

    COMMENT,
    APPROVAL
}
