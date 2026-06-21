import { is as cssIs } from 'css-select';
import { Element } from 'domhandler';
import { parseDocument } from 'htmlparser2';
import postcss from 'postcss';

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
 * State pseudo-classes and pseudo-elements that represent a NON-resting state.
 * A selector containing any of these is EXCLUDED from attribution: the scanner
 * measures contrast in the element's default/resting state, so `a:focus`,
 * `.btn:hover`, `::before` etc. are not the rule that produced the flagged
 * value. (Matching them was the bug — `a:focus` outranked the real resting `a`.)
 */
const STATE_OR_PSEUDO_ELEMENT =
    /::?(hover|focus|active|visited|focus-within|focus-visible|target|before|after|placeholder|selection|first-line|first-letter|link)\b/i;

/** A single color-related CSS rule (one selector, split from comma groups). */
export interface CssRule {
    /** A single selector (comma groups are split so each is tested independently). */
    selector: string;
    /** The color-affecting declarations on this rule. */
    decls: { prop: string; value: string }[];
    /** The postcss Rule node — callers use it for surgical edits + decl position lookup. */
    node: postcss.Rule;
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

        // Split "a, .b .c" into separate selectors so each is tested independently.
        for (const sel of rule.selector.split(',').map((s) => s.trim())) {
            if (sel) {
                rules.push({ selector: sel, decls, node: rule });
            }
        }
    });

    return rules;
}

/**
 * Parse all CSS custom-property definitions (`--name: value`) into a name→value
 * map. Used to resolve `var(--name)` declarations to a concrete color so the
 * attribution's color-equality check can match what axe reports (the *computed*
 * color). Later definitions win (last-wins approximates the cascade for the common
 * single-:root case; we don't model per-selector overrides).
 */
export function parseCustomProperties(css: string): Map<string, string> {
    const root = postcss.parse(css);
    const vars = new Map<string, string>();
    root.walkDecls((d) => {
        if (d.prop.startsWith('--')) {
            vars.set(d.prop.trim(), d.value.trim());
        }
    });
    return vars;
}

/**
 * Resolve a declaration value to a concrete color, expanding `var(--x[, fallback])`
 * against `vars` (recursively, with a depth guard). Returns the input unchanged if
 * there's no var() to expand. A var that resolves to another var is followed; an
 * unknown var falls back to its declared fallback, else the original string.
 */
export function resolveVarValue(value: string, vars: Map<string, string>, depth = 0): string {
    const m = value.match(/^\s*var\(\s*(--[\w-]+)\s*(?:,\s*([^)]+))?\)\s*$/);
    if (!m || depth > 10) {
        return value.trim();
    }
    const [, name, fallback] = m;
    const resolved = vars.get(name);
    if (resolved !== undefined) {
        return resolveVarValue(resolved, vars, depth + 1);
    }
    return fallback !== undefined ? resolveVarValue(fallback, vars, depth + 1) : value.trim();
}

/**
 * The postcss `Declaration` node on `rule` matching `prop`+`value` (a rule may
 * have several color decls, so match the exact one). Callers use `.source.start`
 * for sourcemap resolution. Lives here so postcss knowledge stays in this module.
 */
export function findColorDeclNode(
    rule: postcss.Rule,
    prop: string,
    value: string
): postcss.Declaration | undefined {
    const decls = rule.nodes.filter((n): n is postcss.Declaration => n.type === 'decl');
    return (
        decls.find((d) => d.prop === prop && d.value === value) ??
        decls.find((d) => d.value === value) ??
        decls.find((d) => /color/i.test(d.prop))
    );
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
 * Parse one compound selector (e.g. `a[href="/"]`, `.book-card__genre`) into a
 * synthetic Element carrying just the tag, id, classes, and attributes it asserts.
 * This lets css-select test combinator rules (`.site-nav a`) against a real
 * parent/child chain built from axe's `target` path. Unsupported pieces (pseudos)
 * are ignored — they only narrow, and we've already excluded state pseudos.
 */
function elementFromCompound(compound: string): Element {
    const attribs: Record<string, string> = {};
    let tag = '';

    // tag (leading, optional)
    const tagMatch = compound.match(/^[a-zA-Z][\w-]*/);
    if (tagMatch) {
        tag = tagMatch[0];
    }
    // #id
    const idMatch = compound.match(/#([\w-]+)/);
    if (idMatch) {
        attribs['id'] = idMatch[1];
    }
    // .class (possibly several)
    const classes = [...compound.matchAll(/\.([\w-]+)/g)].map((m) => m[1]);
    if (classes.length) {
        attribs['class'] = classes.join(' ');
    }
    // [attr], [attr=val], [attr$=val] … — record the bare attr so css-select can
    // evaluate the original rule's attribute selector against it.
    for (const am of compound.matchAll(/\[\s*([\w-]+)\s*(?:([~^$*|]?=)\s*"?([^\]"]*)"?\s*)?\]/g)) {
        const [, name, , val] = am;
        attribs[name] = val ?? '';
    }

    return new Element(tag || 'div', attribs);
}

/**
 * Build a parent→…→leaf Element chain from an axe `target` selector
 * (e.g. `.site-nav > a[href="/"]` or `article:nth-child(1) > .book-card__body > .x`).
 * Combinators are flattened to ancestry (we treat `>` and descendant the same —
 * sound enough for attribution: a child IS a descendant). Returns the LEAF element
 * with its `.parent` chain wired, or null if the target is empty.
 */
function elementChainFromTarget(target: string): Element | null {
    const compounds = target
        .split(/\s*[>~+]\s*|\s+/)
        .map((s) => s.trim())
        .filter(Boolean);
    if (!compounds.length) {
        return null;
    }
    let parent: Element | null = null;
    let leaf: Element | null = null;
    for (const compound of compounds) {
        const el = elementFromCompound(compound);
        if (parent) {
            el.parent = parent;
            parent.children = [el];
        }
        parent = el;
        leaf = el;
    }
    return leaf;
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

/** True if the selector targets a non-resting state (so it's not what axe flagged). */
function isStateSelector(selector: string): boolean {
    return STATE_OR_PSEUDO_ELEMENT.test(selector);
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
        // Exclude non-resting-state rules (:hover/:focus/::before …): the scanner
        // measures the resting state, so these are not the flagged rule.
        if (isStateSelector(r.selector)) {
            return false;
        }
        try {
            return cssIs(el, r.selector);
        } catch {
            return false; // unsupported selector syntax
        }
    });

    return matched.sort((a, b) => cmpSpec(specificity(b.selector), specificity(a.selector)));
}

/**
 * Attribute using axe's full `target` selector path (e.g. `.site-nav > a[href="/"]`)
 * instead of just the element HTML. axe already computed the precise DOM path, so
 * we can synthesize the element's ancestor chain and soundly match COMBINATOR
 * rules too (`.site-nav a`, `.book-card__meta > .book-card__genre`) — the cases the
 * HTML-only {@link attribute} must skip. State-pseudo rules are still excluded
 * (the scanner measures the resting state). Ranked by specificity (cascade winner
 * first). Returns [] on an empty/unparseable target so callers can fall back.
 */
export function attributeByTarget(target: string, rules: CssRule[]): CssRule[] {
    const leaf = elementChainFromTarget(target);
    if (!leaf) {
        return [];
    }

    const matched = rules.filter((r) => {
        if (isStateSelector(r.selector)) {
            return false;
        }
        try {
            return cssIs(leaf, r.selector);
        } catch {
            return false; // unsupported selector syntax
        }
    });

    return matched.sort((a, b) => cmpSpec(specificity(b.selector), specificity(a.selector)));
}
