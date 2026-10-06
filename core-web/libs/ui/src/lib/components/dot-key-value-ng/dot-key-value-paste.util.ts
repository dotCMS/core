import { DotKeyValue } from './dot-key-value-ng.component';

/**
 * Reads a pasted block into pairs. Two shapes are understood, tried in this order:
 *
 * 1. A JSON object — a whole `{...}`, or the loose `"key": value,` lines someone
 *    selects out of the middle of one.
 * 2. `KEY=VALUE` lines — the shape of a `.env` file.
 *
 * The `.env` side is deliberately a small subset of dotenv. It covers what someone
 * actually copies out of a `.env`: comments, blank lines, `export` prefixes and
 * quoted values. It does **not** cover multi-line values, variable interpolation
 * (`${OTHER}`) or escape sequences — a certificate pasted here would come out
 * truncated at its first line break, so those stay a single-pair job through the
 * normal inputs.
 */

/** A line worth reading: anything before the first `=`, and everything after it. */
const ASSIGNMENT = /^\s*(?:export\s+)?([^=\s]+)\s*=(.*)$/;

/**
 * Removes one matching pair of surrounding quotes.
 *
 * Only a matching pair, and only the outermost: `"a"b"` keeps its inner quotes, and
 * `'it's'` is left alone rather than mangled.
 */
const unquote = (value: string): string => {
    const trimmed = value.trim();
    const quote = trimmed[0];

    if ((quote === '"' || quote === "'") && trimmed.length > 1 && trimmed.endsWith(quote)) {
        return trimmed.slice(1, -1);
    }

    return trimmed;
};

/**
 * Reads the text as a JSON object, or `null` when it is not one.
 *
 * Two attempts, because people paste both the whole file and a selection out of its
 * middle: the text as given, then the text wrapped in braces with a trailing comma
 * dropped, which is what `"id": 1,` on its own needs to become an object.
 *
 * Only a plain object counts. A top-level array, string or number parses fine and is
 * still rejected — there are no keys in it to make pairs from — and so is anything
 * genuinely malformed, such as a selection that cut a string in half. Those fall
 * through to the `KEY=VALUE` reader, and from there to the browser's own paste, where
 * the user can see the text and fix it.
 */
const parseJsonObject = (text: string): Record<string, unknown> | null => {
    const trimmed = text.trim();

    if (!trimmed) {
        return null;
    }

    const candidates = [trimmed, `{${trimmed.replace(/,\s*$/, '')}}`];

    for (const candidate of candidates) {
        try {
            const parsed: unknown = JSON.parse(candidate);

            if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
                return parsed as Record<string, unknown>;
            }
        } catch {
            // Not this shape. The next candidate, or the KEY=VALUE reader, gets a turn.
        }
    }

    return null;
};

/**
 * Renders one JSON value as the string a pair holds, or `null` for a value that
 * cannot become one.
 *
 * A number or a boolean is written the way the JSON wrote it. An object or an array
 * keeps its JSON text, so a nested block survives the paste and stays visible and
 * editable instead of being dropped in silence.
 *
 * `null` and a blank string are skipped, for the same reason `KEY=` is: the entry row
 * refuses a blank value and so does an in-place edit, and a paste is not a way around
 * that.
 */
const jsonValueToString = (value: unknown): string | null => {
    if (value === null || value === undefined) {
        return null;
    }

    const text = typeof value === 'object' ? JSON.stringify(value) : String(value);

    return text.trim() ? text : null;
};

/**
 * Turns a parsed JSON object into pairs, in the order its keys are written.
 */
const pairsFromJson = (
    source: Record<string, unknown>,
    existingKeys: Record<string, boolean>
): DotKeyValue[] => {
    const pairs: DotKeyValue[] = [];

    for (const [rawKey, rawValue] of Object.entries(source)) {
        const key = rawKey.trim();

        // A key already on screen is skipped rather than overwritten, as below. A blank
        // one has nothing to show in the key column, so it goes too.
        if (!key || existingKeys[key]) {
            continue;
        }

        const value = jsonValueToString(rawValue);

        if (value === null) {
            continue;
        }

        pairs.push({ key, value });
    }

    return pairs;
};

/**
 * Parses pasted text into pairs, in the order the text lists them.
 *
 * Returns an empty array for anything that is neither a JSON object nor a block of
 * assignments, which is how the caller tells a block paste from someone pasting a
 * single key.
 *
 * @param text the pasted text
 * @param existingKeys keys already in the list, which a paste never overwrites
 */
export const parseKeyValueBlock = (
    text: string,
    existingKeys: Record<string, boolean> = {}
): DotKeyValue[] => {
    const json = parseJsonObject(text);

    if (json) {
        return pairsFromJson(json, existingKeys);
    }

    const pairs: DotKeyValue[] = [];
    const seen = new Set<string>();

    for (const line of text.split(/\r?\n/)) {
        const trimmed = line.trim();

        if (!trimmed || trimmed.startsWith('#')) {
            continue;
        }

        const match = ASSIGNMENT.exec(trimmed);

        if (!match) {
            continue;
        }

        const [, key, rawValue] = match;

        // A key already on screen, or repeated within the paste itself. Skipped rather
        // than overwritten — a paste should not quietly replace a value already set.
        if (existingKeys[key] || seen.has(key)) {
            continue;
        }

        const value = unquote(rawValue);

        // `KEY=` is legal in a `.env`, but not here: the entry row refuses a blank
        // value and so does an in-place edit, and a paste is not a way around that.
        // Blank by the same measure they use — trimmed — so a quoted run of spaces
        // does not slip through where a bare one is turned away.
        if (!value.trim()) {
            continue;
        }

        seen.add(key);
        pairs.push({ key, value });
    }

    return pairs;
};
