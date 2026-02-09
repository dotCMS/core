import { useCallback, useReducer, useRef, useEffect } from 'react';

import {
    DotCMSAISearchResponse,
    DotCMSBasicContentlet,
    DotCMSEntityState,
    DotCMSEntityStatus
} from '@dotcms/types';

import { DotCMSAISearchProps, DotCMSAISearchValue } from '../shared/types';

type State<T extends DotCMSBasicContentlet> = {
    response: DotCMSAISearchResponse<T> | null;
    status: DotCMSEntityStatus;
};

type Action<T extends DotCMSBasicContentlet> =
    | { type: typeof DotCMSEntityState.LOADING }
    | { type: typeof DotCMSEntityState.SUCCESS; payload: DotCMSAISearchResponse<T> }
    | { type: typeof DotCMSEntityState.ERROR; payload: Error }
    | { type: typeof DotCMSEntityState.IDLE };

function reducer<T extends DotCMSBasicContentlet>(state: State<T>, action: Action<T>): State<T> {
    switch (action.type) {
        case DotCMSEntityState.LOADING:
            return { ...state, status: { state: DotCMSEntityState.LOADING } };
        case DotCMSEntityState.SUCCESS:
            return { response: action.payload, status: { state: DotCMSEntityState.SUCCESS } };
        case DotCMSEntityState.ERROR:
            return { ...state, status: { state: DotCMSEntityState.ERROR, error: action.payload } };
        case DotCMSEntityState.IDLE:
            return { response: null, status: { state: DotCMSEntityState.IDLE } };
        default:
            return state;
    }
}

/**
 * Hook to search for contentlets using AI.
 * @template T - The type of the contentlet.
 * @param client - The client to use for the search.
 * @param indexName - The name of the index to search in.
 * @param params - The parameters for the search.
 * @returns The search results.
 *
 * @example
 * ```typescript
 * const { results, status, search, reset } = useAISearch<BlogPost>({
 *   client: dotCMSClient,
 *   indexName: 'blog-search-index',
 *   params: {
 *     query: {
 *       limit: 10,
 *       offset: 0,
 *       contentType: 'Blog'
 *     },
 *     config: {
 *       threshold: 0.5,
 *       responseLength: 1024
 *     }
 *   }
 * });
 * ```
 */
export const useAISearch = <T extends DotCMSBasicContentlet>({
    client,
    indexName,
    params
}: DotCMSAISearchProps): DotCMSAISearchValue<T> => {
    const [state, dispatch] = useReducer(reducer<T>, {
        response: null,
        status: { state: DotCMSEntityState.IDLE }
    });

    // Use ref to store params so search callback doesn't change when params change
    const paramsRef = useRef(params);

    // Keep ref updated with latest params
    useEffect(() => {
        paramsRef.current = params;
    }, [params]);

    const reset = useCallback(() => {
        dispatch({ type: DotCMSEntityState.IDLE });
    }, []);

    const search = useCallback(
        async (prompt: string) => {
            if (!prompt.trim()) {
                dispatch({ type: DotCMSEntityState.IDLE });
                return;
            }

            dispatch({ type: DotCMSEntityState.LOADING });
            try {
                const response = await client.ai.search<T>(prompt, indexName, {
                    ...paramsRef.current
                });
                dispatch({ type: DotCMSEntityState.SUCCESS, payload: response });
            } catch (error) {
                dispatch({ type: DotCMSEntityState.ERROR, payload: error as Error });
            }
        },
        [client, indexName]
    );

    return {
        response: state.response,
        results: state.response?.results ?? [],
        status: state.status,
        search,
        reset
    };
};
