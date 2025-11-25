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
