'use client';

import { ReactNode, createContext } from 'react';

import { DotCMSBasicContentlet, DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

/**
 * @internal
 *
 * Props for the DotCMSPageContext
 * @interface DotCMSPageContextProps
 * @property {DotCMSPageAsset} pageAsset - The DotCMS page asset
 * @property {RendererMode} mode - The renderer mode
 * @property {Record<string, React.ComponentType<DotCMSContentlet>>} userComponents - The user components
 * @property {Record<string, ReactNode>} [slots] - Pre-rendered server component nodes keyed by contentlet identifier
 */
export interface DotCMSPageContextProps {
    /**
     * Can be `undefined` while `useEditableDotCMSPage` is still waiting on the UVE editor to
     * resolve a draft/non-live page, or when permissions leave it unset outside the editor.
     */
    pageAsset: DotCMSPageAsset | undefined;
    mode: DotCMSPageRendererMode;
    userComponents: Record<string, React.ComponentType<DotCMSBasicContentlet>>;
    slots?: Record<string, ReactNode>;
    /**
     * Whether editor metadata (`data-dot-*` attributes, placeholders, fallbacks) should be
     * emitted. Resolved once at the layout root and shared with the whole tree so it isn't
     * recomputed by every container and contentlet.
     */
    isDevMode: boolean;
    /**
     * Whether dotCMS Analytics is active. Resolved once at the layout root — a single
     * `dotcms:analytics:ready` listener for the tree instead of one per contentlet.
     */
    isAnalyticsActive: boolean;
}

/**
 * The `PageContext` is a React context that provides access to the DotCMS page context.
 *
 * @category Contexts
 */
export const DotCMSPageContext = createContext<DotCMSPageContextProps>({
    pageAsset: undefined,
    mode: 'production',
    userComponents: {},
    slots: {},
    isDevMode: false,
    isAnalyticsActive: false
});
