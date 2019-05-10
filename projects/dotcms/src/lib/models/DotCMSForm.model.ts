export interface DotCMSFormConfig {
    contentHost?: string;
    contentType?: string;
    identifier: string;
    workflowtoSubmit?: string;
    fields?: string[];
    labels?: {
        submit?: string,
        reset?: string
    };
    win?: Window;
    onSuccess?(data: any): any;
    onError?(error: any): any;
}
