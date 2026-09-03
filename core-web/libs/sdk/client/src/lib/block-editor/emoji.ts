import type { BlockEditorNode } from '@dotcms/types';

/** Kept per render pass so a document repeating a bad name warns once, not once per node. */
export type EmojiWarnScope = Set<string>;

/**
 * Resolves a legacy `emoji` node to the text a renderer should output (#37340).
 *
 * The Block Editor no longer creates `emoji` nodes — a typed emoji stays a character in the
 * surrounding text node — so this only ever runs against content saved before that fix.
 *
 * Those nodes store a TipTap shortcode (`{"attrs":{"name":"copyright"}}`), never the character
 * and never a codepoint, and a shortcode is not an HTML entity: `:copyright:` is not `&copy;`,
 * and most emoji have no entity at all, so a browser cannot resolve it either. Turning the name
 * back into a character would need a ~1900-entry lookup table shipped to three published SDKs
 * and the Java classpath — deliberately not carried, since nothing creates these nodes any more
 * and the affected content is a single reported case.
 *
 * So: emit the node's own text when it carries one, otherwise the literal `:name:`. Never empty
 * — the original defect was the character silently disappearing, and a visible `:copyright:` is
 * what tells an author which content to re-enter.
 *
 * `attrs.name` arrives from the Contentlet REST API, which applies no Story Block schema
 * validation, so it is user-controlled. Callers MUST escape the result; every framework renderer
 * here interpolates it as text, which escapes by default.
 */
export const resolveEmoji = (node: BlockEditorNode, warned?: EmojiWarnScope): string => {
    const name = typeof node.attrs?.['name'] === 'string' ? node.attrs['name'] : '';

    // `emoji` is an atom, so a literal character rides in `attrs.text`; `node.text` is only
    // populated on real text nodes. Accept either so a hand-authored payload still resolves.
    const carried = typeof node.attrs?.['text'] === 'string' ? node.attrs['text'] : node.text;

    if (typeof carried === 'string' && carried.length > 0) {
        return carried;
    }

    if (!warned?.has(name)) {
        warned?.add(name);
        console.warn(`[dotCMS Block Editor]: Emoji ${name} is not supported`);
    }

    return `:${name}:`;
};
