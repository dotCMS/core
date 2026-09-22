import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSFieldType,
    DotCMSFieldTypes,
    FieldOf
} from '@dotcms/dotcms-models';

import {
    CALENDAR_FIELD_TYPES,
    FLATTENED_FIELD_TYPES,
    UNCASTED_FIELD_TYPES
} from '../../models/dot-edit-content-field.constant';
import { EditContentQueryParams } from '../../store/edit-content.store';
import {
    castSingleSelectableValue,
    getSingleSelectableFieldOptions,
    parseCalendarTimestamp
} from '../../utils/functions.util';
import { orderedKeyValueText } from '../../utils/key-value-order.util';
import { getRelationshipFromContentlet } from '../../utils/relationshipFromContentlet';

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @param {EditContentQueryParams} queryParams - Optional query params from the URL.
 * @returns {*} The resolved value for the field.
 */
export type FnResolutionValue<T> = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField,
    queryParams?: EditContentQueryParams,
    isManualTranslation?: boolean
) => T;

/**
 * The same signature, narrowed to one field type.
 *
 * A resolver declared with this receives the arm its key registers it under, checked by the
 * compiler rather than assumed. `FnResolutionValue` alone cannot give that guarantee here:
 * this library compiles with `strict: false`, so parameters compare bivariantly and a narrowed
 * parameter on a union-wide signature is accepted without being verified (FR-007).
 */
export type FnResolutionValueFor<K extends DotCMSFieldType, T> = (
    contentlet: DotCMSContentlet,
    field: FieldOf<K>,
    queryParams?: EditContentQueryParams,
    isManualTranslation?: boolean
) => T;

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @returns {*} The resolved value for the field.
 */
const emptyResolutionFn: FnResolutionValue<string> = () => '';

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const defaultResolutionFn: FnResolutionValue<string> = (
    contentlet,
    field,
    _queryParams,
    isManualTranslation
) => {
    if (contentlet) {
        return contentlet[field.variable] ?? field.defaultValue;
    }
    return isManualTranslation ? null : field.defaultValue;
};

/**
 * Resolves a Key/Value field, preferring the key order the response actually carried.
 *
 * A JavaScript object cannot hold that order: integer-like keys are enumerated first,
 * so a key such as `123` is at the front of `contentlet[variable]` however the user
 * arranged it. `getContentById` parks an order-preserving copy on the contentlet, and
 * this is the one place that may read it — reached by declared field type, so no other
 * field can be mistaken for this one.
 *
 * Falls back to the plain value when there is nothing to recover: a contentlet loaded
 * by some other path, or a response that could not be re-parsed.
 */
const keyValueResolutionFn: FnResolutionValue<string> = (
    contentlet,
    field,
    queryParams,
    isManualTranslation
) =>
    (contentlet &&
        orderedKeyValueText(contentlet as unknown as Record<string, unknown>, field.variable)) ||
    defaultResolutionFn(contentlet, field, queryParams, isManualTranslation);

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const textFieldResolutionFn: FnResolutionValue<string> = (
    contentlet,
    field,
    _queryParams,
    isManualTranslation
) => {
    if (!contentlet) {
        return isManualTranslation ? null : field.defaultValue;
    }

    const value = contentlet[field.variable] ?? field.defaultValue;

    const shouldRemoveLeadingSlash =
        contentlet?.baseType === 'HTMLPAGE' &&
        field.variable === 'url' &&
        typeof value === 'string' &&
        value.startsWith('/');

    return shouldRemoveLeadingSlash ? value.substring(1) : value;
};

/**
 * Resolves the host folder path for a contentlet based on its type and URL structure.
 *
 * For FILEASSET and HTMLPAGE, removes the last path segment to get the parent path:
 * - File assets: the last segment is the filename, so the result is the directory path.
 * - Pages: the last segment is the page URL segment (e.g. /about/team), so the result is the path of the parent folder.
 * For other content types, extracts the path up to the '/content' segment.
 *
 * @param contentlet - The contentlet object containing hostName, url, and type
 * @param field - The field object containing the default value
 * @returns The resolved host folder path or the field's default value
 */
// isManualTranslation not needed: null contentlet already falls back to queryParams / field.defaultValue.
const hostFolderResolutionFn: FnResolutionValue<string> = (contentlet, field, queryParams) => {
    // For new content, prefer folderPath from query params over field default
    if (!contentlet?.hostName || !contentlet?.url) {
        return queryParams?.folderPath || field?.defaultValue || '';
    }

    const { hostName, url, baseType } = contentlet;

    // Ensure hostName and url are strings
    if (typeof hostName !== 'string' || typeof url !== 'string') {
        return field?.defaultValue || '';
    }

    const fullPath = `${hostName}${url}`;

    try {
        if (
            baseType === DotCMSBaseTypesContentTypes.FILEASSET ||
            baseType === DotCMSBaseTypesContentTypes.HTMLPAGE
        ) {
            // Remove the last path segment: filename for file assets, page URL segment for pages
            const pathSegments = fullPath.split('/');
            if (pathSegments.length > 1) {
                return pathSegments.slice(0, -1).join('/');
            }
            return fullPath;
        } else {
            // For other content types, extract path up to '/content'
            const contentIndex = fullPath.indexOf('/content');
            if (contentIndex !== -1) {
                return fullPath.slice(0, contentIndex);
            }
            return fullPath;
        }
    } catch (error) {
        console.warn('Error processing host folder path:', error);
        return field?.defaultValue || '';
    }
};

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const categoryResolutionFn: FnResolutionValue<string[] | string> = (
    contentlet,
    field,
    _queryParams,
    isManualTranslation
) => {
    const values = contentlet?.[field.variable];

    if (Array.isArray(values)) {
        // isManualTranslation is not checked here: manual translation always passes
        // contentlet=null, so the array branch is never reached in that path.
        return values.map((item) => Object.keys(item)[0]);
    }

    return isManualTranslation ? [] : (field.defaultValue ?? []);
};

/**
 * Resolution function for date/time fields
 * Backend always returns numeric timestamps when value exists, or the field is not included
 *
 * @param {DotCMSContentlet} contentlet - The contentlet object
 * @param {DotCMSContentTypeField} field - The field object
 * @returns {number | null} Numeric timestamp or null if no value
 */
const dateResolutionFn: FnResolutionValue<number | null> = (contentlet, field) => {
    if (!contentlet) {
        // For new content, let the calendar component handle defaultValue processing
        // The calendar component has proper logic for "now" and fixed dates with server timezone
        return null;
    }

    const value = contentlet[field.variable];
    const timestamp = parseCalendarTimestamp(value);

    // Preserve diagnostics: backend should always return numeric timestamps, so a
    // non-empty value that fails to parse signals an unexpected payload worth logging.
    if (timestamp == null && value != null && value !== '') {
        console.warn('Calendar field received unexpected value from backend:', {
            fieldVariable: field.variable,
            value,
            type: typeof value
        });
    }

    return timestamp ?? null;
};

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const relationshipResolutionFn: FnResolutionValue<string> = (contentlet, field) => {
    const relationship = getRelationshipFromContentlet({
        contentlet,
        variable: field.variable
    });

    return relationship.map((item) => item.identifier).join(',');
};

/**
 * Resolution function for block editor fields.
 * The API may return block editor content as a JSON string when copying/translating.
 * This function parses the string to an object so the block editor component receives structured data.
 */
const blockEditorResolutionFn: FnResolutionValue<string | Record<string, unknown>> = (
    contentlet,
    field,
    _queryParams,
    isManualTranslation
) => {
    if (!contentlet) {
        return isManualTranslation ? null : field.defaultValue;
    }

    const value = contentlet[field.variable] ?? field.defaultValue;

    if (typeof value === 'string' && value.trim().startsWith('{')) {
        try {
            return JSON.parse(value);
        } catch {
            return value;
        }
    }

    return value;
};

const selectResolutionFn: FnResolutionValue<string> = (
    contentlet,
    field,
    _queryParams,
    isManualTranslation
) => {
    if (!contentlet && isManualTranslation) return null;

    const value = contentlet
        ? (contentlet[field.variable] ?? field.defaultValue)
        : field.defaultValue;
    if (value === null || value === undefined || value === '') {
        const options = getSingleSelectableFieldOptions(field?.values || '', field.dataType);
        return options[0]?.value;
    }
    return value;
};

/**
 * The resolutionValue variable is a record that is responsible for mapping and transforming the
 * saved value in the contentlet to its corresponding form representation, based on the field type.
 * This enables each field type to properly process its own data.
 *
 */
export const resolutionValue: Record<
    DotCMSFieldType,
    FnResolutionValue<string | string[] | Date | number | Record<string, unknown> | null>
> = {
    [DotCMSFieldTypes.BINARY]: defaultResolutionFn,
    [DotCMSFieldTypes.FILE]: defaultResolutionFn,
    [DotCMSFieldTypes.IMAGE]: defaultResolutionFn,
    [DotCMSFieldTypes.BLOCK_EDITOR]: blockEditorResolutionFn,
    [DotCMSFieldTypes.CHECKBOX]: defaultResolutionFn,
    [DotCMSFieldTypes.CONSTANT]: defaultResolutionFn,
    [DotCMSFieldTypes.CUSTOM_FIELD]: defaultResolutionFn,
    [DotCMSFieldTypes.DATE]: dateResolutionFn,
    [DotCMSFieldTypes.DATE_AND_TIME]: dateResolutionFn,
    [DotCMSFieldTypes.TIME]: dateResolutionFn,
    [DotCMSFieldTypes.HIDDEN]: defaultResolutionFn,
    [DotCMSFieldTypes.HOST_FOLDER]: hostFolderResolutionFn,
    [DotCMSFieldTypes.JSON]: defaultResolutionFn,
    [DotCMSFieldTypes.KEY_VALUE]: keyValueResolutionFn,
    [DotCMSFieldTypes.MULTI_SELECT]: defaultResolutionFn,
    [DotCMSFieldTypes.RADIO]: defaultResolutionFn,
    [DotCMSFieldTypes.SELECT]: selectResolutionFn,
    [DotCMSFieldTypes.TAG]: defaultResolutionFn,
    [DotCMSFieldTypes.TEXT]: textFieldResolutionFn,
    [DotCMSFieldTypes.TEXTAREA]: defaultResolutionFn,
    [DotCMSFieldTypes.WYSIWYG]: defaultResolutionFn,
    [DotCMSFieldTypes.CATEGORY]: categoryResolutionFn,
    [DotCMSFieldTypes.RELATIONSHIP]: relationshipResolutionFn,
    [DotCMSFieldTypes.LINE_DIVIDER]: emptyResolutionFn,
    // The four layout types. The map used to be keyed by the local FIELD_TYPES enum, which
    // never declared them, so they simply had no entry — invisible because the lookup was
    // reached through an `as FIELD_TYPES` assertion. Keyed by the shared vocabulary the map is
    // exhaustive by construction, and a layout field resolves to '' because it holds no value
    // of its own: rows, columns and dividers arrange other fields, they do not carry data.
    [DotCMSFieldTypes.ROW]: emptyResolutionFn,
    [DotCMSFieldTypes.COLUMN]: emptyResolutionFn,
    [DotCMSFieldTypes.TAB_DIVIDER]: emptyResolutionFn,
    [DotCMSFieldTypes.COLUMN_BREAK]: emptyResolutionFn
};

/**
 * Turns a contentlet's stored value into the value its form control holds.
 *
 * **This is the single transformation path** — what issue #31911 asked for and did not get. The
 * work used to run in two stages that each branched on the field type: `resolutionValue` read
 * the value out of the contentlet, then `getFinalCastedValue` cast it again, and both reached
 * their branch through a `fieldType` assertion. Two passes meant two places to keep in step,
 * and they had already drifted — `resolutionValue` covered 24 field types while the vocabulary
 * had 28.
 *
 * The stages are still distinguishable inside this function, and deliberately so: reading a
 * value out of a contentlet and casting it for a control are different jobs, and several field
 * types need only the first. What has gone is the second dispatch on field type and the
 * assertion that reached it — callers now have one function to call and one place to look.
 *
 * @param contentlet The contentlet being edited, or null during manual translation.
 * @param field The field whose control is being populated.
 * @param queryParams Query params from the URL, used to seed a field on a new contentlet.
 * @param isManualTranslation Whether the form is being re-initialised for a manual translation.
 * @returns The value for the form control, or null when there is none.
 */
const castResolvedValue = (value: unknown, field: DotCMSContentTypeField): unknown => {
    if (CALENDAR_FIELD_TYPES.includes(field.fieldType)) {
        return value;
    }

    if (FLATTENED_FIELD_TYPES.includes(field.fieldType)) {
        return (value as string)?.split(',').map((item) => item.trim());
    }

    if (value === undefined || UNCASTED_FIELD_TYPES.includes(field.fieldType)) {
        return value;
    }

    if (field.fieldType === DotCMSFieldTypes.JSON) {
        // Indented on purpose: Monaco would otherwise show the value as one flat line.
        return JSON.stringify(value, null, 2);
    }

    return castSingleSelectableValue(value, field.dataType);
};

export const resolveFieldValue = (
    contentlet: DotCMSContentlet | null,
    field: DotCMSContentTypeField,
    queryParams?: EditContentQueryParams,
    isManualTranslation = false
): unknown => {
    const resolve = resolutionValue[field.fieldType];

    // Exhaustive by construction — the map is keyed by the vocabulary — but the content type
    // arrives from the backend, whose set of field types is open (FR-013). A plugin's field
    // type reaches here with no resolver, and the form shows it as unsupported rather than
    // failing to build.
    if (!resolve) {
        console.warn(`No resolution function found for field type: ${field.fieldType}`);

        return null;
    }

    const value = resolve(contentlet, field, queryParams, isManualTranslation);

    return castResolvedValue(value, field) ?? null;
};
