import { createContext } from 'react';

import { DotCMSContentlet, DotCMSPageAsset } from '../types';

export type RendererMode = 'production' | 'development';

export interface DotCMSPageContextProps {
    pageAsset: DotCMSPageAsset;
    mode: RendererMode;
    userComponents?: Record<string, React.ComponentType<DotCMSContentlet>>;
}

/**
 * The `PageContext` is a React context that provides access to the DotCMS page context.
 *
 * @category Contexts
 */
export const DotCMSPageContext = createContext<DotCMSPageContextProps>({
    pageAsset: {} as DotCMSPageAsset,
    mode: 'production',
    userComponents: {}
});
