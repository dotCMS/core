import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';

import { UVEEventType } from '@dotcms/types';
import { createUVESubscription, getUVEState, initUVE, updateNavigation } from '@dotcms/uve';

import { useEditableDotCMSPage } from './useEditableDotCMSPage';

vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn(),
    initUVE: vi.fn(),
    createUVESubscription: vi.fn(),
    updateNavigation: vi.fn()
}));

vi.mock('@dotcms/uve/internal', () => ({
    registerStyleEditorSchemas: vi.fn()
}));

const PAGE = {
    pageAsset: { page: { pageURI: '/test' } },
    content: {}
} as never;

const mountWith = () => {
    let exposed: ReturnType<typeof useEditableDotCMSPage> | undefined;
    const Comp = defineComponent({
        setup() {
            exposed = useEditableDotCMSPage(PAGE);

            return () => h('div');
        }
    });
    const wrapper = mount(Comp);

    return { wrapper, get: () => exposed! };
};

describe('useEditableDotCMSPage', () => {
    const destroyUVESubscriptions = vi.fn();
    const unsubscribe = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
        (initUVE as ReturnType<typeof vi.fn>).mockReturnValue({ destroyUVESubscriptions });
        (createUVESubscription as ReturnType<typeof vi.fn>).mockReturnValue({ unsubscribe });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('returns the initial page response', () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({ mode: 'EDIT' });
        const { get } = mountWith();
        expect(get().value).toEqual(PAGE);
    });

    it('does not init UVE outside the editor', () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue(undefined);
        mountWith();
        expect(initUVE).not.toHaveBeenCalled();
        expect(updateNavigation).not.toHaveBeenCalled();
    });

    it('inits UVE and updates navigation inside the editor', () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({ mode: 'EDIT' });
        mountWith();
        expect(initUVE).toHaveBeenCalledWith(PAGE);
        expect(updateNavigation).toHaveBeenCalledWith('/test');
    });

    it('updates the response on a CONTENT_CHANGES event', async () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({ mode: 'EDIT' });
        let cb: ((p: unknown) => void) | undefined;
        (createUVESubscription as ReturnType<typeof vi.fn>).mockImplementation(
            (type: string, callback: (p: unknown) => void) => {
                if (type === UVEEventType.CONTENT_CHANGES) {
                    cb = callback;
                }

                return { unsubscribe };
            }
        );

        const { get } = mountWith();
        const updated = { pageAsset: { page: { pageURI: '/test' } }, content: { x: 1 } };
        cb?.(updated);
        await flushPromises();
        expect(get().value).toEqual(updated);
    });

    it('cleans up subscriptions on unmount', () => {
        (getUVEState as ReturnType<typeof vi.fn>).mockReturnValue({ mode: 'EDIT' });
        const { wrapper } = mountWith();
        wrapper.unmount();
        expect(destroyUVESubscriptions).toHaveBeenCalled();
        expect(unsubscribe).toHaveBeenCalled();
    });
});
