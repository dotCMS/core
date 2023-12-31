import { useContext } from 'react';

import { PageProviderContext } from '../components/PageProvider/PageProvider';
import { PageContext } from '../contexts/PageContext';

export function useDotcmsPageContext() {
    return useContext<PageProviderContext>(PageContext);
}
