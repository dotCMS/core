import { DotCMSWorkflowAction, DotCMSWorkflowStatus } from '@dotcms/dotcms-models';

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
}

/**
 * Interface representing an activity in the content sidebar
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
