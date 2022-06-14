/**
 * Interface for Push Publish Post response.
 *
 * @interface
 */
export interface DotAjaxActionResponseView {
    _body: unknown;
    errorMessages: string[];
    total: number;
    bundleId: string;
    errors: number;
}
