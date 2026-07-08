import { isReactive, isRef, onBeforeUnmount, onMounted, shallowRef, toRaw, type Ref } from 'vue';

import {
    UVEEventType,
    type DotCMSComposedPageResponse,
    type DotCMSExtendedPageResponse
} from '@dotcms/types';
import { createUVESubscription, getUVEState, initUVE, updateNavigation } from '@dotcms/uve';
import { registerStyleEditorSchemas } from '@dotcms/uve/internal';

/**
 * Recursively unwrap Vue reactivity (refs / reactive proxies) into a plain,
 * structured-clone-safe object.
 *
 * The UVE bridge sends the page response to the editor via `postMessage`, which
 * uses the structured clone algorithm — and that throws a `DataCloneError` on
 * Vue reactive Proxies. Callers commonly pass a value that came through reactive
 * props or `reactive()`, so we defensively deep-unwrap before handing anything
 * to the UVE. Plain values pass through untouched.
 *
 * @internal
 */
function toPlain<T>(value: T): T {
    const seen = new WeakMap<object, unknown>();

    const unwrap = (input: unknown): unknown => {
        const raw = isRef(input)
            ? (input as { value: unknown }).value
            : isReactive(input)
              ? toRaw(input)
              : input;

        if (raw === null || typeof raw !== 'object') {
            return raw;
        }

        if (seen.has(raw as object)) {
            return seen.get(raw as object);
        }

        if (Array.isArray(raw)) {
            const arr: unknown[] = [];
            seen.set(raw as object, arr);
            raw.forEach((item) => arr.push(unwrap(item)));

            return arr;
        }

        // Preserve non-plain objects (Date, etc.) as-is — they clone fine.
        const proto = Object.getPrototypeOf(raw);
        if (proto !== Object.prototype && proto !== null) {
            return raw;
        }

        const out: Record<string, unknown> = {};
        seen.set(raw as object, out);
        for (const key of Object.keys(raw as Record<string, unknown>)) {
            out[key] = unwrap((raw as Record<string, unknown>)[key]);
        }

        return out;
    };

    return unwrap(value) as T;
}

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
