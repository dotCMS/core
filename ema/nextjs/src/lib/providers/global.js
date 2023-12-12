'use client';

import React, { createContext } from 'react';

export const GlobalContext = createContext({});

export default function PageProvider({ children, entity }) {
    return <GlobalContext.Provider value={entity}>{children}</GlobalContext.Provider>;
}
