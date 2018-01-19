/**
 * Interface for Push Publish Post response.
 *
 * @interface
 */
export interface AjaxActionResponseView {
    errorMessages: string[];
    total: number;
    bundleId: string;
    errors: number;
}
