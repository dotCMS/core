import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSWorkflowAction
} from '@dotcms/dotcms-models';

/**
 * Represents the payload for editing content.
 * @interface EditContentPayload
 */
export interface EditContentPayload {
    contentType: DotCMSContentType;
    actions: DotCMSWorkflowAction[];
    contentlet?: DotCMSContentlet;
    loading: boolean;
    layout: {
        showSidebar: boolean;
    };
}

/**
 * Represents the form structure for editing content.
 * @interface EditContentForm
 */
export interface EditContentForm {
    contentType: DotCMSContentType;
    contentlet?: DotCMSContentlet;
    tabs?: Tab[];
}

/**
 * Represents a tab in the edit content form.
 * @interface Tab
 */
type Tab = {
    title: string;
    layout: {
        divider: DotCMSContentTypeField;
        columns: {
            columnDivider: DotCMSContentTypeField;
            fields: DotCMSContentTypeField[];
        }[];
    }[];
};
