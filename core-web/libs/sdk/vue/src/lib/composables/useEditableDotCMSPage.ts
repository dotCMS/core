import { onBeforeUnmount, onMounted, shallowRef, type Ref } from 'vue';

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
 * @param pageResponse the initial page response from `client.page.get()`
 * @returns a reactive ref holding the (possibly live-updating) page response
 */
export function useEditableDotCMSPage<T extends DotCMSExtendedPageResponse>(
    pageResponse: DotCMSComposedPageResponse<T>
): Ref<DotCMSComposedPageResponse<T>> {
    // shallowRef: the page response is a large object and we always replace it
    // wholesale on a content change, so deep reactivity would be wasted work.
    const response = shallowRef<DotCMSComposedPageResponse<T>>(pageResponse);

    let destroyUVESubscriptions: (() => void) | undefined;
    let unsubscribeContentChanges: (() => void) | undefined;

    // Mirror the React SDK's structure: one effect initializes the UVE (guarded
    // by getUVEState), and a SEPARATE, UNCONDITIONAL subscription listens for
    // content changes. Keeping them separate ensures the content-change listener
    // is always registered even if init returns early.
    onMounted(() => {
        if (!getUVEState()) {
            return;
        }

        if (!pageResponse) {
            console.warn('[useEditableDotCMSPage]: No page response provided');

            return;
        }

        // Callers often pass a value that came through reactive props / reactive().
        // The UVE posts this to the editor via structured clone, which cannot clone
        // Vue Proxies — so unwrap to a plain object before any UVE call.
        const plainResponse = toPlain(pageResponse);
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
    });

    // Subscribe to content changes unconditionally (matches React's `[]` effect).
    // The editor posts `uve-set-page-data`; swap in the new response so the
    // reactive `response` ref re-renders the page.
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
