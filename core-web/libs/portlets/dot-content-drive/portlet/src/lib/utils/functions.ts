import { SiteEntity } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';

import { BASE_QUERY, SYSTEM_HOST } from '../shared/constants';
import {
    DotContentDriveDecodeFunction,
    DotContentDriveFilters,
    DotKnownContentDriveFilters
} from '../shared/models';

/**
 * Decodes a multi-selector value.
 *
 * @param {string} value
 * @return {*}  {string[]}
 */
const multiSelector: DotContentDriveDecodeFunction = (value = ''): string[] =>
    value
        .split(',')
        .map((v) => v.trim())
        .filter((v) => v !== '');

/**
 * Decodes a single-selector value.
 *
 * @param {string} value
 * @return {*}  {string}
 */
const singleSelector: DotContentDriveDecodeFunction = (value = ''): string => value.trim();

/**
 * Decodes the value by the key. This is a dictionary of functions that will be used to decode the value by the key.
 *
 * @example
 *
 * ```typescript
 * decodeByFilterKey.baseType('1,2,3')
 * // Output: ['1', '2', '3']
 * ```
 *
 * @return {*}  {Record<keyof DotKnownContentDriveFilters, (value: string) => string | string[]>}
 */
export const decodeByFilterKey: Record<
    keyof DotKnownContentDriveFilters,
    DotContentDriveDecodeFunction
> = {
    // Should always return an array
    baseType: multiSelector,
    // Should always return an array
    contentType: multiSelector,
    title: singleSelector,
    languageId: multiSelector
};

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
 * @return {*}  {DotContentDriveFilters}
 */
export function decodeFilters(filters: string): DotContentDriveFilters {
    if (!filters) {
        return {};
    }

    const filtersArray = filters.split(';').filter((filter) => filter.trim() !== '');

    return filtersArray.reduce((acc, filter) => {
        // Get the first colon index
        const colonIndex = filter.indexOf(':');

        if (colonIndex === -1) {
            return acc;
        }

        // Handle the case where the filter has a colon in the value
        // Ex. someContentType.url:http://some.url (Looking forward for complex filters)
        const key = filter.substring(0, colonIndex).trim();
        const value = filter.substring(colonIndex + 1).trim();

        const decodeFunction = decodeByFilterKey[key];

        if (decodeFunction) {
            // Use decode function for known keys
            acc[key] = decodeFunction(value);
        } else {
            // Use default functions for unknown keys
            acc[key] = value.includes(',') ? multiSelector(value) : singleSelector(value);
        }

        return acc;
    }, {});
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
 * @param {DotContentDriveFilters} filters
 * @return {*}  {string}
 */
export function encodeFilters(filters: DotContentDriveFilters): string {
    if (!filters) {
        return '';
    }

    // Filter out empty values
    const filtersArray = Object.entries(filters).filter(([_key, value]) => value !== '');

    if (filtersArray.length === 0) {
        return '';
    }

    // Join the filters with semicolons
    return filtersArray
        .reduce((acc, filter) => {
            const [key, value] = filter;

            // Handle the multiselector (,)
            if (Array.isArray(value)) {
                acc.push(`${key}:${value.join(',')}`);
            } else {
                acc.push(`${key}:${value}`);
            }

            return acc;
        }, [])
        .join(';');
}

/**
 * Builds a search query for content drive based on the provided parameters.
 *
 * @example
 *
 * ```typescript
 * buildContentDriveQuery({
 *   path: '/some/path',
 *   currentSite: { identifier: 'site123' },
 *   filters: { contentType: 'Blog', title: 'test' }
 * })
 * // Output: A built query string for content search
 * ```
 *
 * @export
 * @param {Object} params - The query parameters
 * @param {string} [params.path] - The path to filter by
 * @param {SiteEntity} params.currentSite - The current site
 * @param {DotContentDriveFilters} [params.filters] - The filters to apply
 * @return {string} The built query string
 */
export function buildContentDriveQuery({
    path,
    currentSite,
    filters = {}
}: {
    path?: string;
    currentSite: SiteEntity;
    filters?: DotContentDriveFilters;
}): string {
    const query = new QueryBuilder();
    const baseQuery = query.raw(BASE_QUERY);
    let modifiedQuery = baseQuery;

    const filtersEntries = Object.entries(filters);

    // Add path filter if provided
    if (path) {
        modifiedQuery = modifiedQuery.field('parentPath').equals(path);
    }

    // Add site and working/variant filters
    modifiedQuery = modifiedQuery.raw(
        `+(conhost:${currentSite?.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
    );

    // Apply custom filters
    filtersEntries
        .filter(([_key, value]) => value !== undefined)
        .forEach(([key, value]) => {
            // Handle multiselectors
            if (Array.isArray(value)) {
                const orChain = value.join(' OR ');
                const orQuery = value.length > 1 ? `+${key}:(${orChain})` : `+${key}:${orChain}`;
                modifiedQuery = modifiedQuery.raw(orQuery);
                return;
            }

            // Handle raw search for title
            if (key === 'title') {
                modifiedQuery = modifiedQuery.raw(
                    `+catchall:*${value}* title_dotraw:*${value}*^5 title:'${value}'^15`
                );
                value
                    .split(' ')
                    .filter((word) => word.trim().length > 0)
                    .forEach((word) => {
                        modifiedQuery = modifiedQuery.raw(`title:${word}^5`);
                    });
                return;
            }

            modifiedQuery = modifiedQuery.field(key).equals(value);
        });

    return modifiedQuery.build();
}
