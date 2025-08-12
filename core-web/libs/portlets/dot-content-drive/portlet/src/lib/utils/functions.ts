import { DotContentDriveFilters } from '../shared/models';

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
export function decodeFilters(filters: string): DotContentDriveFilters {
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
            if (value.includes(',')) {
                acc[key] = value
                    .split(',')
                    .map((v) => v.trim())
                    .filter((v) => v !== '');
            } else {
                acc[key] = value;
            }

            return acc;
        },
        {} as Record<string, string | string[]>
    );
}

/**
 * Encodes the filters into a string.
 *
 * @example
 *
 * ```typescript
 * encodeFilters({ contentType: 'Blog', language: 'en', folder: '123' })
 * // Output:
 * // 'contentType:Blog;language:en;folder:123'
 * ```
 *
 * @export
 * @param {string} filters
 * @return {*}  {Record<string, string>}
 */
export function encodeFilters(filters: DotContentDriveFilters): string {
    if (!filters) {
        return '';
    }

    const filtersArray = Object.entries(filters).filter(([_key, value]) => value !== '');

    if (filtersArray.length === 0) {
        return '';
    }

    // Join the filters with semicolons
    return filtersArray
        .reduce((acc, filter) => {
            const [key, value] = filter;

            if (Array.isArray(value)) {
                acc.push(`${key}:${value.join(',')}`);
            } else {
                acc.push(`${key}:${value}`);
            }

            return acc;
        }, [] as string[])
        .join(';');
}
