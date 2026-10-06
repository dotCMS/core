import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';

import DotCMSLayoutBody from './DotCMSLayoutBody.vue';

import { MOCK_PAGE_ASSET } from '../../__test__/mocks';

// Keep UVE state out of the editor so dev-only metadata is not emitted.
vi.mock('@dotcms/uve', () => ({ getUVEState: vi.fn().mockReturnValue(undefined) }));

const Banner = defineComponent({
    props: { title: { type: String, default: '' } },
    setup: (props) => () => h('div', { 'data-testid': 'banner' }, props.title)
});

describe('DotCMSLayoutBody per-contentlet slots', () => {
    beforeEach(() => vi.clearAllMocks());

    it('renders a #contentlet-<identifier> slot instead of the mapped component', () => {
        const wrapper = mount(DotCMSLayoutBody, {
            props: { page: MOCK_PAGE_ASSET, components: { Banner }, mode: 'production' },
            slots: {
                // MOCK_PAGE_ASSET's only contentlet has identifier 'contentlet-1'.
                'contentlet-contentlet-1': () => h('div', { 'data-testid': 'slotted' }, 'Slotted!')
            }
        });

        expect(wrapper.find('[data-testid="slotted"]').exists()).toBe(true);
        expect(wrapper.find('[data-testid="slotted"]').text()).toBe('Slotted!');
        // The mapped component must NOT render for a slotted contentlet.
        expect(wrapper.find('[data-testid="banner"]').exists()).toBe(false);
    });

    it('passes the contentlet as the slot scope prop', () => {
        const wrapper = mount(DotCMSLayoutBody, {
            props: { page: MOCK_PAGE_ASSET, components: { Banner }, mode: 'production' },
            slots: {
                'contentlet-contentlet-1': (scope: { contentlet: { title: string } }) =>
                    h('div', { 'data-testid': 'slotted' }, scope.contentlet.title)
            }
        });

        expect(wrapper.find('[data-testid="slotted"]').text()).toBe('Test Banner');
    });

    it('falls back to the mapped component when no slot matches the identifier', () => {
        const wrapper = mount(DotCMSLayoutBody, {
            props: { page: MOCK_PAGE_ASSET, components: { Banner }, mode: 'production' },
            slots: {
                'contentlet-does-not-exist': () => h('div', { 'data-testid': 'slotted' })
            }
        });

        expect(wrapper.find('[data-testid="slotted"]').exists()).toBe(false);
        expect(wrapper.find('[data-testid="banner"]').exists()).toBe(true);
    });
});
