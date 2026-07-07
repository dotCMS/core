import { forkJoin, Observable } from 'rxjs';

import { map } from 'rxjs/operators';

import { DotFolderService } from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotContentDriveDateRange,
    DotContentDriveFolder,
    DotContentDriveItem,
    DotContentDriveUserSearchableValue,
    DotFolder
} from '@dotcms/dotcms-models';
import { getSingleSelectableFieldOptions } from '@dotcms/edit-content';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { createTreeNode, generateAllParentPaths } from './tree-folder.utils';

import {
    FIELD_FILTER_CHECKBOX_TYPE,
    FIELD_FILTER_DATE_TYPES,
    FIELD_FILTER_MULTI_VALUE_TYPES,
    USER_SEARCHABLE_PREFIX,
    USER_SEARCHABLE_VALUE_SEPARATOR
} from '../shared/constants';
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

/** A single workflow filter entry: one scheme, optionally pinned to a step. */
export interface WorkflowFilterEntry {
    scheme: string;
    step?: string;
}

/** Separator for the `schemeId[:stepId]` workflow token encoding. */
export const WORKFLOW_TOKEN_SEPARATOR = ':';

/**
 * Canonical parse for one `workflow` token. Splits on the FIRST separator only,
 * so any separator inside the step id is preserved.
 * `'A:X'` → `{ scheme: 'A', step: 'X' }`; `'B'` → `{ scheme: 'B' }`.
 *
 * @param {string} token
 * @return {*}  {WorkflowFilterEntry}
 */
export function parseWorkflowToken(token: string): WorkflowFilterEntry {
    const index = token.indexOf(WORKFLOW_TOKEN_SEPARATOR);
    return index === -1
        ? { scheme: token }
        : { scheme: token.slice(0, index), step: token.slice(index + 1) };
}

/**
 * Canonical serialize, inverse of {@link parseWorkflowToken}.
 * `{ scheme: 'A', step: 'X' }` → `'A:X'`; `{ scheme: 'B' }` → `'B'`.
 *
 * @param {WorkflowFilterEntry} entry
 * @return {*}  {string}
 */
export function workflowEntryToToken({ scheme, step }: WorkflowFilterEntry): string {
    return step ? `${scheme}${WORKFLOW_TOKEN_SEPARATOR}${step}` : scheme;
}

/**
 * Parses the `workflow` filter tokens (`schemeId` or `schemeId:stepId`) into the
 * `{ scheme, step? }` entries the drive-search request expects.
 *
 * @param {string[]} tokens
 * @return {*}  {WorkflowFilterEntry[]}
 */
export function parseWorkflowFilter(tokens: string[] = []): WorkflowFilterEntry[] {
    return tokens.map(parseWorkflowToken);
}

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
    languageId: multiSelector,
    // Each entry is `schemeId` or `schemeId:stepId`; comma-separated in the URL
    workflow: multiSelector
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

        // Field-filter (user-searchable) values are stored raw: the field type — not comma
        // sniffing — decides their shape downstream, so never split/trim them here.
        if (key.startsWith(USER_SEARCHABLE_PREFIX)) {
            acc[key] = singleSelector(value);

            return acc;
        }

        const decodeFunction = decodeByFilterKey[key];

        if (decodeFunction) {
            // Use decode function for known keys
            acc[key] = decodeFunction(value);
        } else {
            // Use default functions for unknown keys
            acc[key] = value.includes(',') ? multiSelector(value) : singleSelector(value);
        }

        return acc;
    }, {} as DotContentDriveFilters);
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
        }, [] as string[])
        .join(';');
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

/** True when the field type stores a `{ from, to }` date range (Date / Date-and-Time / Time). */
export function isDateFieldFilterType(fieldType: string): boolean {
    return (FIELD_FILTER_DATE_TYPES as readonly string[]).includes(fieldType);
}

/** True when the field type stores a list of values (Multi-Select / Checkbox / Tag / …). */
export function isMultiValueFieldFilterType(fieldType: string): boolean {
    return FIELD_FILTER_MULTI_VALUE_TYPES.includes(fieldType);
}

/**
 * True for a binary (boolean) checkbox — a Checkbox field with a single option (e.g. `|true`).
 * Unlike a multi-option checkbox, this is a single boolean *value* (true/false), not a selection.
 */
export function isBinaryCheckboxField(field: DotCMSContentTypeField): boolean {
    return (
        field.fieldType === FIELD_FILTER_CHECKBOX_TYPE &&
        getSingleSelectableFieldOptions(field.values ?? '', field.dataType).length <= 1
    );
}

/**
 * Reshapes a raw stored field-filter string into the payload value for its field type:
 * date → `{ from, to }`, multi-select → `string[]`, everything else → the raw string.
 * Returns `undefined` when the value is effectively empty (so callers can skip it).
 *
 * @param {string} raw - The raw value stored in the filter bag.
 * @param {string} fieldType - The content-type field type (e.g. `Text`, `Date`, `Multi-Select`).
 * @return {*}  {(DotContentDriveUserSearchableValue | undefined)}
 */
export function parseUserSearchableValue(
    raw: string,
    fieldType: string
): DotContentDriveUserSearchableValue | undefined {
    if (!raw) {
        return undefined;
    }

    if (isDateFieldFilterType(fieldType)) {
        const [from = '', to = ''] = raw.split(USER_SEARCHABLE_VALUE_SEPARATOR);

        return from || to ? { from, to } : undefined;
    }

    if (isMultiValueFieldFilterType(fieldType)) {
        const values = parseMultiValue(raw);

        return values.length ? values : undefined;
    }

    return raw;
}

/** Safe `decodeURIComponent` that returns the input unchanged on a malformed sequence. */
const safeDecode = (value: string): string => {
    try {
        return decodeURIComponent(value);
    } catch {
        return value;
    }
};

/**
 * Splits a stored multi-value string back into its values. Each value is percent-encoded on
 * serialize (see {@link serializeMultiValue}) so a value containing the separator — e.g. a tag
 * label like `"News, Press"` — round-trips intact.
 *
 * @param {string} raw
 * @return {*}  {string[]}
 */
export function parseMultiValue(raw: string): string[] {
    if (!raw) {
        return [];
    }

    return raw
        .split(USER_SEARCHABLE_VALUE_SEPARATOR)
        .map((value) => safeDecode(value.trim()))
        .filter(Boolean);
}

/**
 * Joins multi-value entries into the stored string, percent-encoding each value so it can safely
 * contain the separator. Inverse of {@link parseMultiValue}.
 *
 * @param {string[]} values
 * @return {*}  {string}
 */
export function serializeMultiValue(values: string[]): string {
    return values.map(encodeURIComponent).join(USER_SEARCHABLE_VALUE_SEPARATOR);
}

/**
 * Serializes a shaped field-filter value back into the raw string stored in the filter bag,
 * inverse of {@link parseUserSearchableValue}. Empty values serialize to `''` so the URL encoder
 * (which drops empty entries) leaves no dangling criterion.
 *
 * @param {(DotContentDriveUserSearchableValue | null | undefined)} value
 * @param {string} fieldType
 * @return {*}  {string}
 */
export function serializeUserSearchableValue(
    value: DotContentDriveUserSearchableValue | null | undefined,
    fieldType: string
): string {
    if (value == null) {
        return '';
    }

    if (isDateFieldFilterType(fieldType)) {
        const range = value as DotContentDriveDateRange;
        if (!range?.from && !range?.to) {
            return '';
        }

        return `${range.from ?? ''}${USER_SEARCHABLE_VALUE_SEPARATOR}${range.to ?? ''}`;
    }

    if (isMultiValueFieldFilterType(fieldType)) {
        return serializeMultiValue(Array.isArray(value) ? value : []);
    }

    return String(value);
}

/**
 * Builds the `userSearchable` payload object from the flat filter bag, keyed by field variable.
 * Only `us.`-prefixed entries whose field metadata is known (loaded) are considered. A binary
 * checkbox emits its boolean value when set (`true`/`false`); every field type is included only
 * when its value is non-empty. Returns `undefined` when there are no active field filters.
 *
 * @param {DotContentDriveFilters} filters - The full filter bag.
 * @param {DotCMSContentTypeField[]} fields - The active content type's searchable fields.
 * @return {*}  {(Record<string, DotContentDriveUserSearchableValue> | undefined)}
 */
export function buildUserSearchablePayload(
    filters: DotContentDriveFilters,
    fields: DotCMSContentTypeField[]
): Record<string, DotContentDriveUserSearchableValue> | undefined {
    const fieldByVariable = new Map(fields.map((field) => [field.variable, field]));
    const payload: Record<string, DotContentDriveUserSearchableValue> = {};

    for (const [key, raw] of Object.entries(filters ?? {})) {
        if (!key.startsWith(USER_SEARCHABLE_PREFIX)) {
            continue;
        }

        const variable = key.slice(USER_SEARCHABLE_PREFIX.length);
        const field = fieldByVariable.get(variable);
        if (!field) {
            continue;
        }

        const rawValue = Array.isArray(raw)
            ? raw.join(USER_SEARCHABLE_VALUE_SEPARATOR)
            : (raw ?? '');

        // A binary checkbox filters for the chosen boolean; empty means not filtering.
        if (isBinaryCheckboxField(field)) {
            if (rawValue === 'true' || rawValue === 'false') {
                payload[variable] = rawValue === 'true';
            }

            continue;
        }

        const value = parseUserSearchableValue(rawValue, field.fieldType);
        if (value === undefined) {
            continue;
        }

        payload[variable] = value;
    }

    return Object.keys(payload).length ? payload : undefined;
}
