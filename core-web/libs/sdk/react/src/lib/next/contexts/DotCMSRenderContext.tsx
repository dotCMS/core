import { createContext } from 'react';

import { DotCMSContentlet, DotCMSPageAsset } from '../types';

export interface DotCMSRenderContextI {
    dotCMSPageAsset: DotCMSPageAsset;
    customComponents?: Record<string, React.ComponentType<DotCMSContentlet>>;
    isDevMode: boolean;
}

/**
 * The `PageContext` is a React context that provides access to the DotCMS page context.
 *
 * @category Contexts
 */
export const DotCMSRenderContext = createContext<DotCMSRenderContextI | null>(null);
