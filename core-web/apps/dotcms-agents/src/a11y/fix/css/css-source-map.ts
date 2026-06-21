import { type RawSourceMap, SourceMapConsumer } from 'source-map';

/**
 * Compiled-CSS → SCSS source resolution for the a11y agent (plan §sass-sourcemap).
 *
 * dotCMS serves compiled CSS from SCSS at `…/styles.dotsass?sourcemap=true` with
 * an INLINE v3 sourcemap appended to the body as a comment:
 *   `/*# sourceMappingURL=data:application/json;charset=utf-8,<URL-ENCODED-JSON> *\/`
 * The payload is URL-encoded JSON (NOT base64), carrying theme-relative `sources[]`,
 * the source text inline in `sourcesContent[]`, and COLUMN-level `mappings`.
 *
 * That column granularity is the whole point: a compiled color VALUE maps back to
 * the exact `$variable` definition that produced it — so the agent edits the right
 * `.scss` (and the right token), not the regenerated artifact. Validated live on the
 * demo `travel` theme: `.button-primary{background-color:#e76300}` → value column →
 * `custom-styles/_variables-custom.scss:64` (`$primary: #E76300`).
 *
 * Pure functions over strings (+ the `source-map` lib). No network.
 */

/** A parsed v3 inline sourcemap. */
export type SourceMapJson = {
    version: number;
    sources: string[];
    sourcesContent?: string[];
    mappings: string;
    names?: string[];
    sourceRoot?: string;
};

/** An original source position resolved from a generated (compiled CSS) position. */
export type ResolvedSource = {
    source: string;
    line: number;
    column: number;
    /** The matching `sourcesContent[]` entry (looked up by `source` index), when present. */
    content?: string;
};

/**
 * Extract and parse the inline sourcemap appended to compiled CSS, or `null`.
 *
 * EXTRACTION GOTCHA: the URL-encoded payload can itself contain `*` (e.g. `%2A` or a
 * literal `*` inside a string), so a non-greedy `/[^*]+?/` regex breaks on the first
 * one. The robust approach delimits by absolute positions instead:
 *   1. find `sourceMappingURL=data:application/json` (the comment marker),
 *   2. the data-URI payload starts after the FIRST `,` past that marker,
 *   3. the comment closes at the file's LAST `*\/`,
 *   4. `decodeURIComponent` + `JSON.parse` the slice between.
 */
export function extractInlineSourceMap(css: string): SourceMapJson | null {
    const i = css.indexOf('sourceMappingURL=data:application/json');
    if (i === -1) {
        return null;
    }

    const comma = css.indexOf(',', i);
    if (comma === -1) {
        return null;
    }

    const end = css.lastIndexOf('*/');
    if (end === -1 || end <= comma) {
        return null;
    }

    const payload = css.slice(comma + 1, end).trim();

    try {
        return JSON.parse(decodeURIComponent(payload)) as SourceMapJson;
    } catch {
        return null;
    }
}

/**
 * Resolve a generated (compiled CSS) position back to its original SCSS source.
 *
 * Returns `null` when the position maps to no source. `content` is populated from
 * `map.sourcesContent` (by the index of `source` in `map.sources`) when present.
 */
export async function resolveSource(
    map: SourceMapJson,
    line: number,
    column: number
): Promise<ResolvedSource | null> {
    // `SourceMapJson` is the minimal contract the dotCMS endpoint guarantees; the
    // lib's `RawSourceMap` additionally types `file` as required, which the served
    // map need not carry. The cast bridges the two — the consumer tolerates it.
    const consumer = await new SourceMapConsumer(map as unknown as RawSourceMap);
    try {
        const pos = consumer.originalPositionFor({ line, column });
        if (pos.source == null || pos.line == null || pos.column == null) {
            return null;
        }

        const resolved: ResolvedSource = {
            source: pos.source,
            line: pos.line,
            column: pos.column
        };

        const idx = map.sources.indexOf(pos.source);
        const content = idx === -1 ? undefined : map.sourcesContent?.[idx];
        if (content != null) {
            resolved.content = content;
        }

        return resolved;
    } finally {
        consumer.destroy();
    }
}

/**
 * Resolve a CSS declaration's VALUE position (not the property) back to source.
 *
 * Given a declaration `prop: value`, the value starts `prop.length + 2` columns
 * past the declaration start (the `: ` separator). Landing on the value column is
 * what reaches the `$variable` definition when the value came from a variable —
 * the property column would only reach the declaration site.
 */
export async function resolveDeclarationValue(
    map: SourceMapJson,
    declLine: number,
    declColumn: number,
    prop: string
): Promise<ResolvedSource | null> {
    const valueColumn = declColumn + prop.length + 2;
    return resolveSource(map, declLine, valueColumn);
}
