'use client';

import { ReactNode } from 'react';

import { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';

interface DotCMSPageProviderProps {
    page: DotCMSPageAsset;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    components: Record<string, React.ComponentType<any>>;
    mode: DotCMSPageRendererMode;
    slots: Record<string, ReactNode>;
    children: ReactNode;
}

/**
 * @internal
 *
 * Client boundary that provides the DotCMS page context to the layout tree.
 * Keeping this separate from DotCMSLayoutBody allows the layout to remain
 * a server component while only the context provider runs on the client.
 */
export function DotCMSPageProvider({ page, components, mode, slots, children }: DotCMSPageProviderProps) {
    return (
        <DotCMSPageContext.Provider value={{ pageAsset: page, userComponents: components, mode, slots }}>
            {children}
        </DotCMSPageContext.Provider>
    );
}
