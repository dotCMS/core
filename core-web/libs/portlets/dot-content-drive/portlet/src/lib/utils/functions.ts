import { forkJoin, Observable } from 'rxjs';

import { map } from 'rxjs/operators';

import { DotFolderService } from '@dotcms/data-access';
import {
    DotContentDriveFolder,
    DotContentDriveItem,
    DotFolder,
    DotSite
} from '@dotcms/dotcms-models';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';
import { QueryBuilder } from '@dotcms/query-builder';

import { createTreeNode, generateAllParentPaths } from './tree-folder.utils';

import { BASE_QUERY, SYSTEM_HOST } from '../shared/constants';
import {
    DotContentDriveDecodeFunction,
    DotContentDriveFilters,
    DotKnownContentDriveFilters
} from '../shared/models';

/**
 * Escapes special Lucene characters in a search term.
 * Special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
 *
 * WHY WE NEED THIS:
 * Without escaping, Lucene interprets special characters as query operators:
 * - Searching "profile-test" would be interpreted as "profile" NOT "test" (dash means exclusion)
 * - Searching "A+B" would be interpreted as "A" AND "B" (plus means required term)
 * - This causes queries to fail or return wrong results
 *
 * By escaping (adding backslash before special chars), we tell Lucene to treat them as literal text.
 *
 * WHY WE HAVE DIFFERENT SEARCH STRATEGIES:
 * - Analyzed fields ('title', 'catchall'): Lucene strips special chars during indexing
 *   Example: "profile-test" is indexed as ["profile", "test"] - the dash is removed
 *   Searching for escaped "profile\-test" won't match because the dash doesn't exist in the index
 *
 * - Non-analyzed fields ('title_dotraw'): Preserve exact text including special chars
 *   Example: "profile-test" is indexed as "profile-test" - the dash is kept
 *   Searching for escaped "profile\-test" WILL match because we're looking for the literal dash
 *
 * This is why we use escaped values for title_dotraw but not for catchall/title fields.
 *
 * @see https://lucene.apache.org/core/9_0_0/core/org/apache/lucene/analysis/package-summary.html
 * @see https://www.baeldung.com/lucene-analyzers
 *
 * @param {string} term - The term to escape
 * @return {string} The escaped term
 * @example
 * escapeLuceneSpecialChars('profile-test') // returns 'profile\\-test'
 * escapeLuceneSpecialChars('A+B') // returns 'A\\+B'
 */
export function escapeLuceneSpecialChars(term: string): string {
    // Escape special Lucene characters by prefixing with backslash
    return term.replace(/([+\-&|!(){}[\]^"~*?:\\/])/g, '\\$1');
}

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

    // Filter out empty values (empty strings, null, undefined)
    const filtersArray = Object.entries(filters).filter(
        ([_key, value]) => value !== '' && value !== null && value !== undefined
    );

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
    currentSite: DotSite;
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

    if (currentSite && currentSite.identifier !== SYSTEM_HOST.identifier) {
        // Add site and working/variant filters
        modifiedQuery = modifiedQuery.raw(
            `+(conhost:${currentSite?.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
        );
    } else {
        modifiedQuery = modifiedQuery.raw(
            `+conhost:${SYSTEM_HOST.identifier} +working:true +variant:default`
        );
    }

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
                // Check if the search term contains special characters
                const hasSpecialChars = /[+\-&|!(){}[\]^"~*?:\\/]/.test(value);

                if (hasSpecialChars) {
                    // Always escape special characters for Lucene
                    const escapedValue = escapeLuceneSpecialChars(value);
                    // For searches with special chars, search both title_dotraw and catchall
                    /// We cannot use title field because it is analyzed and special characters are stripped
                    modifiedQuery = modifiedQuery.raw(
                        `+(title_dotraw:*${escapedValue}*^20 OR catchall:*${escapedValue}*^10)`
                    );
                } else {
                    // For regular searches without special chars, use the original broader approach
                    modifiedQuery = modifiedQuery.raw(
                        `+catchall:*${value}* title_dotraw:*${value}*^5 title:'${value}'^15`
                    );

                    // Split by spaces only (not dashes) and search individual words
                    value
                        .split(/\s+/)
                        .filter((word) => word.trim().length > 0)
                        .forEach((word) => {
                            modifiedQuery = modifiedQuery.raw(`title:${word}^5`);
                        });
                }

                return;
            }

            modifiedQuery = modifiedQuery.field(key).equals(value);
        });

    return modifiedQuery.build();
}

/**
 * Fetches all parent folders from a given path using parallel API calls
 *
 * Example: '/main/sub-folder/inner-folder/child-folder' will make calls to:
 * - /main/sub-folder/inner-folder/child-folder
 * - /main/sub-folder/inner-folder
 * - /main/sub-folder
 * - /main
 * - /
 *
 * @param {string} path - The full path to generate parent paths from
 * @param {DotFolderService} dotFolderService - The folder service
 * @returns {Observable<DotFolder[][]>} Observable that emits an array of folder arrays (one for each path level)
 */
export function getFolderHierarchyByPath(
    path: string,
    dotFolderService: DotFolderService
): Observable<DotFolder[][]> {
    const paths = generateAllParentPaths(path);

    // Handle empty paths case - forkJoin doesn't emit for empty array
    if (paths.length === 0) {
        return new Observable((observer) => {
            observer.next([]);
            observer.complete();
        });
    }

    const folderRequests = paths.map((path) => dotFolderService.getFolders(path));

    return forkJoin(folderRequests);
}

/**
 * Fetches folders and transforms them into tree nodes
 *
 * @param {string} path - The path to fetch folders from
 * @param {DotFolderService} dotFolderService - The folder service
 * @returns {Observable<{ parent: DotFolder; folders: DotFolderTreeNodeItem[] }>}
 */
export function getFolderNodesByPath(
    path: string,
    dotFolderService: DotFolderService
): Observable<{ parent: DotFolder; folders: DotFolderTreeNodeItem[] }> {
    return dotFolderService.getFolders(path).pipe(
        map((folders) => {
            const [parent, ...childFolders] = folders;

            return {
                parent,
                folders: childFolders.map((folder) => createTreeNode(folder))
            };
        })
    );
}

/**
 * Checks if an item is a folder.
 *
 * @param {DotContentDriveItem} item - The item to check
 * @returns {boolean} True if the item is a folder, false otherwise
 */
export function isFolder(item: DotContentDriveItem): item is DotContentDriveFolder {
    return item != null && 'type' in item && item.type === 'folder';
}
