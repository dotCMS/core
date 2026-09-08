import { type JSONContent } from '@tiptap/core';
import { emojis } from '@tiptap/extension-emoji';

import { healEmojiNodes } from './emoji-heal.utils';

import {
    NESTED_BLOCKS,
    NO_EMOJI_CONTROL,
    NODE_ALREADY_MARKED,
    REPORTED_PAYLOAD,
    REPORTED_PAYLOAD_HEALED,
    SANDWICH_AT_BLOCK_END,
    SANDWICH_AT_BLOCK_START,
    SANDWICH_DIFFERENT_ARIA_LABEL,
    SANDWICH_DIFFERENT_HREF,
    SANDWICH_DIFFERENT_TARGET,
    MALFORMED_MARKS,
    SANDWICH_DIFFERENT_REL,
    SANDWICH_DIFFERENT_TITLE,
    SANDWICH_HARD_BREAK_SIBLING,
    SANDWICH_MARKED_EMOJI_BOUNDARY,
    SANDWICH_TWO_MARKS_BOTH_SIDES,
    SANDWICH_TWO_MARKS_ONE_SIDE,
    SANDWICH_UNRESOLVABLE_BOUNDARY,
    SANDWICH_IMAGE_SIBLING,
    SANDWICH_RUN_BROKEN_BY_HARD_BREAK,
    SANDWICH_TWO_EMOJI_DIFFERENT_LINK,
    SANDWICH_TWO_EMOJI_SAME_LINK,
    SANDWICH_UNLINKED_NEIGHBOUR,
    UNRESOLVABLE_NAME
} from '../testing/emoji.fixtures';

/**
 * #37340 — the heal, as a pure JSON transform.
 *
 * `emoji(marks) → text(same marks, character)`, plus one narrow inference (the link sandwich) and
 * a merge pass. No editor instance: this is JSON in, JSON out, which is exactly the level the
 * defect and the fix live at.
 *
 * **Assert on the JSON, never the DOM.** The editor renders a mark run as a single element however
 * many nodes span it, which is how the original "ProseMirror joins them" assumption survived three
 * drafts (research.md R10). The stored document is what VTL and the SDKs consume.
 */
describe('healEmojiNodes — #37340', () => {
    const inline = (doc: JSONContent, blockIndex = 0): JSONContent[] =>
        ((doc.content?.[blockIndex] as JSONContent)?.content ?? []) as JSONContent[];

    const types = (doc: JSONContent): string[] => {
        const found: string[] = [];
        const walk = (node: JSONContent) => {
            if (node.type) found.push(node.type);
            (node.content ?? []).forEach(walk);
        };
        walk(doc);

        return found;
    };

    const markTypes = (node: JSONContent): string[] => (node.marks ?? []).map((m) => m.type);

    describe('AC-013 — every emoji node becomes text', () => {
        it('leaves no emoji node in the document', () => {
            expect(types(healEmojiNodes(REPORTED_PAYLOAD, emojis))).not.toContain('emoji');
        });

        it('resolves attrs.name to the character', () => {
            const healed = healEmojiNodes(REPORTED_PAYLOAD, emojis);
            const text = inline(healed)
                .map((n) => n.text ?? '')
                .join('');

            expect(text).toContain('©');
        });

        it('does not mutate the input', () => {
            const before = JSON.stringify(REPORTED_PAYLOAD);
            healEmojiNodes(REPORTED_PAYLOAD, emojis);

            expect(JSON.stringify(REPORTED_PAYLOAD)).toBe(before);
        });
    });

    describe('AC-014 — marks preserved, never inherited', () => {
        /**
         * The criterion that keeps this a shape change rather than an edit. A transform that
         * guessed at marks could alter documents that never had this defect — silently, and
         * irreversibly.
         */
        it('a bare node yields bare text when the sandwich does not apply', () => {
            const healed = healEmojiNodes(SANDWICH_DIFFERENT_HREF, emojis);
            const holder = inline(healed).find((n) => (n.text ?? '').includes('©'));

            expect(holder).toBeDefined();
            expect(markTypes(holder as JSONContent)).toEqual([]);
        });

        it('a node carrying its own link mark keeps that mark', () => {
            const healed = healEmojiNodes(NODE_ALREADY_MARKED, emojis);

            expect(types(healed)).not.toContain('emoji');
            expect(inline(healed).every((n) => markTypes(n).includes('link'))).toBe(true);
        });
    });

    describe('AC-015 — the link sandwich', () => {
        it('a bare node between attribute-identical link marks inherits that mark', () => {
            const healed = healEmojiNodes(REPORTED_PAYLOAD, emojis);

            expect(inline(healed).every((n) => markTypes(n).includes('link'))).toBe(true);
        });

        it('a RUN of symbols between identical links joins that link', () => {
            // `©®` typed together inside one link. Each node's inner neighbour is the other
            // symbol, so an immediate-siblings-only rule left this shape behind.
            const healed = healEmojiNodes(SANDWICH_TWO_EMOJI_SAME_LINK, emojis);
            const nodes = inline(healed);

            expect(types(healed)).not.toContain('emoji');
            expect(nodes).toHaveLength(1);
            expect(nodes[0].text).toBe('dotCMS Copyright ©®All rights reserved');
            expect(markTypes(nodes[0])).toEqual(['link']);
        });

        it('AC-021 — the reported payload becomes exactly ONE text node with the link mark', () => {
            const healed = healEmojiNodes(REPORTED_PAYLOAD, emojis);
            const nodes = inline(healed);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('text');
            expect(nodes[0].text).toBe('dotCMS Copyright ©All rights reserved');
            expect(markTypes(nodes[0])).toEqual(['link']);
        });

        /**
         * Whole-document equality, not field-by-field.
         *
         * The assertions above pass even if some other part of the document changed — a different
         * paragraph, a stray attr, a mark on a node nobody looked at. This one cannot.
         */
        it('AC-021 — the healed document equals the expected document exactly', () => {
            expect(healEmojiNodes(REPORTED_PAYLOAD, emojis)).toEqual(REPORTED_PAYLOAD_HEALED);
        });
    });

    describe('AC-016 — where the sandwich rule must NOT fire', () => {
        /**
         * Each case is asserted on its own rather than in a loop, so a regression names the shape
         * it broke. This block is the real deliverable of the sandwich rule: it is what stops the
         * single permitted inference widening into the general one the spec rejected.
         */
        const staysBare = (doc: JSONContent, label: string) => {
            const healed = healEmojiNodes(doc, emojis);
            // Find by CONTAINS, not equality: where the symbol's neighbour is itself unmarked the
            // merge legitimately absorbs it, so no node's text is exactly '©'. What matters is
            // that whatever node holds the symbol carries no inherited mark.
            const holder = inline(healed).find((n) => (n.text ?? '').includes('©'));

            expect(holder).toBeDefined();
            expect(markTypes(holder as JSONContent)).toEqual([]);
            expect(types(healed)).not.toContain('emoji');

            return label;
        };

        it('link marks differing in href', () => {
            staysBare(SANDWICH_DIFFERENT_HREF, 'href');
        });

        it('link marks differing in target', () => {
            staysBare(SANDWICH_DIFFERENT_TARGET, 'target');
        });

        it('link marks differing in aria-label', () => {
            staysBare(SANDWICH_DIFFERENT_ARIA_LABEL, 'aria-label');
        });

        it('link marks differing in rel', () => {
            staysBare(SANDWICH_DIFFERENT_REL, 'rel');
        });

        it('link marks differing in title', () => {
            staysBare(SANDWICH_DIFFERENT_TITLE, 'title');
        });

        it('a boundary emoji carrying its OWN marks stops the run', () => {
            staysBare(SANDWICH_MARKED_EMOJI_BOUNDARY, 'marked-boundary');
        });

        it('a boundary emoji whose name never resolves stops the run', () => {
            const healed = healEmojiNodes(SANDWICH_UNRESOLVABLE_BOUNDARY, emojis);
            const holder = inline(healed).find((n) => (n.text ?? '').includes('©'));

            expect(markTypes(holder as JSONContent)).toEqual([]);
            // The unresolvable node is left in place, which is what stops the run.
            expect(types(healed)).toContain('emoji');
        });

        it('only ONE side carrying an extra mark blocks inheritance', () => {
            staysBare(SANDWICH_TWO_MARKS_ONE_SIDE, 'one-sided-bold');
        });

        it('an inline image sibling keeps the symbol out of the link', () => {
            staysBare(SANDWICH_IMAGE_SIBLING, 'image');
        });

        it('a hardBreak INSIDE a run of symbols still breaks it', () => {
            // The scan steps over adjacent symbols only. It must not tunnel through anything else
            // hunting for a text node to match.
            staysBare(SANDWICH_RUN_BROKEN_BY_HARD_BREAK, 'run-broken');
        });

        it('two symbols between links with DIFFERENT hrefs stay out of both', () => {
            const healed = healEmojiNodes(SANDWICH_TWO_EMOJI_DIFFERENT_LINK, emojis);
            const holder = inline(healed).find((n) => (n.text ?? '').includes('©'));

            expect(markTypes(holder as JSONContent)).toEqual([]);
            expect(types(healed)).not.toContain('emoji');
        });

        it('a hardBreak sibling breaks the run', () => {
            staysBare(SANDWICH_HARD_BREAK_SIBLING, 'hardBreak');
        });

        it('a sibling carrying no link mark', () => {
            staysBare(SANDWICH_UNLINKED_NEIGHBOUR, 'unlinked');
        });

        it('the node is first in its block — no preceding sibling', () => {
            const healed = healEmojiNodes(SANDWICH_AT_BLOCK_START, emojis);

            expect(markTypes(inline(healed)[0])).toEqual([]);
        });

        it('the node is last in its block — no following sibling', () => {
            const healed = healEmojiNodes(SANDWICH_AT_BLOCK_END, emojis);
            const nodes = inline(healed);

            expect(markTypes(nodes[nodes.length - 1])).toEqual([]);
        });

        it('text nodes whose marks differ are never merged', () => {
            const healed = healEmojiNodes(SANDWICH_DIFFERENT_HREF, emojis);

            expect(inline(healed).length).toBeGreaterThan(1);
        });

        /**
         * The heal normalizes what it CREATES, not what it finds. A document-wide merge would
         * rewrite identical-mark runs that have nothing to do with this defect — the same class of
         * harm the mark rules exist to prevent, arriving through the back door.
         */
        it('an inline array the heal did not touch is returned as found', () => {
            const untouched: JSONContent = {
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        content: [
                            { type: 'text', marks: [{ type: 'bold' }], text: 'one' },
                            { type: 'text', marks: [{ type: 'bold' }], text: 'two' }
                        ]
                    }
                ]
            };

            expect(healEmojiNodes(untouched, emojis)).toEqual(untouched);
        });
    });

    describe('AC-015 — the gate matches on the FULL mark set', () => {
        /**
         * The reviewer read `previous.marks` as "wider than AC-015, which says the link mark".
         * It is the opposite: requiring both sides' whole sets to match fires strictly LESS often
         * than comparing `href` alone, and inheritance then carries the set that matched. Narrower
         * gate, wider payload, and the two are the same decision.
         */
        it('inherits every mark when both boundaries carry the identical set', () => {
            const healed = healEmojiNodes(SANDWICH_TWO_MARKS_BOTH_SIDES, emojis);
            const nodes = inline(healed);

            expect(nodes).toHaveLength(1);
            expect(markTypes(nodes[0]).sort()).toEqual(['bold', 'link']);
        });
    });

    describe('malformed stored JSON must not blank the field', () => {
        /**
         * `loadContent` calls the heal BEFORE `setContent`, so a throw here means `setContent`
         * never runs and the field renders empty over intact stored JSON — the #37145 mechanism
         * this spec cites twice. Returning the input untouched degrades to pre-fix behaviour.
         */
        it('returns the input untouched for null attrs and non-array marks', () => {
            expect(() => healEmojiNodes(MALFORMED_MARKS, emojis)).not.toThrow();
            expect(healEmojiNodes(MALFORMED_MARKS, emojis)).toBeDefined();
        });
    });

    describe('AC-017 — recursive over every block that holds inline content', () => {
        it('heals emoji nodes in headings, list items, blockquotes and table cells', () => {
            const healed = healEmojiNodes(NESTED_BLOCKS, emojis);

            expect(types(healed)).not.toContain('emoji');

            const flat = JSON.stringify(healed);
            expect(flat).toContain('©');
            expect(flat).toContain('™');
            expect(flat).toContain('®');
        });
    });

    describe('AC-018 — an unresolvable name is left untouched', () => {
        it('keeps the node rather than blanking it or emitting :name:', () => {
            const healed = healEmojiNodes(UNRESOLVABLE_NAME, emojis);

            expect(types(healed)).toContain('emoji');
            expect(JSON.stringify(healed)).not.toContain(':not_a_real_shortcode_37340:');
        });
    });

    describe('AC-019 — the identity path', () => {
        /**
         * The overwhelmingly common case: nearly every field load in production contains no
         * `emoji` node at all. Asserted loudest because a heal that rewrote those would be a far
         * bigger problem than the defect it fixes.
         */
        it('returns a document with no emoji node completely unchanged', () => {
            expect(healEmojiNodes(NO_EMOJI_CONTROL, emojis)).toEqual(NO_EMOJI_CONTROL);
        });

        it('returns the same object reference when nothing changed', () => {
            expect(healEmojiNodes(NO_EMOJI_CONTROL, emojis)).toBe(NO_EMOJI_CONTROL);
        });

        it('handles an empty document', () => {
            const empty: JSONContent = { type: 'doc', content: [] };

            expect(healEmojiNodes(empty, emojis)).toBe(empty);
        });
    });

    describe('both value shapes the editor can receive', () => {
        /**
         * `normalizeEditorContent` yields either a `{type:'doc'}` object or, for hosts like the UVE
         * side panel, a bare node array. Spreading an array into an object drops the document type
         * and makes TipTap throw — which is how #37145 blanked a field.
         */
        it('accepts a bare node array and returns one', () => {
            const asArray = REPORTED_PAYLOAD.content as JSONContent[];
            const healed = healEmojiNodes(asArray, emojis) as JSONContent[];

            expect(Array.isArray(healed)).toBe(true);
            expect(types({ type: 'doc', content: healed })).not.toContain('emoji');
        });
    });
});
