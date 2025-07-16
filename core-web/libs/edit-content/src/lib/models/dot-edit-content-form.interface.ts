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
}

/**
 * Represents the form structure for editing content.
 * @interface EditContentForm
 */
export interface ContentTypeAndContentlet {
    contentType: DotCMSContentType;
    contentlet?: DotCMSContentlet;
}

/**
 * Represents a tab in the edit content form.
 * @interface Tab
 */
export type Tab = {
    title: string;
    layout: {
        divider: DotCMSContentTypeField;
        columns: {
            columnDivider: DotCMSContentTypeField;
            fields: DotCMSContentTypeField[];
        }[];
    }[];
};

export interface DotFormData {
    contentType: DotCMSContentType;
    contentlet: DotCMSContentlet | null;
    tabs: Tab[];
}

/**
 * Represents the form field value.
 * @type FormFieldValue
 */
type FormFieldValue = string | string[] | Date | number;

/**
 * Represents the form values.
 * @interface FormValues
 */
export type FormValues = Record<string, FormFieldValue>;
