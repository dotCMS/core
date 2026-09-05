import { format } from 'date-fns';

import {
    DotCMSContentTypeField,
    DotContentDriveDateRange,
    DotContentDriveUserSearchableValue
} from '@dotcms/dotcms-models';

import {
    FIELD_FILTER_CHECKBOX_TYPE,
    FIELD_FILTER_DATE_TYPES,
    FIELD_FILTER_KEY_VALUE_TYPE,
    FIELD_FILTER_MULTI_VALUE_TYPES,
    USER_SEARCHABLE_PREFIX,
    USER_SEARCHABLE_VALUE_SEPARATOR
} from './constants';
import { getSingleSelectableFieldOptions } from './field-options.util';

/**
 * The value layer behind the field-filter chips: how a `us.<variable>` entry is stored in a flat
 * filter bag, and how it becomes the `userSearchable` payload the browse endpoint takes.
 *
 * Moved out of the Content Drive portlet unchanged. Both surfaces build the same request from the
 * same bag, so the reshaping has to be one implementation — the alternative is two, drifting, and
 * that is the drift this feature exists to end. The portlet re-exports these so its own store and
 * URL layer keep their imports.
 *
 * `filters` is typed as a plain bag rather than either surface's filter type: the functions only
 * ever read `us.`-prefixed string values, and both surfaces' types satisfy that.
 */
type FilterBag = Record<string, string | string[] | undefined>;

/** True when the field type stores a `{ from, to }` date range (Date / Date-and-Time / Time). */
export function isDateFieldFilterType(fieldType: string): boolean {
    return (FIELD_FILTER_DATE_TYPES as readonly string[]).includes(fieldType);
}

/** True when the field type stores a list of values (Multi-Select / Checkbox / Tag / …). */
export function isMultiValueFieldFilterType(fieldType: string): boolean {
    return FIELD_FILTER_MULTI_VALUE_TYPES.includes(fieldType);
}

/**
 * The field variables that have a `us.*` entry in the bag, in insertion order.
 *
 * This is what restores the visible chips on a surface that persists its filters: Content Drive
 * deep-links them, so the chip row has to be rebuilt from the bag alone.
 *
 * @param filters The full filter bag.
 * @return The field variables, without the prefix.
 */
export function getUserSearchableActive(filters: FilterBag): string[] {
    return Object.keys(filters ?? {})
        .filter((key) => key.startsWith(USER_SEARCHABLE_PREFIX))
        .map((key) => key.slice(USER_SEARCHABLE_PREFIX.length));
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
 * @param raw The raw value stored in the filter bag.
 * @param fieldType The content-type field type (e.g. `Text`, `Date`, `Multi-Select`).
 * @return The shaped value, or `undefined` when there is nothing to filter on.
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

    if (fieldType === FIELD_FILTER_KEY_VALUE_TYPE) {
        return toKeyValueTerm(raw);
    }

    return raw;
}

/**
 * Translates a Key/Value filter input into the term the backend contains-matches against the
 * indexed `.key_value` subfield (stored as `key_value` = `key + "_" + value`).
 *
 * The term is lowercased to match the indexed `.key_value` sub-field, which dotCMS stores as
 * `(key + "_" + value).toLowerCase()` — so `Color:Red` matches the same content as `color:red`.
 *
 * Shorthand rules (the **first** colon is the key/value separator — everything after it is the
 * value, so a value may itself contain colons):
 * - `key:value`         → `key_value`           (exact-pair match; e.g. `Deploy:HTTPS://x` → `deploy_https://x`)
 * - `key:` / `:value`   → `key` / `value`       (only the filled side)
 * - bare term (no `:`)  → the term              (loose match on a key OR a value)
 *
 * ⚠️ Greedy shorthand: because *any* colon is treated as the separator, a **bare** value that
 * happens to contain a colon (a URL like `https://x`, a time like `12:30`, a ratio like `16:9`) is
 * read as `key:value` (`https_//x`, `12_30`, `16_9`) and will likely match nothing. To search a
 * colon-bearing value, prefix it with its key (`myKey:12:30`) so the intended value is preserved.
 * A raw colon is never sent to the backend — that path is metadata-only and wouldn't match a
 * regular Key/Value field anyway.
 *
 * @param raw The literal value the user typed (also what is kept in the URL/chip).
 * @return The term, or `undefined` when the input is empty.
 */
function toKeyValueTerm(raw: string): string | undefined {
    const trimmed = raw.trim();
    if (!trimmed) {
        return undefined;
    }

    // Split on the FIRST colon only, so a value may contain further colons (e.g. `key:12:30`).
    const separator = trimmed.indexOf(':');
    // The index stores `.key_value` as `(key + "_" + value).toLowerCase()`, so the term is
    // lowercased to match regardless of the case the user typed (e.g. `Color:Red` → `color_red`).
    if (separator === -1) {
        return trimmed.toLowerCase();
    }

    const key = trimmed.slice(0, separator).trim();
    const value = trimmed.slice(separator + 1).trim();

    if (key && value) {
        return `${key}_${value}`.toLowerCase();
    }

    // Only one side of the `key:value` was filled — match on whichever is present.
    return (key || value).toLowerCase() || undefined;
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
 */
export function serializeMultiValue(values: string[]): string {
    return values.map(encodeURIComponent).join(USER_SEARCHABLE_VALUE_SEPARATOR);
}

/** Narrows a user-searchable value to a `{ from, to }` date range (object, not array). */
function isDateRange(value: DotContentDriveUserSearchableValue): value is DotContentDriveDateRange {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Formats a Date as a timezone-naive local wall-clock ISO string (`yyyy-MM-ddTHH:mm:ss`, no `Z` or
 * offset) — i.e. the exact date/time the user sees in the picker.
 *
 * Date/Date-and-Time/Time filters must send the wall-clock, not a UTC instant: `toISOString()`
 * shifts by the browser's offset (a UTC-3 user's 10:00 becomes `13:00Z`), and the backend then
 * parses that `Z` value as an instant and reformats it in the SERVER zone — so the bound no longer
 * matches what the user picked. A no-offset value instead round-trips as identity: the backend
 * parses it in the server zone and formats it back in the server zone (see
 * `BrowserAPIImpl#parseFlexibleDate` → `LocalDateTime.parse(...)` and `normalizeDateBound`). On the
 * FE, `new Date('…T10:00:00')` (no offset) also parses as local, so the picker round-trips too.
 *
 * Returns `''` for an invalid/absent Date: the typeable Time picker (`[keepInvalid]="true"`) can
 * emit an `Invalid Date` mid-typing, and `date-fns` `format` throws `RangeError` on one — so a
 * partial time simply clears that bound instead of blowing up the range application.
 */
export function toLocalIsoString(date: Date): string {
    if (!date || Number.isNaN(date.getTime())) {
        return '';
    }

    // `date-fns` formats by the Date's LOCAL components, so this is the wall-clock with no offset/Z.
    return format(date, "yyyy-MM-dd'T'HH:mm:ss");
}

/**
 * Serializes a shaped field-filter value back into the raw string stored in the filter bag,
 * inverse of {@link parseUserSearchableValue}. Empty values serialize to `''` so a surface that
 * drops empty entries leaves no dangling criterion.
 */
export function serializeUserSearchableValue(
    value: DotContentDriveUserSearchableValue | null | undefined,
    fieldType: string
): string {
    if (value == null) {
        return '';
    }

    if (isDateFieldFilterType(fieldType)) {
        // Guard the shape rather than blindly casting: a mismatched fieldType/value pair yields ''
        // (not filtering) instead of a misleading partial range.
        if (!isDateRange(value)) {
            return '';
        }

        if (!value.from && !value.to) {
            return '';
        }

        return `${value.from ?? ''}${USER_SEARCHABLE_VALUE_SEPARATOR}${value.to ?? ''}`;
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
 * Returning `undefined` rather than `{}` is load-bearing on both surfaces: an absent key leaves
 * the request byte-identical to one that never mentioned `userSearchable`.
 *
 * @param filters The full filter bag.
 * @param fields The active content type's searchable fields.
 * @return The payload, or `undefined` when no field filter carries a value.
 */
export function buildUserSearchablePayload(
    filters: FilterBag,
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
