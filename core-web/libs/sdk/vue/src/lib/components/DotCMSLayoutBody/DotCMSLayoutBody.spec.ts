import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';

import DotCMSLayoutBody from './DotCMSLayoutBody.vue';

import { MOCK_PAGE_ASSET, PAGE_ASSET_NO_BODY } from '../../__test__/mocks';

// Keep UVE state out of the editor so dev-only metadata is not emitted.
vi.mock('@dotcms/uve', () => ({ getUVEState: vi.fn().mockReturnValue(undefined) }));

const Banner = defineComponent({
    props: { title: { type: String, default: '' } },
    setup: (props) => () => h('div', { 'data-testid': 'banner' }, props.title)
});

describe('DotCMSLayoutBody', () => {
    beforeEach(() => vi.clearAllMocks());

    it('renders the rows and dispatches contentlets to mapped components', () => {
        const wrapper = mount(DotCMSLayoutBody, {
            props: { page: MOCK_PAGE_ASSET, components: { Banner }, mode: 'production' }
        });
        const banner = wrapper.find('[data-testid="banner"]');
        expect(banner.exists()).toBe(true);
        expect(banner.text()).toBe('Test Banner');
    });

    it('renders the error message when layout body is missing (dev mode)', () => {
        const wrapper = mount(DotCMSLayoutBody, {
            props: { page: PAGE_ASSET_NO_BODY, components: {}, mode: 'development' }
        });
        expect(wrapper.find('[data-testid="error-message"]').exists()).toBe(true);
    });

    it('wraps contentlets with the contentlet class', () => {
        const wrapper = mount(DotCMSLayoutBody, {
            props: { page: MOCK_PAGE_ASSET, components: { Banner }, mode: 'production' }
        });
        expect(wrapper.find('.dotcms-contentlet').exists()).toBe(true);
    });
});
