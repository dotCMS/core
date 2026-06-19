import { is as cssIs } from 'css-select';
import { parseDocument } from 'htmlparser2';
import postcss from 'postcss';

import type { Element } from 'domhandler';

/**
 * Deterministic CSS attribution for `color-contrast` violations — NO LLM.
 *
 * Given an axe finding's offending element (its `context` HTML) and a stylesheet's
 * CSS text, find the CSS rule(s) that govern that element's color. The agent then
 * sends the model only the matched rule(s) (~100 tokens) instead of the whole
 * theme (~34K tokens for the demo `travel` theme), and applies the corrected
 * value surgically to the postcss AST node (kept on each {@link CssRule}).
 *
 * Pipeline (validated in scratch/SPIKE-css-attribution.md):
 *   postcss      → parse CSS into rules (selector + color declarations)
 *   htmlparser2  → build a DOM node from the violation's context HTML
 *   css-select   → which rule selectors actually MATCH this element?
 *   specificity  → rank; the cascade winner is the rule to edit
 *
 * SOUND MATCHING: the finding gives the element, NOT its ancestors — so we can
 * only verify selectors that target this element directly (pure compound
 * selectors). Descendant/child/sibling combinator selectors are excluded; a
 * naive "match the rightmost compound" approach false-positives every `... a`
 * rule against any `<a>` (the spike proved this wrong).
 */

/** Color-affecting properties that determine contrast. */
const COLOR_PROPS = new Set(['color', 'background', 'background-color', 'border-color', 'fill']);

/**
 * Dynamic pseudo-classes and pseudo-elements a lone element can't satisfy.
 * Stripped before the match test, but KEPT in the returned selector string.
 */
const DYNAMIC_PSEUDOS =
    /::?(hover|focus|active|visited|focus-within|focus-visible|before|after)\b(\([^)]*\))?/g;

/** A single color-related CSS rule (one selector, split from comma groups). */
export interface CssRule {
    /** A single selector (comma groups are split so each is tested independently). */
    selector: string;
    /** The color-affecting declarations on this rule. */
    decls: { prop: string; value: string }[];
    /** The postcss Rule node — callers need it for surgical edits + position lookup. */
    node: postcss.Rule;
    /** Source start position (1-based), for sourcemap resolution. */
    pos?: { line: number; column: number };
}

/**
 * Parse a stylesheet into individual `(selector, color-decls)` rules.
 *
 * Keeps only declarations whose (lowercased) property is in {@link COLOR_PROPS};
 * rules with no color declarations are skipped. Comma-grouped selectors are split
 * so each selector is tested independently.
 */
export function parseColorRules(css: string): CssRule[] {
    const root = postcss.parse(css);
    const rules: CssRule[] = [];

    root.walkRules((rule) => {
        const decls = rule.nodes
            .filter((n): n is postcss.Declaration => n.type === 'decl')
            .filter((d) => COLOR_PROPS.has(d.prop.toLowerCase()))
            .map((d) => ({ prop: d.prop, value: d.value }));

        if (decls.length === 0) {
            return;
        }

        const start = rule.source?.start;
        const pos = start ? { line: start.line, column: start.column } : undefined;

        // Split "a, .b .c" into separate selectors so each is tested independently.
        for (const sel of rule.selector.split(',').map((s) => s.trim())) {
            if (sel) {
                rules.push({ selector: sel, decls, node: rule, pos });
            }
        }
    });

    return rules;
}

/** Rough specificity `(ids, classes/attrs/pseudo-classes, types)`. */
export function specificity(selector: string): [number, number, number] {
    const ids = (selector.match(/#[\w-]+/g) || []).length;
    const classes = (selector.match(/\.[\w-]+|\[[^\]]+\]|:[\w-]+(?!:)/g) || []).length;
    const types = (selector.match(/(^|[\s>+~])[a-zA-Z][\w-]*/g) || []).length;
    return [ids, classes, types];
}

/** Compare two specificity tuples (`a - b`; positive ⇒ `a` wins). */
function cmpSpec(a: [number, number, number], b: [number, number, number]): number {
    return a[0] - b[0] || a[1] - b[1] || a[2] - b[2];
}

/** The first element node in the parsed context HTML. */
function elementFromHtml(html: string): Element {
    const doc = parseDocument(html);
    const el = doc.children.find((n) => n.type === 'tag') as Element | undefined;
    if (!el) {
        throw new Error(`No element in context HTML: ${html}`);
    }
    return el;
}

/**
 * True if the selector has a descendant/child/sibling combinator (so it needs
 * ancestor context we don't have). Attribute selectors are stripped first so
 * their contents don't trip the whitespace check.
 */
function hasCombinator(selector: string): boolean {
    const stripped = selector.replace(/\[[^\]]*\]/g, '');
    return /[\s>+~]/.test(stripped.trim());
}

/**
 * Normalize for matching: drop dynamic pseudos a lone element can't satisfy
 * (`:hover/:focus/:active/::before` …), keeping the rest of the structure.
 */
function matchable(selector: string): string {
    return selector.replace(DYNAMIC_PSEUDOS, '').trim();
}

/**
 * Find the color rules that style `elementHtml`'s element, ranked by specificity
 * descending (the cascade winner first).
 *
 * Only pure compound selectors are matched (combinator selectors are excluded —
 * see the module note on SOUND MATCHING). Selectors that throw under css-select
 * (unsupported syntax) are skipped.
 */
export function attribute(elementHtml: string, rules: CssRule[]): CssRule[] {
    const el = elementFromHtml(elementHtml);

    const matched = rules.filter((r) => {
        // Sound matching only: we have the element, not its ancestors, so we can
        // only verify selectors that target THIS element directly — pure compound
        // selectors. Excluding combinator selectors trades recall for precision
        // (no false positives like ".x .y a" matching any <a>).
        if (hasCombinator(r.selector)) {
            return false;
        }
        const sel = matchable(r.selector);
        if (!sel) {
            return false; // was purely a pseudo (e.g. "::before") — skip
        }
        try {
            return cssIs(el, sel);
        } catch {
            return false; // unsupported selector syntax
        }
    });

    return matched.sort((a, b) => cmpSpec(specificity(b.selector), specificity(a.selector)));
}
