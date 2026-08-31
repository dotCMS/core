/**
 * Recovers the key order of Key/Value fields from a contentlet's raw JSON text.
 *
 * ## Why this exists
 *
 * A Key/Value field is sent over REST as a JSON object. ECMAScript enumerates
 * integer-like keys first, in ascending numeric order, in **any** object — so the
 * moment `JSON.parse` builds one, a key such as `123` jumps to the front however
 * the user ordered it. The order is gone before any application code runs, and it
 * cannot be put back by reordering the object: assigning the keys again re-hoists
 * them.
 *
 * The only way to keep it is to never let those keys live in an object. This
 * module reads the order out of the raw response text and hands back **JSON text**,
 * which carries its pairs in written order and is what the form control then holds
 * end to end — see {@link restoreKeyValueOrder} for why text rather than an array.
 */

/**
 * Prefix applied to integer-like keys before parsing, so the engine treats them as
 * ordinary string keys and leaves them where they are.
 *
 * It starts with a NUL character deliberately. The prefix briefly occupies the
 * user's key namespace, and a key that collides with it would either be renamed on
 * the way out or silently overwrite a real one. NUL cannot be typed into the
 * editor, which makes that collision practically unreachable.
 */
const NUMERIC_KEY_PREFIX = '\u0000__dotKeyValue__';

/**
 * The same prefix as it must be written *into* the JSON text. A raw control
 * character is not legal inside a JSON string literal, so it goes in escaped and
 * `JSON.parse` turns it back into the character above.
 */
const NUMERIC_KEY_PREFIX_ESCAPED = '\\u0000__dotKeyValue__';

/** Matches a JSON *key* that is entirely digits. */
const NUMERIC_KEY = /"(\d+)"(\s*):/g;

/**
 * A Key/Value pair as it appears once order has been recovered.
 */
export interface OrderedKeyValueEntry {
    key: string;
    value: string;
}

/**
 * Parses JSON while preserving the source order of integer-like keys.
 *
 * Escaped quotes inside string values read as `\"` in the raw text, so the pattern
 * `"digits":` cannot match a value — only a key position.
 */
export const parsePreservingKeyOrder = (json: string): unknown =>
    JSON.parse(json.replace(NUMERIC_KEY, `"${NUMERIC_KEY_PREFIX_ESCAPED}$1"$2:`));

const stripPrefix = (key: string): string =>
    key.startsWith(NUMERIC_KEY_PREFIX) ? key.slice(NUMERIC_KEY_PREFIX.length) : key;

/**
 * True when a value has the shape a Key/Value field produces: a plain object whose
 * values are all primitives.
 *
 * The content type is not available at the point this runs, so the field is
 * identified by shape. Every other contentlet property is either a primitive, an
 * array (categories, relationships) or an object with nested structure (binaries),
 * so none of them match.
 */
const looksLikeKeyValueField = (value: unknown): value is Record<string, unknown> => {
    if (typeof value !== 'object' || value === null || Array.isArray(value)) {
        return false;
    }

    return Object.values(value).every((entry) => entry === null || typeof entry !== 'object');
};

/** Serialises pairs to JSON text without ever building an object from them. */
const toJsonText = (entries: OrderedKeyValueEntry[]): string =>
    `{${entries
        .map(({ key, value }) => `${JSON.stringify(key)}:${JSON.stringify(value)}`)
        .join(',')}}`;

/**
 * Reads pairs back out of the JSON text a Key/Value field carries.
 *
 * Uses the order-preserving parse, so integer-like keys stay where the text put
 * them — a plain `JSON.parse` here would undo the whole point.
 */
export const parseOrderedKeyValue = (text: string): OrderedKeyValueEntry[] => {
    let parsed: unknown;

    try {
        parsed = parsePreservingKeyOrder(text);
    } catch {
        return [];
    }

    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        return [];
    }

    return Object.entries(parsed as Record<string, unknown>).map(([key, value]) => ({
        key: stripPrefix(key),
        value: value === null ? 'null' : String(value)
    }));
};

/**
 * Replaces every Key/Value-shaped property of a contentlet with the **JSON text**
 * of its pairs, in the order the response carried them.
 *
 * Text rather than an array on purpose: this value goes straight into a form
 * control, and whatever sits there is what gets sent back on save. The backend
 * accepts a JSON string for these fields but not a list, so a contentlet opened
 * and saved untouched round-trips correctly — and the order survives, which an
 * object could not manage.
 *
 * Anything that cannot be recovered is left exactly as the normal parse produced
 * it, so a malformed response degrades instead of failing the load.
 *
 * @param contentlet the contentlet from a normal `JSON.parse`
 * @param orderedContentlet the same contentlet from {@link parsePreservingKeyOrder}.
 *   It must come from the raw response text: re-serializing the parsed contentlet
 *   would carry the hoisted order and recover nothing.
 */
export const restoreKeyValueOrder = <T extends Record<string, unknown>>(
    contentlet: T,
    orderedContentlet: unknown
): T => {
    if (typeof orderedContentlet !== 'object' || orderedContentlet === null) {
        return contentlet;
    }

    const ordered = orderedContentlet as Record<string, unknown>;
    const result: Record<string, unknown> = { ...contentlet };

    for (const [field, value] of Object.entries(contentlet)) {
        if (!looksLikeKeyValueField(value) || !looksLikeKeyValueField(ordered[field])) {
            continue;
        }

        const entries: OrderedKeyValueEntry[] = Object.entries(
            ordered[field] as Record<string, unknown>
        ).map(([key, entryValue]) => ({
            key: stripPrefix(key),
            value: entryValue === null ? 'null' : String(entryValue)
        }));

        // Only trust the recovered order when it accounts for exactly the same keys.
        const sameKeys =
            entries.length === Object.keys(value).length &&
            entries.every(({ key }) => key in value);

        result[field] = sameKeys ? toJsonText(entries) : value;
    }

    return result as T;
};
