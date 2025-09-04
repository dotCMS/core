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
 * Enum for timeline item action types in the history sidebar.
 * Defines the available actions that can be performed on a timeline item.
 *
 * @export
 * @enum {string}
 */
export enum DotHistoryTimelineItemActionType {
    PREVIEW = 'preview',
    RESTORE = 'restore',
    COMPARE = 'compare',
    DELETE = 'delete'
}

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
 * Interface for pagination data in history components.
 * Used to control pagination state and navigation.
 *
 * @export
 * @interface DotHistoryPagination
 */
export interface DotHistoryPagination {
    /** Current page number (1-based) */
    currentPage: number;
    /** Number of items per page */
    perPage: number;
    /** Total number of entries available */
    totalEntries: number;
}
