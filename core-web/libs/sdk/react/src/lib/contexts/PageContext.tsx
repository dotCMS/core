import { createContext } from 'react';

import { DotCMSPageContext } from '../components/PageProvider/PageProvider';

export const PageContext = createContext<DotCMSPageContext | null>(null);
