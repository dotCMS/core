import { useCallback, useReducer } from 'react';

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

export const useAISearch = <T extends DotCMSBasicContentlet>({
    client,
    indexName,
    params
}: DotCMSAISearchProps): DotCMSAISearchValue<T> => {
    const [state, dispatch] = useReducer(reducer<T>, {
        response: null,
        status: { state: DotCMSEntityState.IDLE }
    });

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
                    ...params
                });
                dispatch({ type: DotCMSEntityState.SUCCESS, payload: response });
            } catch (error) {
                dispatch({ type: DotCMSEntityState.ERROR, payload: error as Error });
            }
        },
        [client, indexName, params]
    );

    return {
        response: state.response,
        results: state.response?.results,
        status: state.status,
        search,
        reset
    };
};
