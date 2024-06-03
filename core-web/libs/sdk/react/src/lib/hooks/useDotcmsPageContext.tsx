import { useContext } from 'react';

import { DotCMSPageContext } from '../components/PageProvider/PageProvider';
import { PageContext } from '../contexts/PageContext';

/**
 * `useDotcmsPageContext` is a custom React hook that provides access to the `PageProviderContext`.
 * It takes no parameters and returns the context value or `null` if it's not available.
 *
 * @category Hooks
 * @returns {DotCMSPageContext | null} - The context value or `null` if it's not available.
 */
export function useDotcmsPageContext() {
    return useContext<DotCMSPageContext | null>(PageContext);
}
