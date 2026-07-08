import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick } from 'vue';

import Container from './Container.vue';

import { MOCK_CONTAINER, MOCK_PAGE_ASSET } from '../../__test__/mocks';
import { provideDotCMSPageContext } from '../../contexts/dotcms-page.context';

// UVE_MODE.EDIT === 'EDIT_MODE'.
vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn().mockReturnValue({ mode: 'EDIT_MODE' })
}));

const Banner = defineComponent({
    setup: () => () => h('div', { 'data-testid': 'banner' }, 'b')
});

const Host = defineComponent({
    setup() {
        provideDotCMSPageContext({
            pageAsset: MOCK_PAGE_ASSET,
            mode: 'production',
            userComponents: { Banner }
        });

        return () => h(Container, { container: MOCK_CONTAINER });
    }
});

describe('Container (edit mode)', () => {
    beforeEach(() => vi.clearAllMocks());

    it('emits the container editor attributes in edit mode', async () => {
        const wrapper = mount(Host);
        await flushPromises();
        await nextTick();

        const container = wrapper.find('[data-dot-object="container"]');
        expect(container.exists()).toBe(true);
        expect(container.attributes('data-dot-identifier')).toBe('test-container-id');
        expect(container.attributes('data-dot-accept-types')).toBe('Banner');
        // The contentlet inside is emitted too.
        expect(wrapper.find('[data-dot-object="contentlet"]').exists()).toBe(true);
    });
});
