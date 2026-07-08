import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick } from 'vue';

import Contentlet from './Contentlet.vue';

import { provideDotCMSPageContext } from '../../contexts/dotcms-page.context';

// EDIT-mode UVE state so the contentlet should emit editor metadata.
// NOTE: UVE_MODE.EDIT === 'EDIT_MODE' (not 'EDIT').
vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn().mockReturnValue({ mode: 'EDIT_MODE' })
}));

const Banner = defineComponent({
    props: { title: { type: String, default: '' } },
    setup: (p) => () => h('div', { 'data-testid': 'banner' }, p.title)
});

const CONTENTLET = {
    identifier: 'c1',
    inode: 'i1',
    title: 'Hi',
    contentType: 'Banner',
    baseType: 'CONTENT'
} as never;

const Host = defineComponent({
    setup() {
        provideDotCMSPageContext({
            pageAsset: {} as never,
            mode: 'production',
            userComponents: { Banner }
        });

        return () =>
            h(Contentlet, { contentlet: CONTENTLET, container: '{"identifier":"cont-1"}' });
    }
});

describe('Contentlet (edit mode)', () => {
    beforeEach(() => vi.clearAllMocks());

    it('emits data-dot-object="contentlet" and the editor metadata in edit mode', async () => {
        const wrapper = mount(Host);
        await flushPromises();
        await nextTick();

        const el = wrapper.find('.dotcms-contentlet');
        expect(el.exists()).toBe(true);
        expect(el.attributes('data-dot-object')).toBe('contentlet');
        expect(el.attributes('data-dot-identifier')).toBe('c1');
        expect(el.attributes('data-dot-inode')).toBe('i1');
        // The mapped component still renders inside.
        expect(wrapper.find('[data-testid="banner"]').exists()).toBe(true);
    });
});
