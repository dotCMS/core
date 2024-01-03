import { createContext } from 'react';

import { PageProviderContext } from '../components/PageProvider/PageProvider';

export const PageContext = createContext<PageProviderContext | null>(null);
