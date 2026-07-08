import { onBeforeUnmount, onMounted, shallowRef, type Ref } from 'vue';

import {
    UVEEventType,
    type DotCMSComposedPageResponse,
    type DotCMSExtendedPageResponse
} from '@dotcms/types';
import { createUVESubscription, getUVEState, initUVE, updateNavigation } from '@dotcms/uve';
import { registerStyleEditorSchemas } from '@dotcms/uve/internal';

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

    onMounted(() => {
        // Only wire up the UVE when we are actually inside the editor iframe.
        if (!getUVEState()) {
            return;
        }

        if (!pageResponse) {
            console.warn('[useEditableDotCMSPage]: No page response provided');

            return;
        }

        const pageURI = pageResponse?.pageAsset?.page?.pageURI;

        ({ destroyUVESubscriptions } = initUVE(pageResponse));

        // Sometimes the page is null due to permissions, so we only update the
        // navigation when we actually have a pageURI and let the UVE resolve it.
        if (pageURI) {
            updateNavigation(pageURI);
        }

        if (pageResponse.styleEditorSchemas?.length) {
            registerStyleEditorSchemas(pageResponse.styleEditorSchemas);
        }

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
