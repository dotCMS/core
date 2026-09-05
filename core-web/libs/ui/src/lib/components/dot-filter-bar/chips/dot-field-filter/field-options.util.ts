import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

/**
 * Option parsing for the field-filter chips, shared with `@dotcms/edit-content`.
 *
 * These three helpers were written for the *edit* side and are reused verbatim by the filter: a
 * Select field offers the same options whether you are setting its value or matching against it.
 * They live here because `@dotcms/ui` cannot import `@dotcms/edit-content` — that dependency runs
 * the other way, in ~98 files — and `edit-content` re-exports them so its own callers are
 * untouched.
 *
 * Pure functions with no Angular surface, which is why moving them is safe: nothing here reaches a
 * store, a service or the DOM.
 */

/** Value shapes a single-selectable field's option may carry. */
export type DotSingleSelectableValue = string | boolean | number;

/**
 * The three `dataType`s a single-selectable field can declare.
 *
 * Compared as plain strings rather than against `edit-content`'s enum: the enum has 40-odd
 * consumers over there and does not need to move for these functions to.
 */
const SINGLE_SELECTABLE_DATA_TYPE = {
    BOOL: 'BOOL',
    INTEGER: 'INTEGER',
    FLOAT: 'FLOAT'
} as const;

/**
 * String option values a True/False field may legitimately use for `true`.
 *
 * A True/False field's options are authored by the user, and dotCMS invites them to use the
 * database's own representation: the Radio field's help text gives `True|1 False|0` as the example,
 * `SelectableValuesField.check()` accepts those plus `y`/`n`, `t`/`f` and `on`/`off`, and the product
 * itself ships `Host.runDashboard` as `Yes|1 / No|0`. The backend coerces the whole set through
 * commons-lang `BooleanUtils.toBoolean` on save, so this mirrors it — matching only `'true'` made
 * every such option collapse to `false`, i.e. two options with the same value.
 */
const BOOL_TRUE_TOKENS = new Set(['true', '1', 'y', 'yes', 't', 'on']);

/**
 * Casts a raw option value to the type its field's `dataType` declares.
 *
 * @param value The raw option value.
 * @param type The field's `dataType`.
 * @return The cast value, or `null` when it is absent or uncastable.
 */
export const castSingleSelectableValue = (
    value: unknown,
    type: string
): DotSingleSelectableValue | null => {
    // Early return for null/undefined/empty values
    if (value === null || value === undefined || value === '') {
        return null;
    }

    switch (type) {
        case SINGLE_SELECTABLE_DATA_TYPE.BOOL: {
            // For boolean type, handle both boolean and string values
            return typeof value === 'boolean'
                ? value
                : BOOL_TRUE_TOKENS.has(String(value).toLowerCase().trim());
        }

        case SINGLE_SELECTABLE_DATA_TYPE.INTEGER:

        // fallthrough
        case SINGLE_SELECTABLE_DATA_TYPE.FLOAT: {
            const num = Number(value);

            return isNaN(num) ? null : num;
        }

        default: {
            return String(value);
        }
    }
};

/**
 * Parses field options for single selectable fields (Checkbox, Radio, Select).
 *
 * The function handles the following formats:
 *
 * 1. Multi-line pipe format (standard format):
 *    ```
 *    label1|value1
 *    label2|value2
 *    ```
 *    Each line represents a separate option with label and value separated by pipe.
 *
 * 2. Special case for checkboxes:
 *    ```
 *    |true
 *    ```
 *    Creates a checkbox without label, using the value after the pipe.
 *
 * 3. Simple value format:
 *    ```
 *    value1,value2,value3
 *    ```
 *    When no pipes are present, each comma-separated value is used as both label and value.
 *
 * Note: If the input contains line breaks, it will be treated as a single option,
 * preserving the line breaks as part of the option text.
 *
 * Pipe detection is applied per option, so a single option (`label|value`),
 * multi-line options (`label|value` per line) and comma-separated options
 * (`label|value,label|value`) are all parsed correctly. Options without a pipe
 * use the whole string as both label and value.
 *
 * @param options - The string containing the options to parse
 * @param dataType - The data type of the field
 * @returns Array of parsed options with label and value
 */
export const getSingleSelectableFieldOptions = (
    options: string,
    dataType: string
): { label: string; value: DotSingleSelectableValue }[] => {
    if (!options?.trim()) return [];

    const LINE_BREAKS_REGEX = /\r\n|\n|\r/;
    const hasLineBreaks = LINE_BREAKS_REGEX.test(options);

    let items: string[] = [];

    if (hasLineBreaks) {
        // Multi-line format (standard dotCMS format)
        items = options.split(LINE_BREAKS_REGEX).filter((line) => line.trim());
    } else if (options.trim().startsWith('|')) {
        // Special case: "|true" (checkbox without label)
        items = [options.trim()];
    } else {
        // Comma-separated format
        items = options
            .split(',')
            .map((v) => v.trim())
            .filter(Boolean);
    }

    // Handle nested line breaks in single items
    if (items.length === 1 && LINE_BREAKS_REGEX.test(items[0])) {
        items = items[0].split(LINE_BREAKS_REGEX).filter((line) => line.trim());
    }

    return items
        .map((item) => {
            let label: string;
            let value: string;

            if (item.includes('|')) {
                const parts = item.split('|');
                // If a pipe is present, label is the first part and value the second;
                // if there's no second part, value equals label
                label = (parts[0] || '').trim();
                value = parts[1]?.trim() || label;
            } else {
                // No pipe: label and value are the same
                label = item.trim();
                value = label;
            }

            if (!value) return null;

            const castedValue = castSingleSelectableValue(value, dataType);

            return castedValue !== null ? { label, value: castedValue } : null;
        })
        .filter(
            (item): item is { label: string; value: DotSingleSelectableValue } => item !== null
        );
};

/**
 * Extracts the content type ID from a relationship field.
 *
 * @param field - The DotCMS content type field object containing the relationship data
 * @returns The content type ID, or null if not found
 */
export function getContentTypeIdFromRelationship(field: DotCMSContentTypeField): string | null {
    if (!field?.relationships?.velocityVar) {
        return null;
    }

    const [contentTypeId] = field.relationships.velocityVar.split('.');

    return contentTypeId || null;
}
