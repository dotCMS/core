import { DotCMSContentType } from './dot-content-types.model';
import { DotCMSWorkflow, WorkflowStep } from './dot-workflow.model';

export interface DotCMSWorkflowActionEvent {
    workflow: DotCMSWorkflowAction;
    callback: string;
    inode: string;
    selectedInodes?: string | string[];
}

export interface DotCMSWorkflowAction {
    assignable: boolean;
    commentable: boolean;
    condition: string;
    icon: string;
    id: string;
    name: string;
    nextAssign: string;
    nextStep: string;
    nextStepCurrentStep: boolean;
    order: number;
    owner?: string;
    roleHierarchyForAssign: boolean;
    schemeId: string;
    showOn: string[];
    actionInputs: DotCMSWorkflowInput[];
    metadata?: Record<string, string>;
    hasArchiveActionlet?: boolean;
    hasCommentActionlet?: boolean;
    hasDeleteActionlet?: boolean;
    hasDestroyActionlet?: boolean;
    hasMoveActionletActionlet?: boolean;
    hasMoveActionletHasPathActionlet?: boolean;
    hasOnlyBatchActionlet?: boolean;
    hasPublishActionlet?: boolean;
    hasPushPublishActionlet?: boolean;
    hasResetActionlet?: boolean;
    hasSaveActionlet?: boolean;
    hasUnarchiveActionlet?: boolean;
    hasUnpublishActionlet?: boolean;
}

export enum DotCMSSystemActionType {
    UNPUBLISH = 'UNPUBLISH',
    UNARCHIVE = 'UNARCHIVE',
    PUBLISH = 'PUBLISH',
    NEW = 'NEW',
    EDIT = 'EDIT',
    DESTROY = 'DESTROY',
    DELETE = 'DELETE',
    ARCHIVE = 'ARCHIVE'
}

export enum DotCMSActionSubtype {
    SEPARATOR = 'SEPARATOR'
}

export interface DotCMSSystemActionMappings {
    [key: string]: DotCMSSystemAction | string;
}

export interface DotCMSSystemAction {
    identifier: string;
    systemAction: string;
    workflowAction: DotCMSWorkflowAction;
    owner: DotCMSContentType;
    ownerContentType: boolean;
    ownerScheme: boolean;
}

export interface DotCMSWorkflowInput {
    id: string;
    body: any;
}

export interface DotCMSContentletWorkflowActions {
    scheme: DotCMSWorkflow;
    action: DotCMSWorkflowAction;
    firstStep: WorkflowStep;
}
