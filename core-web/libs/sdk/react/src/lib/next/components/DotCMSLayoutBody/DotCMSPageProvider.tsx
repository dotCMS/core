'use client';

import { ReactNode, useMemo } from 'react';

import { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { useIsAnalyticsActive } from '../../hooks/useIsAnalyticsActive';
import { useResolvedDevMode } from '../../hooks/useIsDevMode';

interface DotCMSPageProviderProps {
    page: DotCMSPageAsset | undefined;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    components: Record<string, React.ComponentType<any>>;
    mode: DotCMSPageRendererMode;
    slots?: Record<string, ReactNode>;
    children: ReactNode;
}

/**
 * @internal
 *
 * Client boundary that provides the DotCMS page context to the layout tree.
 * Keeping this separate from DotCMSLayoutBody allows the layout to remain
 * a server component while only the context provider runs on the client.
 *
 * Development mode and the Analytics-active flag are resolved here, once per layout tree,
 * and shared through the context. Resolving them per contentlet meant one
 * `dotcms:analytics:ready` window listener and one UVE-state lookup for every piece of
 * content on the page.
 */
export function DotCMSPageProvider({
    page,
    components,
    mode,
    slots,
    children
}: DotCMSPageProviderProps) {
    const isDevMode = useResolvedDevMode(mode);
    const isAnalyticsActive = useIsAnalyticsActive();

    const value = useMemo(
        () => ({
            pageAsset: page,
            userComponents: components,
            mode,
            slots,
            isDevMode,
            isAnalyticsActive
        }),
        [page, components, mode, slots, isDevMode, isAnalyticsActive]
    );

    return <DotCMSPageContext.Provider value={value}>{children}</DotCMSPageContext.Provider>;
}
