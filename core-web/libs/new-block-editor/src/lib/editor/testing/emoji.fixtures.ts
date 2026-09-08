import { type JSONContent } from '@tiptap/core';

/**
 * Story Block bodies used across the #37340 specs.
 *
 * The defect is a SHAPE defect: the editor replaces a character with a bare `emoji` node, and a
 * bare inline atom inside a marked run ends that run. So every fixture here is the same logical
 * sentence, differing only in the shape of the stored JSON — a control and a broken payload must
 * be indistinguishable once the character is back in the text.
 *
 * Shapes are specified in `specs/37340-emoji-text-node/data-model.md`.
 */

/** The link every fixture uses, unless it is deliberately varying an attribute. */
export const HREF = 'https://dotcms.com';

const link = (attrs: Record<string, unknown> = {}) => ({
    type: 'link',
    attrs: { href: HREF, target: null, rel: null, title: null, ariaLabel: null, ...attrs }
});

const text = (value: string, marks?: JSONContent['marks']): JSONContent =>
    marks ? { type: 'text', marks, text: value } : { type: 'text', text: value };

const emoji = (name = 'copyright', marks?: JSONContent['marks']): JSONContent =>
    marks ? { type: 'emoji', attrs: { name }, marks } : { type: 'emoji', attrs: { name } };

const doc = (...content: JSONContent[]): JSONContent => ({
    type: 'doc',
    content: [{ type: 'paragraph', content }]
});

/**
 * THE REPORTED PAYLOAD (#37340, helpdesk 39197).
 *
 * `text(link) + emoji(bare) + text(link)` with identical link marks. Two `<a>` elements where the
 * author created one, and under VTL the `©` is absent entirely because no consumer outside the
 * editor can resolve a TipTap shortcode.
 *
 * This is the fixture AC-021 heals: the bare node sits between two attribute-identical `link`
 * marks, so the sandwich rule (AC-015) applies and the run rejoins into ONE text node.
 */
export const REPORTED_PAYLOAD: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji(),
    text('All rights reserved', [link()])
);

/**
 * What the reported payload must become — one `text` node carrying one `link` mark.
 *
 * Note there is **no space** around the `©`. The source is `"dotCMS Copyright "` + the node +
 * `"All rights reserved"`, so concatenating gives `"…Copyright ©All rights…"`. The heal does not
 * invent whitespace, and it should not: the author's spacing is whatever they typed around the
 * symbol, and the node itself carried none.
 *
 * This fixture was written with a space and asserted nowhere, so the mismatch went unnoticed —
 * which is precisely why an exported fixture nothing checks is worth deleting or wiring up.
 */
export const REPORTED_PAYLOAD_HEALED: JSONContent = doc(
    text('dotCMS Copyright ©All rights reserved', [link()])
);

/**
 * The control: a document with no `emoji` node anywhere.
 *
 * The heal MUST return this unchanged (AC-019). It is the overwhelmingly common case — nearly
 * every field load in production — so the identity path is the one to assert loudest.
 */
export const NO_EMOJI_CONTROL: JSONContent = doc(
    text('dotCMS Copyright © All rights reserved', [link()])
);

/* ------------------------------------------------------------------------------------------ *
 * Negative cases for the sandwich rule (AC-016).
 *
 * Each one is a shape where a bare `emoji` node has text neighbours but the rule must NOT fire.
 * They are the boundary of the single inference this spec permits, and the reason AC-016 asserts
 * every case separately rather than in a loop that hides which one regressed.
 * ------------------------------------------------------------------------------------------ */

/** Different `href` — two genuinely different links. Never merge across them. */
export const SANDWICH_DIFFERENT_HREF: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji(),
    text('All rights reserved', [link({ href: 'https://dotcms.com/legal' })])
);

/** Same `href`, different `target`. Attribute equality is the whole boundary. */
export const SANDWICH_DIFFERENT_TARGET: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji(),
    text('All rights reserved', [link({ target: '_blank' })])
);

/** Same `href`, different `aria-label` — the accessible names differ, so the links differ. */
export const SANDWICH_DIFFERENT_ARIA_LABEL: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji(),
    text('All rights reserved', [link({ ariaLabel: 'dotCMS legal notice' })])
);

/** A non-`text` sibling breaks the run. `hardBreak` carries no mark to inherit. */
export const SANDWICH_HARD_BREAK_SIBLING: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji(),
    { type: 'hardBreak' },
    text('All rights reserved', [link()])
);

/** Node first in its block — there is no preceding sibling, so there is nothing to match. */
export const SANDWICH_AT_BLOCK_START: JSONContent = doc(
    emoji(),
    text('dotCMS Copyright All rights reserved', [link()])
);

/** Node last in its block — same, on the other side. */
export const SANDWICH_AT_BLOCK_END: JSONContent = doc(
    text('dotCMS Copyright All rights reserved', [link()]),
    emoji()
);

/** A neighbour with no `link` mark at all. */
export const SANDWICH_UNLINKED_NEIGHBOUR: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji(),
    text('All rights reserved')
);

/**
 * TWO symbols side by side inside one link — `©®` typed together.
 *
 * Each node's inner neighbour is the OTHER symbol rather than text, so a rule that looked only at
 * immediate siblings left this alone. It is the same fingerprint as the single-symbol case and
 * arguably more common than the line-break one: anyone who typed two legal marks together inside
 * a link before the fix has exactly this stored.
 */
export const SANDWICH_TWO_EMOJI_SAME_LINK: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji('copyright'),
    emoji('registered'),
    text('All rights reserved', [link()])
);

/** Same run, but the two links differ — the run scan must still refuse to fire. */
export const SANDWICH_TWO_EMOJI_DIFFERENT_LINK: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji('copyright'),
    emoji('registered'),
    text('All rights reserved', [link({ href: 'https://dotcms.com/legal' })])
);

/**
 * A symbol beside an inline image.
 *
 * The image is the run's boundary and is not a `text` node, so the symbol heals to bare text and
 * stays outside the link — a picture next to a symbol says nothing about whether the symbol was
 * part of the link.
 */
export const SANDWICH_IMAGE_SIBLING: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji(),
    {
        type: 'dotImage',
        attrs: { data: { identifier: 'img-37340', title: 'inline image' } }
    },
    text('All rights reserved', [link()])
);

/**
 * A line break INSIDE what would otherwise be a run of symbols.
 *
 * Proves the run scan steps over adjacent symbols only — it must not tunnel through anything
 * else looking for a text node to match.
 */
export const SANDWICH_RUN_BROKEN_BY_HARD_BREAK: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji('copyright'),
    { type: 'hardBreak' },
    emoji('registered'),
    text('All rights reserved', [link()])
);

/**
 * The node already carries its own `link` mark — a link applied OVER an existing node, which the
 * editor supports today.
 *
 * It keeps its own marks under AC-014 and is never re-marked from a neighbour. Healing yields
 * three `text(link)` nodes, which THE HEAL merges into one — ProseMirror does not join them on any
 * load path, measured in research.md R10. Assert on the JSON: the editor's DOM renders one `<a>`
 * either way, which is what hid this for three drafts.
 */
export const NODE_ALREADY_MARKED: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji('copyright', [link()]),
    text('All rights reserved', [link()])
);

/**
 * A `name` that resolves against nothing in the extension's `emojis` table.
 *
 * Unreachable from the shipped table; reachable through hand-crafted JSON via the Contentlet REST
 * API, since no Story Block schema validation exists in `dotCMS/src/main/java`. The heal leaves it
 * untouched (AC-018) — never blank, never a literal `:name:`.
 */
export const UNRESOLVABLE_NAME: JSONContent = doc(
    text('dotCMS Copyright ', [link()]),
    emoji('not_a_real_shortcode_37340'),
    text('All rights reserved', [link()])
);

/**
 * `emoji` nodes nested in every block type that can hold inline content (AC-017).
 *
 * The heal is recursive or it is nothing: authors put symbols in headings and list items far more
 * often than in bare paragraphs.
 */
export const NESTED_BLOCKS: JSONContent = {
    type: 'doc',
    content: [
        { type: 'heading', attrs: { level: 2 }, content: [text('Legal '), emoji()] },
        {
            type: 'bulletList',
            content: [
                {
                    type: 'listItem',
                    content: [{ type: 'paragraph', content: [text('Trademark '), emoji('tm')] }]
                }
            ]
        },
        {
            type: 'blockquote',
            content: [{ type: 'paragraph', content: [text('Registered '), emoji('registered')] }]
        },
        {
            type: 'table',
            content: [
                {
                    type: 'tableRow',
                    content: [
                        {
                            type: 'tableCell',
                            attrs: { colspan: 1, rowspan: 1, colwidth: null },
                            content: [{ type: 'paragraph', content: [emoji()] }]
                        }
                    ]
                }
            ]
        }
    ]
};

/**
 * The AC-002 character sample: representative, not exhaustive (spec Resolved Decision 7).
 *
 * 1907 of the extension's 1949 catalogued characters convert when typed. Testing all of them would
 * be slow and would break on every extension upgrade; testing only `©`/`®`/`™` would test three
 * literals rather than the class. This covers both ends and the awkward middle.
 */
export const REPORTED_SYMBOLS = ['©', '®', '™'] as const;

/** Text-presentation characters — typography an author types as ordinary punctuation. */
export const TEXT_PRESENTATION_SAMPLE = [
    '‼',
    '⁉',
    '✔',
    '✖',
    '✂',
    '✏',
    '⚠',
    'ℹ',
    '♻',
    '▪',
    '↔',
    '↩',
    '⬅',
    '➡',
    '♀',
    '♂',
    '⚖',
    '⚙',
    '☎',
    '✉'
] as const;

/** Pictographic and multi-codepoint cases: a plain emoji, a ZWJ sequence, and a flag. */
export const MULTI_CODEPOINT_SAMPLE = ['🚀', '👩‍💻', '🇨🇷'] as const;

/** Everything AC-002 parameterizes over. */
export const AFFECTED_CHARACTER_SAMPLE = [
    ...REPORTED_SYMBOLS,
    ...TEXT_PRESENTATION_SAMPLE,
    ...MULTI_CODEPOINT_SAMPLE
] as const;
