import {
    computed,
    inject,
    provide,
    type Component,
    type ComputedRef,
    type InjectionKey
} from 'vue';

import type { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

/**
 * The value carried by the dotCMS page context.
 *
 * This is the Vue analog of the React SDK's `DotCMSPageContext`. It is provided
 * once by {@link DotCMSLayoutBody} and consumed by the layout tree (Container,
 * Contentlet). It is also part of the public API: a custom content-type
 * component rendered deep in the tree can read it via {@link useDotCMSPageContext}
 * to access the page asset without prop drilling. This shape is stable.
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
    /**
     * Whether editor metadata (`data-dot-*`, placeholders, fallbacks) should be
     * emitted. Resolved once at the layout root and shared with the whole tree
     * so it isn't recomputed per contentlet.
     */
    isDevMode: boolean;
    /**
     * Whether dotCMS Analytics is active. Resolved once at the layout root (a
     * single `dotcms:analytics:ready` listener) and shared with the tree.
     */
    isAnalyticsActive: boolean;
}

/**
 * Injection key for the dotCMS page context.
 *
 * The value is a `ComputedRef` so that live UVE updates to the page (a new page
 * asset arriving via `uve-set-page-data`) propagate to every consumer that reads
 * `ctx.value` — the whole layout tree re-renders reactively.
 *
 * Advanced: exported so consumers can `inject` directly, but prefer
 * {@link useDotCMSPageContext}.
 */
export const DOTCMS_PAGE_CONTEXT: InjectionKey<ComputedRef<DotCMSPageContextValue>> =
    Symbol('DotCMSPageContext');

/**
 * Provides the dotCMS page context to descendant components.
 *
 * Advanced: {@link DotCMSLayoutBody} already provides this for the rendered tree,
 * so most applications never call it. Exposed for consumers that render the
 * layout tree themselves.
 *
 * @param value a computed producing the current context value
 */
export function provideDotCMSPageContext(value: ComputedRef<DotCMSPageContextValue>): void {
    provide(DOTCMS_PAGE_CONTEXT, value);
}

const EMPTY_CONTEXT: DotCMSPageContextValue = {
    pageAsset: {} as DotCMSPageAsset,
    mode: 'production',
    userComponents: {},
    isDevMode: false,
    isAnalyticsActive: false
};

/**
 * Injects the dotCMS page context as a reactive `ComputedRef`.
 *
 * Public: use this from a custom content-type component to read the current
 * `pageAsset`, renderer `mode`, or editor state without prop drilling. Falls back
 * to an empty context when used outside of a {@link DotCMSLayoutBody}.
 *
 * @returns the current page context value as a computed ref
 */
export function useDotCMSPageContext(): ComputedRef<DotCMSPageContextValue> {
    return inject(
        DOTCMS_PAGE_CONTEXT,
        computed(() => EMPTY_CONTEXT)
    );
}
