import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick } from 'vue';

import type { DotCMSPageAsset } from '@dotcms/types';

import DotCMSLayoutBody from './DotCMSLayoutBody.vue';

import { useEditableDotCMSPage } from '../../composables/useEditableDotCMSPage';

// Capture the CONTENT_CHANGES callback so the test can drive a live update,
// exactly like the editor posting `uve-set-page-data`.
let contentChangesCb: ((payload: unknown) => void) | undefined;

vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn().mockReturnValue({ mode: 'EDIT_MODE' }),
    initUVE: vi.fn().mockReturnValue({ destroyUVESubscriptions: vi.fn() }),
    updateNavigation: vi.fn(),
    createUVESubscription: vi.fn((_type: string, cb: (p: unknown) => void) => {
        contentChangesCb = cb;

        return { unsubscribe: vi.fn() };
    })
}));

vi.mock('@dotcms/uve/internal', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@dotcms/uve/internal');

    return { ...actual, registerStyleEditorSchemas: vi.fn() };
});

const Banner = defineComponent({
    props: { title: { type: String, default: '' } },
    setup: (p) => () => h('div', { 'data-testid': 'banner' }, p.title)
});

/** Builds a minimal page asset with one Banner contentlet of the given title. */
const pageWithTitle = (title: string): DotCMSPageAsset =>
    ({
        layout: {
            body: {
                rows: [
                    {
                        styleClass: '',
                        columns: [
                            {
                                leftOffset: 1,
                                width: 12,
                                containers: [{ identifier: 'c', uuid: '1' }]
                            }
                        ]
                    }
                ]
            }
        },
        containers: {
            c: {
                container: { identifier: 'c', maxContentlets: 1 },
                containerStructures: [{ contentTypeVar: 'Banner' }],
                contentlets: {
                    'uuid-1': [
                        {
                            identifier: 'b1',
                            inode: 'i1',
                            title,
                            contentType: 'Banner',
                            baseType: 'CONTENT'
                        }
                    ]
                }
            }
        }
    }) as unknown as DotCMSPageAsset;

describe('DotCMSLayoutBody live update (uve-set-page-data)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        contentChangesCb = undefined;
    });

    it('re-renders the layout when the editor posts a new page response', async () => {
        const Host = defineComponent({
            setup() {
                const page = useEditableDotCMSPage({
                    pageAsset: pageWithTitle('Original')
                } as never);

                return () =>
                    h(DotCMSLayoutBody, {
                        page: (page.value as { pageAsset: DotCMSPageAsset }).pageAsset,
                        components: { Banner },
                        mode: 'production'
                    });
            }
        });

        const wrapper = mount(Host);
        await flushPromises();
        await nextTick();

        expect(wrapper.find('[data-testid="banner"]').text()).toBe('Original');
        expect(contentChangesCb).toBeTypeOf('function');

        // Editor pushes updated content.
        contentChangesCb?.({ pageAsset: pageWithTitle('Updated') });
        await flushPromises();
        await nextTick();

        expect(wrapper.find('[data-testid="banner"]').text()).toBe('Updated');
    });
});
