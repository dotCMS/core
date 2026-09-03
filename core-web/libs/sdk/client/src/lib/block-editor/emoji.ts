import type { BlockEditorNode } from '@dotcms/types';

import { EMOJI_MAP } from './emoji-map';

/** Kept per render pass so a document repeating a bad name warns once, not once per node. */
export type EmojiWarnScope = Set<string>;

/**
 * Resolves an `emoji` node to the text a renderer should output (#37340).
 *
 * Stored `emoji` nodes carry only a shortcode, so without this table the character is simply
 * lost — which is exactly what the VTL renderer used to do.
 *
 * Precedence, per `specs/37340-emoji-text-node/contracts/renderer.contract.md` C1:
 *
 *   1. the generated map
 *   2. the node's own `text`, if it carries one
 *   3. the literal `:name:`
 *
 * It never returns an empty string: silently dropping content is the original defect.
 *
 * `attrs.name` arrives from the Contentlet REST API, which applies no Story Block schema
 * validation, so it is user-controlled. Callers MUST escape the result — every framework
 * renderer here interpolates it as text, which escapes by default.
 */
export const resolveEmoji = (node: BlockEditorNode, warned?: EmojiWarnScope): string => {
    const name = typeof node.attrs?.['name'] === 'string' ? node.attrs['name'] : '';

    if (name && EMOJI_MAP[name]) {
        return EMOJI_MAP[name];
    }

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
