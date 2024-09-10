import { createContext } from 'react';

import { DotCMSPageContext } from '../models';

/**
 * The `PageContext` is a React context that provides access to the DotCMS page context.
 *
 * @category Contexts
 */
export const PageContext = createContext<DotCMSPageContext | null>(null);
