import {
    onBeforeUnmount,
    onMounted,
    shallowRef,
    toValue,
    watch,
    type MaybeRefOrGetter,
    type Ref
} from 'vue';

import {
    UVEEventType,
    type DotCMSComposedPageResponse,
    type DotCMSExtendedPageResponse
} from '@dotcms/types';
import { createUVESubscription, getUVEState, initUVE, updateNavigation } from '@dotcms/uve';
import { registerStyleEditorSchemas } from '@dotcms/uve/internal';

import { toPlain } from '../utils/toPlain';

/**
 * Composable to manage the editable state of a dotCMS page inside the Universal
 * Visual Editor (UVE).
 *
 * It initializes the UVE, keeps the navigation in sync, and subscribes to
 * content changes so the returned page response updates live while an editor is
 * working. Outside of the UVE it is a pass-through: the returned ref simply
 * holds the initial response.
 *
 * This is the Vue analog of the React SDK's `useEditableDotCMSPage` hook and the
 * Angular SDK's `DotCMSEditablePageService.listen()`.
 *
 * @example
 * ```ts
 * import { useEditableDotCMSPage } from '@dotcms/vue';
 * import { createDotCMSClient } from '@dotcms/client';
 *
 * const client = createDotCMSClient({
 *   dotcmsUrl: 'https://your-dotcms-instance.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * const pageResponse = await client.page.get('/');
 * const page = useEditableDotCMSPage(pageResponse);
 *
 * // page.value.pageAsset — reactive; re-renders on UVE content changes.
 * ```
 *
 * @param pageResponse the page response from `client.page.get()`. May be a plain
 * value, a `ref`, or a getter — when it's reactive, the composable re-initializes
 * the UVE for the new page (e.g. on client-side route changes) so no manual
 * component remount is needed.
 * @returns a reactive ref holding the (possibly live-updating) page response
 */
export function useEditableDotCMSPage<T extends DotCMSExtendedPageResponse>(
    pageResponse: MaybeRefOrGetter<DotCMSComposedPageResponse<T>>
): Ref<DotCMSComposedPageResponse<T>> {
    // shallowRef: the page response is a large object and we always replace it
    // wholesale on a content change, so deep reactivity would be wasted work.
    const response = shallowRef<DotCMSComposedPageResponse<T>>(toValue(pageResponse));

    let destroyUVESubscriptions: (() => void) | undefined;
    let unsubscribeContentChanges: (() => void) | undefined;

    /**
     * (Re)initialize the UVE for the given page: tear down any previous
     * subscriptions, then wire up the new page and sync navigation. Guarded so it
     * is a no-op outside the editor.
     */
    const initForPage = (page: DotCMSComposedPageResponse<T>) => {
        destroyUVESubscriptions?.();
        destroyUVESubscriptions = undefined;

        if (!getUVEState()) {
            return;
        }

        if (!page) {
            console.warn('[useEditableDotCMSPage]: No page response provided');

            return;
        }

        // Callers often pass a value that came through reactive props / reactive().
        // The UVE posts this to the editor via structured clone, which cannot clone
        // Vue Proxies — so unwrap to a plain object before any UVE call.
        const plainResponse = toPlain(page);
        const pageURI = plainResponse?.pageAsset?.page?.pageURI;

        ({ destroyUVESubscriptions } = initUVE(plainResponse));

        // Sometimes the page is null due to permissions, so we only update the
        // navigation when we actually have a pageURI and let the UVE resolve it.
        if (pageURI) {
            updateNavigation(pageURI);
        }

        if (plainResponse.styleEditorSchemas?.length) {
            registerStyleEditorSchemas(plainResponse.styleEditorSchemas);
        }
    };

    // Re-init whenever the source page changes (e.g. route navigation that swaps
    // the response into the same component). `immediate` covers the initial mount.
    watch(
        () => toValue(pageResponse),
        (page) => {
            response.value = page;
            initForPage(page);
        },
        { immediate: true }
    );

    // Subscribe to content changes once (matches React's `[]` effect). The editor
    // posts `uve-set-page-data`; swap in the new response so the reactive
    // `response` ref re-renders the page.
    onMounted(() => {
        ({ unsubscribe: unsubscribeContentChanges } = createUVESubscription(
            UVEEventType.CONTENT_CHANGES,
            (payload: DotCMSComposedPageResponse<T>) => {
                response.value = payload;
            }
        ));
    });

    onBeforeUnmount(() => {
        destroyUVESubscriptions?.();
        unsubscribeContentChanges?.();
    });

    return response;
}
