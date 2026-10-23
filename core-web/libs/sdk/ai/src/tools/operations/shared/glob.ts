import { posix, sep } from 'node:path';

/**
 * The `include` glob filter the asset operations share: `upload_assets` matches it against
 * local paths relative to `src`, `download_assets` against dotCMS paths relative to the folder.
 */

/**
 * Split an `include` string into its comma-separated patterns — but NOT on commas inside a brace
 * group, so `*.{png,webp,jpg}` stays one pattern while `*.vtl,*.scss` is two. Exported for tests.
 */
export function splitIncludePatterns(include?: string): string[] {
    if (!include) {
        return [];
    }
    const patterns: string[] = [];
    let current = '';
    let braceDepth = 0;
    for (const ch of include) {
        if (ch === '{') {
            braceDepth++;
            current += ch;
        } else if (ch === '}') {
            braceDepth = Math.max(0, braceDepth - 1);
            current += ch;
        } else if (ch === ',' && braceDepth === 0) {
            patterns.push(current);
            current = '';
        } else {
            current += ch;
        }
    }
    patterns.push(current);
    return patterns.map((p) => p.trim()).filter(Boolean);
}

/**
 * Build a matcher over relative POSIX paths from a comma-separated `include` string. A file matches
 * if it matches ANY pattern. No `include` → matches everything. Exported for tests.
 *
 * Supports the glob features callers reasonably assume from a standard glob:
 *   - `*`  matches any run of chars WITHIN a path segment (does not cross `/`)
 *   - `**` matches across segments, including zero (so a leading globstar also matches a top-level file)
 *   - `?`  matches a single non-`/` char
 *   - `{png,webp,jpg}` brace expansion (alternation)
 * A pattern with no `/` matches the file's basename anywhere in the tree; a pattern with a `/` is
 * anchored at the root of `src`.
 */
export function includeMatcher(include?: string): (rel: string) => boolean {
    const patterns = splitIncludePatterns(include);

    if (!patterns.length) {
        return () => true;
    }

    // Compile each pattern ONCE here, not per-file — this matcher runs on every asset/file.
    const regexes = patterns.map(globToRegExp);

    return (rel: string) => regexes.some((re) => re.test(rel));
}

/**
 * Compile a single glob pattern to a RegExp with a single left-to-right character scan.
 *
 * A scanner (rather than chained `.replace()` passes) is used deliberately: it has no ordering
 * hazard between `**` and `*`, needs no placeholder sentinels, and each glob token emits its regex
 * exactly once. The old chained-replace version turned every `*` into `[^/]*`, so a `**` + `/*.png`
 * pattern compiled to "exactly one subdirectory" and silently matched nothing for top-level files.
 *
 * Tokens:
 *   - `**` (with an optional adjacent `/`) crosses directory boundaries, matching zero or more
 *     segments, so a leading `**` also matches a top-level file.
 *   - `*` matches any run of chars within one segment (never crosses `/`).
 *   - `?` matches a single non-`/` char.
 *   - `{png,webp,jpg}` expands to alternation `(?:png|webp|jpg)` (nested wildcards are honored).
 * A pattern containing `/` is anchored at the root of `src`; otherwise it matches a basename
 * anywhere in the tree.
 */
function globToRegExp(pattern: string): RegExp {
    const normalized = pattern.split(sep).join(posix.sep);
    const anchored = normalized.includes('/');
    const source = compileGlob(normalized, 0, normalized.length);
    return new RegExp(`${anchored ? '^' : '(^|/)'}${source}$`, 'i');
}

/** Regex-escape a single literal character. */
function escapeRegexChar(ch: string): string {
    return /[|\\{}()[\]^$+.*?]/.test(ch) ? `\\${ch}` : ch;
}

/**
 * Translate the glob in `input[start..end)` to a regex source string. Recurses into brace groups so
 * `{a*,b}` honors the wildcard inside each alternative.
 */
function compileGlob(input: string, start: number, end: number): string {
    let out = '';
    let i = start;

    while (i < end) {
        const ch = input[i];

        if (ch === '*') {
            if (input[i + 1] === '*') {
                // `**` crosses directory boundaries. Consume it plus one adjacent `/` (leading or
                // trailing) and emit an optional "any number of full segments" fragment.
                i += 2;
                const trailing = i >= end;
                if (input[i] === '/') {
                    i++;
                } else if (!trailing && out.endsWith('/')) {
                    out = out.slice(0, -1);
                }

                if (trailing) {
                    // A globstar with nothing after it — `themes/**` — means "everything
                    // below here", so it has to be able to match a final FILENAME segment.
                    // The general fragment below cannot: it only ever ends at a `/`, so
                    // `themes/**` compiled to `^themes(?:.*/)?$` and matched nothing but the
                    // bare string `themes`. Since `dir/**` is the common idiom, users writing
                    // it hit the "matched 0 of N files, check the glob syntax" warning while
                    // their syntax was perfectly reasonable.
                    out += '.*';
                } else {
                    out += '(?:.*/)?';
                }
            } else {
                out += '[^/]*';
                i++;
            }
        } else if (ch === '?') {
            out += '[^/]';
            i++;
        } else if (ch === '{') {
            const close = input.indexOf('}', i);
            if (close === -1 || close >= end) {
                // Unbalanced brace: treat the `{` literally rather than throwing.
                out += '\\{';
                i++;
            } else {
                const alternatives = splitTopLevelCommas(input.slice(i + 1, close)).map((alt) =>
                    compileGlob(alt, 0, alt.length)
                );
                out += `(?:${alternatives.join('|')})`;
                i = close + 1;
            }
        } else {
            out += escapeRegexChar(ch);
            i++;
        }
    }

    return out;
}

/** Split on commas that are NOT inside a nested brace group (for brace-group alternatives). */
function splitTopLevelCommas(group: string): string[] {
    const parts: string[] = [];
    let current = '';
    let depth = 0;
    for (const ch of group) {
        if (ch === '{') {
            depth++;
            current += ch;
        } else if (ch === '}') {
            depth = Math.max(0, depth - 1);
            current += ch;
        } else if (ch === ',' && depth === 0) {
            parts.push(current);
            current = '';
        } else {
            current += ch;
        }
    }
    parts.push(current);
    return parts;
}
