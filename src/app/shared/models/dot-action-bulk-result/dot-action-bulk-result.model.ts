/**
 * Expected response when hit the endpoint: /api/v1/workflow/contentlet/actions/bulk/fire
 *
 * @interface
 */
export interface DotActionBulkResult {
    skippedCount: number;
    successCount: number;
    fails: any[];
}
