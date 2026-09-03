import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import type { BlockEditorMark, BlockEditorNode } from '@dotcms/types';

import { groupLinkRuns, resolveEmoji } from './link-runs';

const fixtures = JSON.parse(
    readFileSync(
        resolve(__dirname, '../../../../types/src/lib/testing/story-block.fixtures.json'),
        'utf8'
    )
);

const load = (key: string): BlockEditorNode[] => fixtures[key].content;

const anchors = (groups: ReturnType<typeof groupLinkRuns>) =>
    groups.filter((group) => group.link).length;

const flatText = (groups: ReturnType<typeof groupLinkRuns>) =>
    groups
        .flatMap((group) => group.nodes)
        .map((node) => (node.type === 'emoji' ? resolveEmoji(node) : (node.text ?? '')))
        .join('');

describe('Story Block render helpers (#37340)', () => {
    describe('AC-013: legacy emoji nodes never render empty', () => {
        it('falls back to :name: and warns once per distinct name', () => {
            const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
            const scope = new Set<string>();
            const node = load('unresolvableName')[0];

            expect(resolveEmoji(node, scope)).toBe(':definitely-not-a-real-shortcode:');
            expect(resolveEmoji(node, scope)).toBe(':definitely-not-a-real-shortcode:');

            expect(warn).toHaveBeenCalledTimes(1);
            expect(warn).toHaveBeenCalledWith(
                '[dotCMS Block Editor]: Emoji definitely-not-a-real-shortcode is not supported'
            );

            warn.mockRestore();
        });

        it('prefers the node text over the shortcode fallback', () => {
            expect(resolveEmoji(load('emojiNodeWithTextAttr')[0])).toBe('★');
        });

        it('returns markup as inert text so renderers escape it', () => {
            const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

            // `name` is user-controlled — the Contentlet API applies no Story Block schema
            // validation — so the fallback must stay data, never live markup.
            expect(resolveEmoji(load('unresolvableNameWithHtml')[0], new Set())).toBe(
                ':<img src=x onerror=alert(1)>:'
            );

            warn.mockRestore();
        });
    });

    describe('AC-014/AC-015: link runs collapse to one anchor', () => {
        it('AC-015 Shape 1 — absorbs the unmarked emoji between two identical links', () => {
            const groups = groupLinkRuns(load('shape1_emojiSplitsLink'));

            expect(anchors(groups)).toBe(1);
            expect(groups).toHaveLength(1);
            // No lookup table is carried, so the legacy node renders its shortcode visibly
            // rather than the character — see resolveEmoji.
            expect(flatText(groups)).toBe('dotCMS Copyright :copyright:All rights reserved');
        });

        it('Shape 2 — an emoji already carrying the link stays in one anchor', () => {
            expect(anchors(groupLinkRuns(load('shape2_emojiCarriesLink')))).toBe(1);
        });

        it('merges plain adjacent text nodes sharing a link', () => {
            expect(anchors(groupLinkRuns(load('boldNestedInLink')))).toBe(1);
        });
    });

    describe('AC-016/AC-017: runs that must NOT merge', () => {
        it('AC-016 — a non-emoji unmarked atom breaks the run', () => {
            expect(anchors(groupLinkRuns(load('nonEmojiAtomBreaksRun')))).toBe(2);
        });

        it('AC-017 — differing href keeps the anchors separate', () => {
            expect(anchors(groupLinkRuns(load('differingLinkAttrs')))).toBe(2);
        });

        it.each(['target', 'rel', 'title', 'aria-label'])(
            'AC-017 — a differing %s keeps the anchors separate',
            (attr) => {
                const base: BlockEditorMark = {
                    type: 'link',
                    attrs: { href: 'https://dotcms.com' }
                };
                const nodes: BlockEditorNode[] = [
                    { type: 'text', marks: [base], text: 'one' },
                    {
                        type: 'text',
                        marks: [{ type: 'link', attrs: { ...base.attrs, [attr]: 'x' } }],
                        text: 'two'
                    }
                ];

                expect(anchors(groupLinkRuns(nodes))).toBe(2);
            }
        );

        it('does not wrap a standalone emoji in an anchor', () => {
            const groups = groupLinkRuns([{ type: 'emoji', attrs: { name: 'copyright' } }]);

            expect(anchors(groups)).toBe(0);
        });
    });

    describe('contract C4: node form and text form render identically', () => {
        it('renders the legacy node visibly alongside the literal character', () => {
            // The text half is the shape all NEW content uses; the node half is legacy and
            // degrades to a visible shortcode.
            expect(flatText(groupLinkRuns(load('mixedRepresentation')))).toBe(
                'node form: :copyright: / text form: ©'
            );
        });

        it('leaves content with nothing to coalesce untouched', () => {
            const nodes = load('noOp');
            const groups = groupLinkRuns(nodes);

            expect(anchors(groups)).toBe(0);
            expect(groups.flatMap((group) => group.nodes)).toEqual(nodes);
        });
    });
});
