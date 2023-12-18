import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentlet,
    DotCMSWorkflowAction
} from '@dotcms/dotcms-models';

export interface EditContentPayload {
    layout: DotCMSContentTypeLayoutRow[];
    fields: DotCMSContentTypeField[];
    contentlet?: DotCMSContentlet;
    actions?: DotCMSWorkflowAction[];
    contentType: string;
}
