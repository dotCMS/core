import { ReactNode } from 'react';

import { createDotCMSClient } from '@dotcms/client';
import {
    DotCMSAISearchContentletData,
    DotCMSAISearchParams,
    DotCMSAISearchResponse,
    DotCMSBasicContentlet,
    DotCMSEntityStatus
} from '@dotcms/types';

/**
 * Return type of the AI Search context
 * @interface DotCMSAISearchContextValue
 * @template T - The content type extending DotCMSBasicContentlet
 */
export interface DotCMSAISearchValue<T extends DotCMSBasicContentlet> {
    response: DotCMSAISearchResponse<T> | null;
    results: DotCMSAISearchContentletData<T>[] | undefined;
    status: DotCMSEntityStatus;
    search: (prompt: string) => Promise<void>;
    reset: () => void;
}

/**
 * Props for the DotCMSAISearchProvider
 * @interface DotCMSAISearchProviderProps
 * @template T - The content type extending DotCMSBasicContentlet
 */
export interface DotCMSAISearchProps {
    client: ReturnType<typeof createDotCMSClient>;
    indexName: string;
    params: DotCMSAISearchParams;
}

/**
 * Props for the DotCMSAISearchProvider
 * @interface DotCMSAISearchProviderProps
 * @template T - The content type extending DotCMSBasicContentlet
 */
export interface DotCMSAISearchProviderProps extends DotCMSAISearchProps {
    children: ReactNode;
}

/**
 * Render prop function parameters for DotCMSAISearchInput
 * @interface DotCMSAISearchInputRenderProps
 */
export interface DotCMSAISearchInputRenderProps {
    /**
     * Function to trigger a search with a prompt
     */
    search: (prompt: string) => Promise<void>;
    /**
     * Function to reset the search
     */
    reset: () => void;
}

/**
 * Props for the DotCMSAISearchInput component
 * @interface DotCMSAISearchInputProps
 */
export interface DotCMSAISearchInputProps {
    /**
     * Optional render prop function for custom input implementations
     * If not provided, renders a default input with search button
     */
    children?: (props: DotCMSAISearchInputRenderProps) => ReactNode;
    /**
     * Optional callback fired when search is triggered (default input only)
     */
    onSearch?: (prompt: string) => void;
    /**
     * Optional callback fired when reset is triggered (default input only)
     */
    onReset?: () => void;
    /**
     * Optional placeholder text for default input
     */
    placeholder?: string;
    /**
     * Optional CSS class for default input container
     */
    className?: string;
}
