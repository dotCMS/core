import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick } from 'vue';

import { getUVEState } from '@dotcms/uve';

import DotCMSEditableText from './DotCMSEditableText.vue';

// Stub the TinyMCE Vue editor so we don't load the real editor in jsdom.
vi.mock('@tinymce/tinymce-vue', () => ({
    default: defineComponent({
        name: 'EditorStub',
        setup: () => () => h('div', { 'data-testid': 'tinymce-editor' })
    })
}));

vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn(),
    sendMessageToUVE: vi.fn()
}));

vi.mock('@dotcms/uve/internal', () => ({
    __TINYMCE_PATH_ON_DOTCMS__: '/tinymce/tinymce.min.js',
    __DEFAULT_TINYMCE_CONFIG__: {},
    __BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__: { plain: {}, minimal: {}, full: {} }
}));

const CONTENTLET = {
    identifier: 'c1',
    inode: 'i1',
    languageId: 1,
    title: '<b>Hello</b> world',
    contentType: 'Banner',
    baseType: 'CONTENT'
} as never;

describe('DotCMSEditableText', () => {
    beforeEach(() => vi.clearAllMocks());

    it('renders the field value as HTML (span) outside edit mode', async () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue(undefined);

        const wrapper = mount(DotCMSEditableText, {
            props: { contentlet: CONTENTLET, fieldName: 'title' }
        });
        await flushPromises();
        await nextTick();

        expect(wrapper.find('[data-testid="tinymce-editor"]').exists()).toBe(false);
        // v-html renders the stored markup, not escaped text.
        expect(wrapper.find('span').html()).toContain('<b>Hello</b> world');
    });

    it('does not mount the editor in EDIT mode without a dotCMSHost', async () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({ mode: 'EDIT_MODE' });

        const wrapper = mount(DotCMSEditableText, {
            props: { contentlet: CONTENTLET, fieldName: 'title' }
        });
        await flushPromises();
        await nextTick();

        expect(wrapper.find('[data-testid="tinymce-editor"]').exists()).toBe(false);
    });

    it('mounts the TinyMCE editor in EDIT mode when a dotCMSHost is present', async () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({
            mode: 'EDIT_MODE',
            dotCMSHost: 'https://demo.dotcms.com'
        });

        const wrapper = mount(DotCMSEditableText, {
            props: { contentlet: CONTENTLET, fieldName: 'title' }
        });
        await flushPromises();
        await nextTick();

        expect(wrapper.find('[data-testid="tinymce-editor"]').exists()).toBe(true);
    });
});
