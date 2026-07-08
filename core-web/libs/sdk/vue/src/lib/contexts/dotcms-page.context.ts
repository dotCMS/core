import { computed, inject, provide, type Component, type ComputedRef, type InjectionKey } from 'vue';

import type { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

/**
 * The value carried by the dotCMS page context.
 *
 * This is the Vue analog of the React SDK's `DotCMSPageContext`. It is provided
 * once by {@link DotCMSLayoutBody} and consumed by the internal layout tree
 * (Container, Contentlet) as well as the dev-mode composable.
 *
 * @internal
 */
export interface DotCMSPageContextValue {
    /** The dotCMS page asset containing the layout and containers. */
    pageAsset: DotCMSPageAsset;
    /** The renderer mode: `production` or `development`. */
    mode: DotCMSPageRendererMode;
    /**
     * Map of content-type variable name to the Vue component that renders it.
     * The special `CustomNoComponent` key is used as the fallback for unmatched
     * content types.
     */
    userComponents: Record<string, Component>;
}

/**
 * Injection key for the dotCMS page context.
 *
 * The value is a `ComputedRef` so that live UVE updates to the page (a new page
 * asset arriving via `uve-set-page-data`) propagate to every consumer that reads
 * `ctx.value` — the whole layout tree re-renders reactively.
 *
 * @internal
 */
export const DOTCMS_PAGE_CONTEXT: InjectionKey<ComputedRef<DotCMSPageContextValue>> =
    Symbol('DotCMSPageContext');

/**
 * Provides the dotCMS page context to descendant components.
 *
 * @internal
 * @param value a computed producing the current context value
 */
export function provideDotCMSPageContext(value: ComputedRef<DotCMSPageContextValue>): void {
    provide(DOTCMS_PAGE_CONTEXT, value);
}

const EMPTY_CONTEXT: DotCMSPageContextValue = {
    pageAsset: {} as DotCMSPageAsset,
    mode: 'production',
    userComponents: {}
};

/**
 * Injects the dotCMS page context as a reactive `ComputedRef`.
 *
 * Falls back to an empty context when used outside of a {@link DotCMSLayoutBody},
 * mirroring the default value the React context is created with.
 *
 * @internal
 * @returns the current page context value as a computed ref
 */
export function useDotCMSPageContext(): ComputedRef<DotCMSPageContextValue> {
    return inject(
        DOTCMS_PAGE_CONTEXT,
        computed(() => EMPTY_CONTEXT)
    );
}
