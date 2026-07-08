import { inject, provide, type InjectionKey, type Component } from 'vue';

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
 * @internal
 */
export const DOTCMS_PAGE_CONTEXT: InjectionKey<DotCMSPageContextValue> =
    Symbol('DotCMSPageContext');

/**
 * Provides the dotCMS page context to descendant components.
 *
 * @internal
 * @param value the context value to expose to children
 */
export function provideDotCMSPageContext(value: DotCMSPageContextValue): void {
    provide(DOTCMS_PAGE_CONTEXT, value);
}

/**
 * Injects the dotCMS page context.
 *
 * Falls back to an empty context when used outside of a {@link DotCMSLayoutBody},
 * mirroring the default value the React context is created with.
 *
 * @internal
 * @returns the current page context value
 */
export function useDotCMSPageContext(): DotCMSPageContextValue {
    return inject(DOTCMS_PAGE_CONTEXT, {
        pageAsset: {} as DotCMSPageAsset,
        mode: 'production',
        userComponents: {}
    });
}
