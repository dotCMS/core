import { useContext } from 'react';

import { PageProviderContext } from '../components/PageProvider/PageProvider';
import { PageContext } from '../contexts/PageContext';

/**
 * Hook to get the page context
 *
 * @category Hooks
 * @export
 * @return {*}
 */
export function useDotcmsPageContext() {
    return useContext<PageProviderContext | null>(PageContext);
}
