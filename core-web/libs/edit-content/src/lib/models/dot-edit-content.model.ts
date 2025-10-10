import {
    DotCMSWorkflowAction,
    DotCMSWorkflowStatus,
    DotCMSContentletVersion
} from '@dotcms/dotcms-models';

/**
 * Interface for workflow action parameters.
 *
 * @export
 * @interface DotWorkflowActionParams
 */
export interface DotWorkflowActionParams {
    workflow: DotCMSWorkflowAction;
    inode: string;
    contentType: string;
    languageId: string;
    identifier: string;
}

/**
 * Type for the internal contentlet state.
 *
 * @export
 * @type {DotContentletState}
 */
export type DotContentletState = 'new' | 'existing' | 'reset' | 'copy';

/**
 * Type for the view state of the activity sidebar.
 *
 * @export
 * @type {DotActivityViewState}
 */
export type DotActivityViewState = 'idle' | 'create';

export interface DotWorkflowState extends DotCMSWorkflowStatus {
    contentState: DotContentletState;
    resetAction?: DotCMSWorkflowAction;
}

/**
 * UIState interface
 *
 * @export
 * @interface UIState
 */
export interface UIState {
    activeTab: number;
    isSidebarOpen: boolean;
    activeSidebarTab: number;
    isBetaMessageVisible: boolean;
}

/**
 * Interface representing an activity in the content sidebar
 *
 * @export
 * @interface Activity
 */
export interface Activity {
    commentDescription: string;
    createdDate: number;
    email: string;
    postedBy: string;
    roleId: string;
    taskId: string;
    type: string;
}

/**
 * Constants for timeline item action types in the history sidebar.
 * Defines the available actions that can be performed on a timeline item.
 *
 * @export
 * @const
 */
export const DotHistoryTimelineItemActionType = {
    PREVIEW: 'preview',
    RESTORE: 'restore',
    COMPARE: 'compare',
    DELETE: 'delete',
    VIEW: 'view'
} as const;

/**
 * Type for timeline item action types in the history sidebar.
 * Derived from the constants object for type safety.
 *
 * @export
 * @type
 */
export type DotHistoryTimelineItemActionType =
    (typeof DotHistoryTimelineItemActionType)[keyof typeof DotHistoryTimelineItemActionType];

/**
 * Interface for timeline item actions in the history sidebar.
 * Represents an action triggered on a specific timeline item.
 *
 * @export
 * @interface DotHistoryTimelineItemAction
 */
export interface DotHistoryTimelineItemAction {
    /** The type of action being performed */
    type: DotHistoryTimelineItemActionType;
    /** The content version item the action is performed on */
    item: DotCMSContentletVersion;
}

/**
 * Interface for push publish timeline items.
 * Represents a push publish operation with bundle and environment information.
 *
 * @export
 * @interface DotPushPublishHistoryItem
 */
export interface DotPushPublishHistoryItem {
    /** Unique identifier for the push publish bundle */
    bundleId: string;
    /** Target environment for the push publish operation */
    environment: string;
    /** Timestamp when the content was pushed (in milliseconds) */
    pushDate: number;
    /** User who performed the push publish operation */
    pushedBy: string;
}
