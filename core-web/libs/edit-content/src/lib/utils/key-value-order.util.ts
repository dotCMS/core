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
 * The only way to keep it is to never let those keys live in an object. This module
 * reads the order out of the raw response text and exposes it as **JSON text**, which
 * carries its pairs in written order and is what the form control then holds end to
 * end — see {@link orderedKeyValueText} for why text rather than an array.
 *
 * ## What it deliberately does not do
 *
 * It never decides which properties of a contentlet are Key/Value fields. An earlier
 * version guessed by shape — "a plain object whose values are all primitives" — and a
 * binary field's `metaData` matches that exactly, so it was rewritten into text and the
 * file preview broke on reload. Only the content type knows a field's type, so the
 * recovered data is attached untouched under {@link ORDERED_FIELDS} and the Key/Value
 * resolver, which is reached by declared field type, picks out its own variable.
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
 * Where the order-preserving parse of the response is parked on the contentlet.
 *
 * A separate namespace, so every existing property is byte-for-byte what the normal
 * parse produced and no consumer of `getContentById` sees a changed shape.
 */
export const ORDERED_FIELDS = '__dotOrderedFields' as const;

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
 * Parks the order-preserving parse of a contentlet on the contentlet itself, under
 * {@link ORDERED_FIELDS}. Every real property is left exactly as it was.
 *
 * Returns the contentlet unchanged when the recovery is unusable, so a malformed
 * response costs the ordering and never the load.
 */
export const attachOrderedFields = <T extends Record<string, unknown>>(
    contentlet: T,
    orderedContentlet: unknown
): T => {
    if (typeof orderedContentlet !== 'object' || orderedContentlet === null) {
        return contentlet;
    }

    return { ...contentlet, [ORDERED_FIELDS]: orderedContentlet } as T;
};

/**
 * The ordered **JSON text** of one Key/Value field, or `null` when it cannot be
 * recovered.
 *
 * Text rather than an array on purpose: this value goes straight into a form control,
 * and whatever sits there is what gets sent back on save. The backend accepts a JSON
 * string for these fields but not a list, so a contentlet opened and saved untouched
 * round-trips correctly — and the order survives, which an object could not manage.
 *
 * @param contentlet a contentlet that went through {@link attachOrderedFields}
 * @param variable the field's variable name, known from the content type
 */
export const orderedKeyValueText = (
    contentlet: Record<string, unknown> | null | undefined,
    variable: string
): string | null => {
    const ordered = contentlet?.[ORDERED_FIELDS] as Record<string, unknown> | undefined;
    const recovered = ordered?.[variable];
    const plain = contentlet?.[variable];

    if (
        typeof recovered !== 'object' ||
        recovered === null ||
        Array.isArray(recovered) ||
        typeof plain !== 'object' ||
        plain === null ||
        Array.isArray(plain)
    ) {
        return null;
    }

    const entries: OrderedKeyValueEntry[] = Object.entries(
        recovered as Record<string, unknown>
    ).map(([key, value]) => ({
        key: stripPrefix(key),
        value: value === null ? 'null' : String(value)
    }));

    // Only trust the recovered order when it accounts for exactly the same keys.
    const source = plain as Record<string, unknown>;
    const sameKeys =
        entries.length === Object.keys(source).length && entries.every(({ key }) => key in source);

    return sameKeys ? toJsonText(entries) : null;
};
