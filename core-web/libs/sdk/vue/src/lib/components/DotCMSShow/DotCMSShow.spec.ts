import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { h, nextTick } from 'vue';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import DotCMSShow from './DotCMSShow.vue';

vi.mock('@dotcms/uve', () => ({ getUVEState: vi.fn() }));

describe('DotCMSShow', () => {
    beforeEach(() => vi.clearAllMocks());

    it('renders the slot when the UVE mode matches', async () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({ mode: UVE_MODE.EDIT });
        const wrapper = mount(DotCMSShow, {
            props: { when: UVE_MODE.EDIT },
            slots: { default: () => h('span', { 'data-testid': 'content' }, 'edit') }
        });
        await flushPromises();
        await nextTick();
        expect(wrapper.find('[data-testid="content"]').exists()).toBe(true);
    });

    it('hides the slot when the UVE mode does not match', async () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({ mode: UVE_MODE.LIVE });
        const wrapper = mount(DotCMSShow, {
            props: { when: UVE_MODE.EDIT },
            slots: { default: () => h('span', { 'data-testid': 'content' }, 'edit') }
        });
        await flushPromises();
        await nextTick();
        expect(wrapper.find('[data-testid="content"]').exists()).toBe(false);
    });
});
