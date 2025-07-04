/**
 * Decodes the filters string into a record of key-value pairs.
 *
 * @example
 *
 * ```typescript
 * decodeFilters('contentType:Blog;language:en;folder:123')
 * // Output:
 * // { contentType: 'Blog', language: 'en', folder: '123' }
 * ```
 *
 * @export
 * @param {string} filters
 * @return {*}  {Record<string, string>}
 */
export function decodeFilters(filters: string): Record<string, string> {
    if (!filters) {
        return {};
    }

    const filtersArray = filters.split(';').filter((filter) => filter.trim() !== '');

    return filtersArray.reduce(
        (acc, filter) => {
            // Get the first colon index
            const colonIndex = filter.indexOf(':');

            if (colonIndex === -1) {
                return acc;
            }

            // Handle the case where the filter has a colon in the value
            // Ex. someContentType.url:http://some.url (Looking forward for complex filters)
            const key = filter.substring(0, colonIndex).trim();
            const value = filter.substring(colonIndex + 1).trim();

            // We have to handle the multiselector (,) but this is enough to pave the path for now
            acc[key] = value;

            return acc;
        },
        {} as Record<string, string>
    );
}
