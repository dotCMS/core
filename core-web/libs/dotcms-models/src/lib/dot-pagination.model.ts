/**
 * Standard pagination interface for dotCMS API responses.
 * Used consistently across all components that need pagination data.
 *
 * @export
 * @interface DotPagination
 */
export interface DotPagination {
    /** Current page number (1-based) */
    currentPage: number;
    /** Number of items per page */
    perPage: number;
    /** Total number of entries available */
    totalEntries: number;
}
