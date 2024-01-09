import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentlet,
    DotCMSWorkflowAction
} from '@dotcms/dotcms-models';

export interface EditContentPayload {
    layout: DotCMSContentTypeLayoutRow[];
    fields: DotCMSContentTypeField[];
    actions: DotCMSWorkflowAction[];
    contentType: string;
    contentlet?: DotCMSContentlet;
}
