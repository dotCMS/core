import { createContext } from 'react';

import { DotCMSBasicContentlet, DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

/**
 * @internal
 *
 * Props for the DotCMSPageContext
 * @interface DotCMSPageContextProps
 * @property {DotCMSPageAsset} pageAsset - The DotCMS page asset
 * @property {RendererMode} mode - The renderer mode
 * @property {Record<string, React.ComponentType<DotCMSContentlet>>} userComponents - The user components
 */
export interface DotCMSPageContextProps {
    pageAsset: DotCMSPageAsset;
    mode: DotCMSPageRendererMode;
    userComponents: Record<string, React.ComponentType<DotCMSBasicContentlet>>;
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
