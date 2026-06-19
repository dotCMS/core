import { attribute, parseColorRules, specificity } from './css-attribution';

describe('css-attribution — deterministic color-rule attribution (no LLM)', () => {
    describe('parseColorRules', () => {
        it('extracts color declarations and splits comma-grouped selectors', () => {
            const css = `a, .b { color: #333; background-color: #fff; }`;
            const rules = parseColorRules(css);

            // "a, .b" → two entries, each carrying both color decls.
            expect(rules.map((r) => r.selector)).toEqual(['a', '.b']);
            expect(rules[0].decls).toEqual([
                { prop: 'color', value: '#333' },
                { prop: 'background-color', value: '#fff' }
            ]);
            // The same postcss node is shared by both split selectors.
            expect(rules[0].node).toBe(rules[1].node);
            expect(rules[0].pos).toEqual({ line: 1, column: 1 });
        });

        it('keeps only color-affecting properties', () => {
            const css = `.x { color: red; font-size: 12px; fill: blue; margin: 0; }`;
            const [rule] = parseColorRules(css);

            expect(rule.decls).toEqual([
                { prop: 'color', value: 'red' },
                { prop: 'fill', value: 'blue' }
            ]);
        });

        it('ignores rules that have no color properties', () => {
            const css = `.layout { display: flex; padding: 1rem; } .y { color: #000; }`;
            const rules = parseColorRules(css);

            expect(rules.map((r) => r.selector)).toEqual(['.y']);
        });

        it('matches color properties case-insensitively', () => {
            const css = `.z { COLOR: #111; Background-Color: #eee; }`;
            const [rule] = parseColorRules(css);

            expect(rule.decls.map((d) => d.prop.toLowerCase())).toEqual([
                'color',
                'background-color'
            ]);
        });
    });

    describe('attribute', () => {
        const html = '<a class="button button-primary" id="book" href="#">Book now</a>';

        it('finds .button-primary and ranks it above a generic a {}', () => {
            const css = `
                a { color: #e76300; }
                .button-primary { color: #fff; background-color: #e76300; }
            `;
            const rules = parseColorRules(css);
            const matched = attribute(html, rules);

            expect(matched.map((r) => r.selector)).toEqual(['.button-primary', 'a']);
            // The specific rule (more classes) ranks first.
            expect(specificity(matched[0].selector)).toEqual([0, 1, 0]);
            expect(specificity(matched[1].selector)).toEqual([0, 0, 1]);
        });

        it('SOUND GUARD: a descendant selector must NOT match a lone element', () => {
            const css = `
                .post-classic .post-title a { color: red; }
                .button-primary { color: #fff; }
            `;
            const rules = parseColorRules(css);
            const matched = attribute(html, rules);

            // Only the pure compound selector survives — the descendant selector
            // is excluded because we can't verify the ancestor context.
            expect(matched.map((r) => r.selector)).toEqual(['.button-primary']);
            expect(matched.map((r) => r.selector)).not.toContain('.post-classic .post-title a');
        });

        it('excludes child and sibling combinator selectors too', () => {
            const css = `
                div > a { color: green; }
                .x + a { color: orange; }
                .x ~ a { color: purple; }
                .button-primary { color: #fff; }
            `;
            const matched = attribute(html, parseColorRules(css));

            expect(matched.map((r) => r.selector)).toEqual(['.button-primary']);
        });

        it('strips dynamic pseudos for matching but keeps them in the selector', () => {
            const css = `.button-primary:focus { color: #fff; }`;
            const matched = attribute('<a class="button-primary">x</a>', parseColorRules(css));

            expect(matched).toHaveLength(1);
            // :focus stripped only for the match test — the returned selector is intact.
            expect(matched[0].selector).toBe('.button-primary:focus');
        });

        it('skips selectors that are purely a pseudo-element', () => {
            const css = `::before { color: #fff; } .button-primary { color: #000; }`;
            const matched = attribute('<a class="button-primary">x</a>', parseColorRules(css));

            expect(matched.map((r) => r.selector)).toEqual(['.button-primary']);
        });

        it('does not crash on an unparseable / unsupported selector', () => {
            const css = `:::weird::: { color: red; } .button-primary { color: #000; }`;
            const matched = attribute(
                '<a class="button-primary">x</a>',
                parseColorRules(css)
            );

            // The weird selector is silently skipped; the valid one still matches.
            expect(matched.map((r) => r.selector)).toEqual(['.button-primary']);
        });

        it('matches by id and attribute selectors', () => {
            const css = `#book { color: #123; } a[href] { color: #456; }`;
            const matched = attribute(html, parseColorRules(css));

            // #book (id) ranks above a[href] (attr + type).
            expect(matched.map((r) => r.selector)).toEqual(['#book', 'a[href]']);
        });

        it('throws a clear error when the context HTML has no element', () => {
            expect(() => attribute('just text, no tag', [])).toThrow(/No element in context HTML/);
        });
    });

    describe('specificity', () => {
        it('counts ids, classes/attrs/pseudo-classes, and types', () => {
            expect(specificity('a')).toEqual([0, 0, 1]);
            expect(specificity('.btn')).toEqual([0, 1, 0]);
            expect(specificity('#book')).toEqual([1, 0, 0]);
            expect(specificity('a.btn[href]:focus')).toEqual([0, 3, 1]);
        });
    });
});
