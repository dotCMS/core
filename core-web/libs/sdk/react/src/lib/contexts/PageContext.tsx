import { createContext } from 'react';

import { DotCMSPageContext } from '../models';

export const PageContext = createContext<DotCMSPageContext | null>(null);
