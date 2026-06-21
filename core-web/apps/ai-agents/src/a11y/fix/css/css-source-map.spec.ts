import { SourceMapGenerator } from 'source-map';

import {
    extractInlineSourceMap,
    resolveDeclarationValue,
    resolveSource,
    type SourceMapJson
} from './css-source-map';

/**
 * Build a valid v3 sourcemap fixture with known mappings using the `source-map`
 * library (already a dependency). Generating it — rather than hand-rolling VLQ —
 * keeps the mappings provably correct so the assertions test OUR code, not the
 * fixture.
 *
 * Two compiled positions map to two different sources at known columns:
 *   generated (1,0)  → _mixins.scss      (5,2)   ← the declaration/prop site
 *   generated (1,18) → _variables.scss   (3,9)   ← the $variable VALUE site
 * Column 18 is the value column for `background-color:` (prop len 16 + ': ' = 18).
 */
function buildFixture(opts: { withContent?: boolean } = {}): SourceMapJson {
    const gen = new SourceMapGenerator({ file: 'styles.css' });

    // Property column (0) → the declaration site in the mixin.
    gen.addMapping({
        generated: { line: 1, column: 0 },
        original: { line: 5, column: 2 },
        source: '_mixins.scss'
    });

    // Value column (18) → the $variable definition in the variables partial.
    gen.addMapping({
        generated: { line: 1, column: 18 },
        original: { line: 3, column: 9 },
        source: '_variables.scss',
        name: 'primary'
    });

    if (opts.withContent) {
        gen.setSourceContent('_mixins.scss', '@mixin btn {\n  color: red;\n}\n');
        gen.setSourceContent('_variables.scss', '// tokens\n\n$primary: #e76300;\n');
    }

    return JSON.parse(gen.toString()) as SourceMapJson;
}

/** Wrap a sourcemap JSON object into an inline URL-encoded data-URI CSS comment. */
function toInlineCss(body: string, map: SourceMapJson): string {
    const encoded = encodeURIComponent(JSON.stringify(map));
    return `${body}\n/*# sourceMappingURL=data:application/json;charset=utf-8,${encoded} */`;
}

describe('css-source-map', () => {
    describe('extractInlineSourceMap', () => {
        it('parses a valid inline (URL-encoded) map', () => {
            const map = buildFixture();
            const css = toInlineCss('.btn{color:#fff}', map);

            const result = extractInlineSourceMap(css);

            expect(result).not.toBeNull();
            expect(result?.version).toBe(3);
            expect(result?.sources).toEqual(map.sources);
            expect(result?.mappings).toBe(map.mappings);
        });

        it('returns null when no inline map is present', () => {
            expect(extractInlineSourceMap('.btn{color:#fff}')).toBeNull();
            expect(extractInlineSourceMap('')).toBeNull();
        });

        it('does NOT break when the encoded payload contains a "*" character', () => {
            // Regression for the gotcha: a `*` inside the payload defeats a
            // non-greedy `[^*]+?` regex. `encodeURIComponent` leaves `*` LITERAL,
            // so a `*` inside a source path rides into the comment body verbatim —
            // the exact thing that breaks a naive regex. Cover both a literal `*`
            // and the `%2A` encoded form (decodeURIComponent restores it to `*`).
            const map = buildFixture();
            map.sources = ['a*b.scss', '_variables.scss'];

            const encoded = encodeURIComponent(JSON.stringify(map)).replace(
                'b.scss',
                'b%2A.scss' // an encoded `*` too, so the payload has both forms
            );
            expect(encoded).toContain('*'); // a literal `*` is in the comment body
            expect(encoded).toContain('%2A'); // and an encoded `*` is in there too
            const css = `.btn{color:#fff}\n/*# sourceMappingURL=data:application/json;charset=utf-8,${encoded} */`;

            const result = extractInlineSourceMap(css);

            expect(result).not.toBeNull();
            expect(result?.sources).toContain('a*b*.scss');
        });

        it('returns null on a malformed payload (not valid JSON after decode)', () => {
            const css =
                '.btn{color:#fff}\n/*# sourceMappingURL=data:application/json;charset=utf-8,not%20json%20at%20all */';

            expect(extractInlineSourceMap(css)).toBeNull();
        });
    });

    describe('resolveSource', () => {
        it('returns the right source/line for a known mapping', async () => {
            const map = buildFixture();

            const result = await resolveSource(map, 1, 0);

            expect(result).toEqual({
                source: '_mixins.scss',
                line: 5,
                column: 2
            });
        });

        it('resolves the value column to the variable source', async () => {
            const map = buildFixture();

            const result = await resolveSource(map, 1, 18);

            expect(result?.source).toBe('_variables.scss');
            expect(result?.line).toBe(3);
            expect(result?.column).toBe(9);
        });

        it('returns null when the position maps to no source', async () => {
            const map = buildFixture();

            // A line with no mappings at all.
            const result = await resolveSource(map, 99, 0);

            expect(result).toBeNull();
        });

        it('populates content from sourcesContent when present', async () => {
            const map = buildFixture({ withContent: true });

            const result = await resolveSource(map, 1, 18);

            expect(result?.source).toBe('_variables.scss');
            expect(result?.content).toContain('$primary: #e76300;');
        });

        it('omits content when sourcesContent is absent', async () => {
            const map = buildFixture();

            const result = await resolveSource(map, 1, 18);

            expect(result?.content).toBeUndefined();
        });
    });

    describe('resolveDeclarationValue', () => {
        it('offsets the column by prop.length + 2 to land on the value source', async () => {
            const map = buildFixture();

            // declColumn 0, prop 'background-color' (16) + 2 = column 18, which
            // maps to _variables.scss — a DIFFERENT source than column 0 (_mixins).
            const result = await resolveDeclarationValue(map, 1, 0, 'background-color');

            expect(result?.source).toBe('_variables.scss');
            expect(result?.line).toBe(3);
            expect(result?.column).toBe(9);
        });

        it('lands on the declaration site source when the offset is for the prop column', async () => {
            const map = buildFixture();

            // Sanity: the property column (0) resolves to _mixins.scss, proving the
            // offset above is what reaches the variable.
            const propSite = await resolveSource(map, 1, 0);

            expect(propSite?.source).toBe('_mixins.scss');
        });
    });
});
