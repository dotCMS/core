import { useContext } from 'react';

import { PageProviderContext, PageContext } from '../components/page-provider/page-provider';

export function useDotcmsPageContext() {
    return useContext<PageProviderContext>(PageContext);
}
