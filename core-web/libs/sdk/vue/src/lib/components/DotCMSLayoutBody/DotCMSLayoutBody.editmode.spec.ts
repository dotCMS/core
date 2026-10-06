import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick } from 'vue';

import DotCMSLayoutBody from './DotCMSLayoutBody.vue';

import { MOCK_PAGE_ASSET } from '../../__test__/mocks';

// Full tree, UVE reports EDIT_MODE — the editor DOM contract must render.
vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn().mockReturnValue({ mode: 'EDIT_MODE' })
}));

const Banner = defineComponent({
    setup: () => () => h('div', { 'data-testid': 'banner' }, 'b')
});

describe('DotCMSLayoutBody (edit mode, full tree)', () => {
    beforeEach(() => vi.clearAllMocks());

    it('renders container + contentlet editor attributes through the whole tree', async () => {
        const wrapper = mount(DotCMSLayoutBody, {
            props: { page: MOCK_PAGE_ASSET, components: { Banner }, mode: 'production' }
        });
        await flushPromises();
        await nextTick();

        expect(wrapper.find('[data-dot-object="container"]').exists()).toBe(true);
        expect(wrapper.find('[data-dot-object="contentlet"]').exists()).toBe(true);
        expect(wrapper.find('[data-dot-object="row"]').exists()).toBe(true);
    });
});
