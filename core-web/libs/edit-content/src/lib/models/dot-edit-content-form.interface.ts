import { DotCMSContentlet, DotCMSContentType, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

export interface EditContentPayload {
    contentType: DotCMSContentType;
    actions: DotCMSWorkflowAction[];
    contentlet?: DotCMSContentlet;
    loading: boolean;
    layout: {
        showSidebar: boolean;
    };
}

export interface EditContentForm {
    contentType: DotCMSContentType;
    contentlet?: DotCMSContentlet;
}
