import { useContext } from 'react';

import { PageProviderContext, PageContext } from '../components/PageProvider/PageProvider';

export function useDotcmsPageContext() {
    return useContext<PageProviderContext>(PageContext);
}
