import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import type { BlockEditorNode } from '@dotcms/types';

import BlockEditorBlock from './BlockEditorBlock.vue';

const fixtures = JSON.parse(
    readFileSync(
        resolve(__dirname, '../../../../../../types/src/lib/testing/story-block.fixtures.json'),
        'utf8'
    )
);

const load = (key: string): BlockEditorNode[] => fixtures[key].content;
const mountNodes = (content: BlockEditorNode[]) => mount(BlockEditorBlock, { props: { content } });

/**
 * `wrapper.text()` collapses whitespace, which would hide a real difference from the React and
 * Angular renderers. Assert against the DOM the browser actually gets — contract C4 requires all
 * five renderers to agree byte for byte.
 */
const domText = (el: Element) => el.textContent ?? '';

/**
 * Renderer-level coverage for #37340 — the DOM the Vue SDK actually emits. The grouping logic
 * itself is pinned by the `@dotcms/client` unit specs.
 */
describe('Vue Block Editor renderer — emoji + link runs (#37340)', () => {
    it('AC-010: renders the character for a stored emoji node', () => {
        expect(domText(mountNodes(load('mixedRepresentation')).element)).toBe(
            fixtures['mixedRepresentation'].expectedText
        );
    });

    it('AC-011: emits the emoji inline, never as a block element', () => {
        const wrapper = mountNodes([{ type: 'emoji', attrs: { name: 'copyright' } }]);

        expect(wrapper.find('div').exists()).toBe(false);
        expect(domText(wrapper.element)).toBe('©');
    });

    it('AC-015: Shape 1 — a link split by an unmarked emoji renders as ONE anchor', () => {
        const wrapper = mountNodes(load('shape1_emojiSplitsLink'));
        const links = wrapper.findAll('a');

        expect(links).toHaveLength(1);
        expect(domText(links[0].element)).toBe(fixtures['shape1_emojiSplitsLink'].expectedText);
        expect(links[0].attributes('href')).toBe('https://dotcms.com');
    });

    it('Shape 2 — an emoji already carrying the link stays in one anchor', () => {
        expect(mountNodes(load('shape2_emojiCarriesLink')).findAll('a')).toHaveLength(1);
    });

    it('AC-016: a non-emoji atom breaks the run', () => {
        expect(mountNodes(load('nonEmojiAtomBreaksRun')).findAll('a')).toHaveLength(2);
    });

    it('AC-017: differing link attrs stay separate anchors', () => {
        expect(mountNodes(load('differingLinkAttrs')).findAll('a')).toHaveLength(2);
    });

    it('AC-018: bold still nests inside the single anchor', () => {
        const wrapper = mountNodes(load('boldNestedInLink'));

        expect(wrapper.findAll('a')).toHaveLength(1);
        expect(wrapper.find('a strong').exists()).toBe(true);
    });

    it('AC-013: an unresolvable name renders escaped, never as live markup', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        const wrapper = mountNodes(load('unresolvableNameWithHtml'));

        expect(wrapper.find('img').exists()).toBe(false);
        expect(domText(wrapper.element)).toContain('<img src=x onerror=alert(1)>');

        warn.mockRestore();
    });

    it('does not wrap unlinked content in an anchor', () => {
        expect(mountNodes(load('noOp')).findAll('a')).toHaveLength(0);
    });
});
