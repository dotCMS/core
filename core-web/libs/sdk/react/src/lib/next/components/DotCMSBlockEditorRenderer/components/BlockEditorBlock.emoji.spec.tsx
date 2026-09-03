import { render } from '@testing-library/react';

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import type { BlockEditorNode } from '@dotcms/types';

import { BlockEditorBlock } from './BlockEditorBlock';

const fixtures = JSON.parse(
    readFileSync(
        resolve(__dirname, '../../../../../../../types/src/lib/testing/story-block.fixtures.json'),
        'utf8'
    )
);

const load = (key: string): BlockEditorNode[] => fixtures[key].content;

const renderNodes = (nodes: BlockEditorNode[]) => {
    const { container } = render(<BlockEditorBlock content={nodes} />);

    return container;
};

/**
 * Renderer-level coverage for #37340. The unit specs in `@dotcms/client` pin the grouping logic;
 * these assert the DOM the React SDK actually emits, which is what a consumer ships.
 */
describe('React Block Editor renderer — emoji + link runs (#37340)', () => {
    it('AC-010: renders the character for a stored emoji node', () => {
        expect(renderNodes(load('mixedRepresentation')).textContent).toBe(
            fixtures['mixedRepresentation'].expectedText
        );
    });

    it('AC-011: emits the emoji inline, never as a block element', () => {
        const container = renderNodes([{ type: 'emoji', attrs: { name: 'copyright' } }]);

        expect(container.querySelector('div')).toBeNull();
        expect(container.textContent).toBe('©');
    });

    it('AC-015: Shape 1 — a link split by an unmarked emoji renders as ONE anchor', () => {
        const container = renderNodes(load('shape1_emojiSplitsLink'));
        const links = container.querySelectorAll('a');

        expect(links).toHaveLength(1);
        expect(links[0].textContent).toBe(fixtures['shape1_emojiSplitsLink'].expectedText);
        expect(links[0].getAttribute('href')).toBe('https://dotcms.com');
    });

    it('Shape 2 — an emoji already carrying the link stays in one anchor', () => {
        expect(renderNodes(load('shape2_emojiCarriesLink')).querySelectorAll('a')).toHaveLength(1);
    });

    it('AC-016: a non-emoji atom breaks the run', () => {
        expect(renderNodes(load('nonEmojiAtomBreaksRun')).querySelectorAll('a')).toHaveLength(2);
    });

    it('AC-017: differing link attrs stay separate anchors', () => {
        expect(renderNodes(load('differingLinkAttrs')).querySelectorAll('a')).toHaveLength(2);
    });

    it('AC-018: bold still nests inside the single anchor', () => {
        const container = renderNodes(load('boldNestedInLink'));

        expect(container.querySelectorAll('a')).toHaveLength(1);
        expect(container.querySelector('a strong')).not.toBeNull();
    });

    it('AC-013: an unresolvable name renders escaped, never as live markup', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const container = renderNodes(load('unresolvableNameWithHtml'));

        // `name` is user-controlled via the Contentlet API, which applies no Story Block schema
        // validation — it must never become a live element.
        expect(container.querySelector('img')).toBeNull();
        expect(container.textContent).toContain('<img src=x onerror=alert(1)>');

        warn.mockRestore();
    });

    it('does not wrap unlinked content in an anchor', () => {
        expect(renderNodes(load('noOp')).querySelectorAll('a')).toHaveLength(0);
    });
});
