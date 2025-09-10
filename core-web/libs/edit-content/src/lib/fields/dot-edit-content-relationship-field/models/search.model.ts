/**
 * Interface representing the parameters for a search operation.
 *
 * @export
 * @interface SearchParams
 */
export interface SearchParams {
    query?: string;
    systemSearchableFields?: Record<string, unknown>;
}
