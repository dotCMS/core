/**
 * Expected object to send when hit the endpoint: /api/v1/workflow/contentlet/actions/bulk/fire
 *
 * @interface
 */
export interface DotActionBulkRequestOptions {
    workflowActionId: string;
    contentletIds?: string[];
    query?: string;
    additionalParams: {
        assignComment: {
            comment: string;
            assign: string;
        };
        pushPublish: {
            whereToSend: string;
            iWantTo: string;
            expireDate: string;
            expireTime: string;
            publishDate: string;
            publishTime: string;
            filterKey: string;
        };
    };
}
