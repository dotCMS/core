import { DotCMSContentlet, DotCMSWorkflowAction, DotCMSContentType } from '@dotcms/dotcms-models';

export interface EditContentPayload {
    contentType: DotCMSContentType;
    actions: DotCMSWorkflowAction[];
    contentlet?: DotCMSContentlet;
    loading: boolean;
    layout: {
        showSidebar: boolean;
    };
}
