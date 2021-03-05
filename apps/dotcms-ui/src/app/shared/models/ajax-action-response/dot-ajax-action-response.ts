/**
 * Interface for Push Publish Post response.
 *
 * @interface
 */
export interface DotAjaxActionResponseView {
    _body: any;
    errorMessages: string[];
    total: number;
    bundleId: string;
    errors: number;
}
