import { DotKeyValue } from './dot-key-value-ng.component';

/**
 * Reads a block of `KEY=VALUE` lines — the shape of a `.env` file — into pairs.
 *
 * Deliberately a small subset of dotenv. It covers what someone actually copies out
 * of a `.env`: comments, blank lines, `export` prefixes and quoted values. It does
 * **not** cover multi-line values, variable interpolation (`${OTHER}`) or escape
 * sequences — a certificate pasted here would come out truncated at its first line
 * break, so those stay a single-pair job through the normal inputs.
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
 * Parses pasted text into pairs, in the order the text lists them.
 *
 * Returns an empty array for anything that does not look like assignments, which is
 * how the caller tells a block paste from someone pasting a single key.
 *
 * @param text the pasted text
 * @param existingKeys keys already in the list, which a paste never overwrites
 */
export const parseKeyValueBlock = (
    text: string,
    existingKeys: Record<string, boolean> = {}
): DotKeyValue[] => {
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
