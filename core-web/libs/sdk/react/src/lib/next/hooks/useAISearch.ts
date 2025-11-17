import { useCallback, useReducer } from 'react';

import { DotCMSAISearchResponse, DotCMSBasicContentlet, DotCMSEntityStatus } from '@dotcms/types';

import { DotCMSAISearchProps, DotCMSAISearchValue } from '../shared/types';

type State<T extends DotCMSBasicContentlet> = {
    response: DotCMSAISearchResponse<T> | null;
    status: DotCMSEntityStatus;
};

type Action<T extends DotCMSBasicContentlet> =
    | { type: 'LOADING' }
    | { type: 'SUCCESS'; payload: DotCMSAISearchResponse<T> }
    | { type: 'ERROR'; payload: Error }
    | { type: 'RESET' };

function reducer<T extends DotCMSBasicContentlet>(state: State<T>, action: Action<T>): State<T> {
    switch (action.type) {
        case 'LOADING':
            return { ...state, status: { state: 'loading' } };
        case 'SUCCESS':
            return { response: action.payload, status: { state: 'success' } };
        case 'ERROR':
            return { ...state, status: { state: 'error', error: action.payload } };
        case 'RESET':
            return { response: null, status: { state: 'idle' } };
        default:
            return state;
    }
}

export const useAISearch = <T extends DotCMSBasicContentlet>({
    client,
    indexName,
    params
}: DotCMSAISearchProps): DotCMSAISearchValue<T> => {
    const [state, dispatch] = useReducer(reducer<T>, {
        response: null,
        status: { state: 'idle' }
    });

    const search = useCallback(
        async (prompt: string) => {
            if (!prompt.trim()) {
                return;
            }

            dispatch({ type: 'LOADING' });
            try {
                const response = await client.ai.search<T>(prompt, indexName, {
                    ...params
                });
                dispatch({ type: 'SUCCESS', payload: response });
            } catch (error) {
                dispatch({ type: 'ERROR', payload: error as Error });
            }
        },
        [client, indexName, params]
    );

    const reset = useCallback(() => {
        dispatch({ type: 'RESET' });
    }, []);

    return {
        response: state.response,
        results: state.response?.dotCMSResults,
        status: state.status,
        search,
        reset
    };
};
