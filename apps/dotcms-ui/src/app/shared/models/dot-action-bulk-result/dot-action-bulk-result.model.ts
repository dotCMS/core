/**
 * Expected response when hit the endpoint: /api/v1/workflow/contentlet/actions/bulk/fire
 * And bulk endpoints in templates
 *
 * @interface
 */
export interface DotActionBulkResult {
    skippedCount: number;
    successCount: number;
    fails: DotBulkFailItem[];
    action?: string;
}

// optional attrs because api is not consistent
export interface DotBulkFailItem {
    errorMessage: string;
    element?: string;
    inode?: string;
    description?: string;
}
