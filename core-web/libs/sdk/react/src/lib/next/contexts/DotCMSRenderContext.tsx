import { createContext } from 'react';
import { DotCMSPageAsset } from '../types';

export interface DotCMSRenderContextI {
    dotCMSPageAsset: DotCMSPageAsset;
    customComponents?: Record<string, React.ComponentType<any>>;
    isDevMode: boolean;
}

/**
 * The `PageContext` is a React context that provides access to the DotCMS page context.
 *
 * @category Contexts
 */
export const DotCMSRenderContext = createContext<DotCMSRenderContextI | null>(null);
