import { createContext, useContext } from 'react';

import { DotCMSBasicContentlet } from '@dotcms/types';

import { useAISearch } from '../hooks/useAISearch';
import { DotCMSAISearchProviderProps, DotCMSAISearchValue } from '../shared/types';

/**
 * The `DotCMSAISearchContext` provides access to AI search functionality.
 *
 * @category <DotCMSAISearchContext>Context</DotCMSAISearchContext>
 */
const DotCMSAISearchContext = createContext<DotCMSAISearchValue<DotCMSBasicContentlet> | undefined>(
    undefined
);

/**
 * Provider component for DotCMS AI Search
 *
 * @example
 * ```tsx
 * <DotCMSAISearchProvider
 *   client={client}
 *   indexName="my-index"
 *   params={{ limit: 10 }}
 * >
 *   <MyComponent />
 * </DotCMSAISearchProvider>
 * ```
 *
 * @category Contexts
 */
export function DotCMSAISearchProvider<T extends DotCMSBasicContentlet>({
    client,
    indexName,
    params,
    children
}: DotCMSAISearchProviderProps) {
    const searchState = useAISearch<T>({ client, indexName, params });

    return (
        <DotCMSAISearchContext.Provider value={searchState}>
            {children}
        </DotCMSAISearchContext.Provider>
    );
}

/**
 * Hook to access the DotCMS AI Search context
 *
 * @throws {Error} If used outside of DotCMSAISearchProvider
 *
 * @example
 * ```tsx
 * const { results, status, search, reset } = useDotCMSAISearch();
 *
 * const handleSearch = () => {
 *   search('my search term');
 * };
 *
 * if (status.state === 'loading') return <div>Loading...</div>;
 * if (status.state === 'error') return <div>Error: {status.error.message}</div>;
 * ```
 *
 * @category Contexts
 */
export function useDotCMSAISearchContext<
    T extends DotCMSBasicContentlet
>(): DotCMSAISearchValue<T> {
    const context = useContext(DotCMSAISearchContext);

    if (!context) {
        throw new Error('useDotCMSAISearchContext must be used within a DotCMSAISearchProvider');
    }

    return context as DotCMSAISearchValue<T>;
}
