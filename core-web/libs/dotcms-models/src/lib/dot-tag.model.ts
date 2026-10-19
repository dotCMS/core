export interface DotTag {
    id: string;
    label: string;
    siteId: string;
    siteName: string;
    persona: boolean;
}

/**
 * Result of `POST /api/v2/tags`. Tags that already existed are not created again;
 * they come back in `duplicates` so the UI can tell the user.
 */
export interface DotTagsCreateResult {
    created: DotTag[];
    duplicates: DotTag[];
}

/**
 * Result of `POST /api/v2/tags/import`. `duplicateCount` counts rows skipped because
 * the tag already existed; they are not part of `successCount` or `failureCount`.
 * `totalRows` is the CSV line count as the backend reports it, header line included.
 */
export interface DotTagsImportResult {
    totalRows: number;
    successCount: number;
    duplicateCount: number;
    failureCount: number;
    success: boolean;
}
