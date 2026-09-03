import { emojis } from '@tiptap/extension-emoji';

import { execFileSync } from 'node:child_process';
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
    describe('AC-012: the generated emoji map', () => {
        it('resolves the three reported symbols', () => {
            expect(resolveEmoji({ type: 'emoji', attrs: { name: 'copyright' } })).toBe('©');
            expect(resolveEmoji({ type: 'emoji', attrs: { name: 'registered' } })).toBe('®');
            expect(resolveEmoji({ type: 'emoji', attrs: { name: 'tm' } })).toBe('™');
        });

        it('covers every entry the editor could have produced', () => {
            const missing = emojis
                .filter((item) => item.name && item.emoji)
                .filter(
                    (item) =>
                        resolveEmoji({ type: 'emoji', attrs: { name: item.name } }) !== item.emoji
                );

            expect(missing).toHaveLength(0);
        });

        it('the committed artifacts are not stale', () => {
            // Same assertion CI makes. A stale map means a renderer resolves a name the editor
            // could produce to nothing.
            expect(() =>
                execFileSync('node', ['tools/scripts/generate-emoji-map.mjs', '--check'], {
                    cwd: resolve(__dirname, '../../../../../..')
                })
            ).not.toThrow();
        });
    });

    describe('AC-013: unresolved names never render empty', () => {
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
            expect(flatText(groups)).toBe(fixtures['shape1_emojiSplitsLink'].expectedText);
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
        it('produces the same text either way', () => {
            expect(flatText(groupLinkRuns(load('mixedRepresentation')))).toBe(
                fixtures['mixedRepresentation'].expectedText
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
