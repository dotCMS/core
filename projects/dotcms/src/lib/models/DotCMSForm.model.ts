export interface DotCMSFormConfig {
    contentType?: string;
    identifier: string;
    workflowtoSubmit?: string;
    fields?: string[];
    onSuccess?: (data) => {};
    onError?: (err) => {};
}
