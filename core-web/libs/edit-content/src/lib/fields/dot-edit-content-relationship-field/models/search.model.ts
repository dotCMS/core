export interface SystemSearchableFields {
    languageId?: number;
    siteId?: string;
    folderId?: string;
    [key: string]: unknown;
}

/**
 * Interface representing the parameters for a search operation.
 *
 * @export
 * @interface SearchParams
 */
export interface SearchParams {
    query?: string;
    systemSearchableFields?: SystemSearchableFields;
}
