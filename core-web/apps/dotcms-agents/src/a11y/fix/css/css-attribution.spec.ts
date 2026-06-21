import {
    attribute,
    attributeByTarget,
    parseColorRules,
    parseCustomProperties,
    resolveVarValue,
    specificity
} from './css-attribution';

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

        it('EXCLUDES state-pseudo rules (the scanner measures the resting state)', () => {
            // :focus/:hover/:active are not the resting state axe flagged — they must
            // NOT be attributed (this was the bug: a:focus outranked the resting a).
            const css = `
                a:focus { color: #fff; }
                a:hover { color: #111; }
                a { color: #e76300; }
            `;
            const matched = attribute('<a class="btn">x</a>', parseColorRules(css));
            expect(matched.map((r) => r.selector)).toEqual(['a']); // only the resting rule
        });

        it('skips pseudo-element selectors (::before/::after)', () => {
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

    describe('attributeByTarget — uses axe’s full target path (combinator-aware)', () => {
        const css = `
            .site-nav a { color: #b3ab9e; }
            .book-card__meta > .book-card__genre { color: #e88c42; }
            a { color: #333; }
            .site-nav a:hover { color: #000; }
        `;
        const rules = parseColorRules(css);

        it('matches a descendant-combinator rule via the target path', () => {
            // The element-HTML attribute() would skip `.site-nav a` (combinator).
            const matched = attributeByTarget('.site-nav > a[href="/"]', rules);
            expect(matched.map((r) => r.selector)).toContain('.site-nav a');
        });

        it('matches a child-combinator rule and ranks by specificity', () => {
            const matched = attributeByTarget(
                'article:nth-child(1) > .book-card__body > .book-card__meta > .book-card__genre',
                rules
            );
            expect(matched[0].selector).toBe('.book-card__meta > .book-card__genre');
        });

        it('still excludes :hover state rules', () => {
            const matched = attributeByTarget('.site-nav > a[href="/"]', rules);
            expect(matched.map((r) => r.selector)).not.toContain('.site-nav a:hover');
        });

        it('returns [] for an empty target (caller falls back to element HTML)', () => {
            expect(attributeByTarget('', rules)).toEqual([]);
        });
    });

    describe('parseCustomProperties + resolveVarValue', () => {
        const css = `
            :root {
                --color-orange: #E47B27;
                --color-orange-light: #E88C42;
                --brand: var(--color-orange);
            }
        `;
        const vars = parseCustomProperties(css);

        it('parses custom-property definitions into a name→value map', () => {
            expect(vars.get('--color-orange')).toBe('#E47B27');
            expect(vars.get('--color-orange-light')).toBe('#E88C42');
        });

        it('resolves var(--x) to its concrete color', () => {
            expect(resolveVarValue('var(--color-orange-light)', vars)).toBe('#E88C42');
        });

        it('follows var → var indirection', () => {
            expect(resolveVarValue('var(--brand)', vars)).toBe('#E47B27');
        });

        it('uses the fallback when the var is undefined', () => {
            expect(resolveVarValue('var(--missing, #123456)', vars)).toBe('#123456');
        });

        it('returns a plain color unchanged', () => {
            expect(resolveVarValue('#abcdef', vars)).toBe('#abcdef');
        });
    });
});
