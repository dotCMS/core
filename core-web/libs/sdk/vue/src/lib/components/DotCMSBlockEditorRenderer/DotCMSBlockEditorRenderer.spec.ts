import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

import DotCMSBlockEditorRenderer from './DotCMSBlockEditorRenderer.vue';

vi.mock('@dotcms/uve', () => ({ getUVEState: vi.fn().mockReturnValue(undefined) }));

const doc = (content: BlockEditorNode[]): BlockEditorNode =>
    ({ type: 'doc', content }) as BlockEditorNode;

const text = (value: string, marks: BlockEditorNode['marks'] = []): BlockEditorNode =>
    ({ type: 'text', text: value, marks }) as BlockEditorNode;

describe('DotCMSBlockEditorRenderer', () => {
    it('renders a paragraph with text', () => {
        const blocks = doc([{ type: 'paragraph', content: [text('Hello')] } as BlockEditorNode]);
        const wrapper = mount(DotCMSBlockEditorRenderer, { props: { blocks } });
        expect(wrapper.find('[data-testid="dot-block-editor-container"]').exists()).toBe(true);
        expect(wrapper.find('p').text()).toBe('Hello');
    });

    it('renders a heading at the right level', () => {
        const blocks = doc([
            { type: 'heading', attrs: { level: 2 }, content: [text('Title')] } as BlockEditorNode
        ]);
        const wrapper = mount(DotCMSBlockEditorRenderer, { props: { blocks } });
        expect(wrapper.find('h2').text()).toBe('Title');
    });

    it('applies text marks (bold)', () => {
        const blocks = doc([
            {
                type: 'paragraph',
                content: [text('Bold', [{ type: 'bold', attrs: {} }])]
            } as BlockEditorNode
        ]);
        const wrapper = mount(DotCMSBlockEditorRenderer, { props: { blocks } });
        expect(wrapper.find('strong').text()).toBe('Bold');
    });

    it('renders a bullet list', () => {
        const blocks = doc([
            {
                type: 'bulletList',
                content: [
                    { type: 'listItem', content: [text('One')] } as BlockEditorNode,
                    { type: 'listItem', content: [text('Two')] } as BlockEditorNode
                ]
            } as BlockEditorNode
        ]);
        const wrapper = mount(DotCMSBlockEditorRenderer, { props: { blocks } });
        expect(wrapper.findAll('ul li')).toHaveLength(2);
    });

    it('honors a custom renderer for a block type', () => {
        const CustomParagraph = defineComponent({
            setup: (_p, { slots }) => () =>
                h('div', { 'data-testid': 'custom' }, slots.default?.())
        });
        const blocks = doc([{ type: 'paragraph', content: [text('X')] } as BlockEditorNode]);
        const wrapper = mount(DotCMSBlockEditorRenderer, {
            props: { blocks, customRenderers: { paragraph: CustomParagraph } }
        });
        expect(wrapper.find('[data-testid="custom"]').exists()).toBe(true);
        expect(wrapper.find('p').exists()).toBe(false);
    });
});
