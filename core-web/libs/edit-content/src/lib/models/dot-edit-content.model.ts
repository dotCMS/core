import { DotCMSWorkflowAction, DotCMSWorkflowStatus } from '@dotcms/dotcms-models';

/**
 * Interface for workflow action parameters.
 *
 * @export
 * @interface DotWorkflowActionParams
 */
export interface DotWorkflowActionParams {
    actionId: string;
    inode: string;
    contentType: string;
}

/**
 * Type for the internal contentlet state.
 *
 * @export
 * @type {DotContentletState}
 */
export type DotContentletState = 'new' | 'existing' | 'reset';

export interface DotWorkflowState extends DotCMSWorkflowStatus {
    contentState: DotContentletState;
    resetAction?: DotCMSWorkflowAction;
}
