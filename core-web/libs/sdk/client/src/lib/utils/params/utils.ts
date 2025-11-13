/**
 * Utility functions for AI search parameter mapping and processing
 */

/**
 * Appends mapped parameters to URLSearchParams based on a mapping configuration.
 * Only includes parameters that have defined values.
 *
 * @param searchParams - The URLSearchParams object to append to
 * @param sourceObject - The source object containing values
 * @param mapping - Array of [targetKey, sourceKey] pairs for parameter mapping, or [key] to use same key
 * @example
 * ```typescript
 * const params = new URLSearchParams();
 * const query = { limit: 10, offset: 0, siteId: 'default', indexName: 'content' };
 * const mapping = [
 *   ['searchLimit', 'limit'],    // Maps limit -> searchLimit
 *   ['searchOffset', 'offset'],  // Maps offset -> searchOffset
 *   ['site', 'siteId'],          // Maps siteId -> site
 *   ['indexName']                // Uses indexName -> indexName (same key)
 * ];
 * appendMappedParams(params, query, mapping);
 * // Results in: searchLimit=10&searchOffset=0&site=default&indexName=content
 * ```
 */
export function appendMappedParams<T extends object>(
    searchParams: URLSearchParams,
    sourceObject: T,
    mapping: Array<Array<string>>
): void {
    mapping.forEach((item) => {
        const targetKey: string = item[0];
        const sourceKey: string = item[1] ?? item[0];

        const value = sourceObject[sourceKey as keyof T];
        if (value !== undefined) {
            searchParams.append(targetKey, String(value));
        }
    });
}
