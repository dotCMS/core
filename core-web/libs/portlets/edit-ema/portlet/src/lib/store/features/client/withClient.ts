import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import { DotCMSLayout, DotCMSPageAsset } from '@dotcms/types';

import { PERSONA_KEY } from '../../../shared/consts';
import { UVEState } from '../../models';
import { withTimeMachine } from '../timeMachine/withTimeMachine';

/**
 * Client configuration state
 *
 * @export
 * @interface ClientConfigState
 */
export interface ClientConfigState {
    legacyResponseFormat: boolean;
    isClientReady: boolean;
    requestMetadata: {
        query: string;
        variables: Record<string, string>;
    };
    pageAssetResponse: {
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
    };
}

/**
 * Interface defining the state, methods, and computed properties provided by withClient
 * Use this as props type in dependent features
 *
 * @export
 * @interface WithClientMethods
 */
export interface WithClientMethods {
    // State (added via withState, available as signals on the store)
    requestMetadata: () => { query: string; variables: Record<string, string> } | null;
    pageAssetResponse: () => { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> } | null;
    isClientReady: () => boolean;
    legacyResponseFormat: () => boolean;

    // Computed: layout from pageAssetResponse.pageAsset (single source of truth)
    layout: () => DotCMSLayout | null;

    // Methods
    setIsClientReady: (isClientReady: boolean) => void;
    setCustomClient: (requestMetadata: { query: string; variables: Record<string, string> }, legacyResponseFormat: boolean) => void;
    setPageAssetResponse: (pageAssetResponse: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    resetClientConfiguration: () => void;

    // Computed
    $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null>;
}

const clientState: ClientConfigState = {
    requestMetadata: null,
    pageAssetResponse: null,
    isClientReady: false,
    legacyResponseFormat: false
};

/**
 * Add methods to handle client configuration
 * The Client represents the page host. In the context of self-hosted pages, dotCMS acts as the client.
 *
 * @description This feature is used to handle the client configuration
 * @export
 * @return {*}
 */
export function withClient() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<ClientConfigState>(clientState),
        // Add time machine to track pageAssetResponse history for optimistic updates
        withTimeMachine<ClientConfigState['pageAssetResponse']>({
            maxHistory: 50, // Reasonable limit for style editor undo
            deepClone: true // Important: pageAssetResponse has nested objects
        }),
        withMethods((store) => {
            return {
                setIsClientReady: (isClientReady: boolean) => {
                    patchState(store, { isClientReady });
                },
                setCustomClient: ({ query, variables }, legacyResponseFormat) => {
                    patchState(store, {
                        legacyResponseFormat,
                        requestMetadata: {
                            query,
                            variables
                        }
                    });
                },
                setPageAssetResponse: (pageAssetResponse) => {
                    patchState(store, { pageAssetResponse });
                },
                /**
                 * Sets pageAssetResponse optimistically by saving current state to history first.
                 * Used for optimistic updates that can be rolled back on failure.
                 *
                 * @param pageAssetResponse - The new page asset response to set
                 */
                setPageAssetResponseOptimistic: (
                    pageAssetResponse: ClientConfigState['pageAssetResponse']
                ) => {
                    const currentResponse = store.pageAssetResponse();
                    // Save snapshot before updating (for optimistic updates rollback)
                    if (currentResponse) {
                        store.addHistory(currentResponse);
                    }
                    patchState(store, { pageAssetResponse });
                },
                /**
                 * Rolls back to the previous pageAssetResponse state.
                 * Used when an optimistic update fails.
                 *
                 * @returns true if rollback was successful, false if no history available
                 */
                rollbackPageAssetResponse: (): boolean => {
                    const previousState = store.undo();
                    if (previousState !== null) {
                        patchState(store, { pageAssetResponse: previousState });
                        return true;
                    }
                    return false;
                },
                resetClientConfiguration: () => {
                    patchState(store, { ...clientState });
                    store.clearHistory();
                }
            };
        }),
        withComputed((store) => {
            return {
                layout: computed<DotCMSLayout | null>(
                    () => store.pageAssetResponse()?.pageAsset?.layout ?? null
                ),
                $requestWithParams: computed(() => {
                    if (!store.requestMetadata()) {
                        return null;
                    }

                    const params = store.pageParams();
                    const { mode, language_id, url, variantName } = params;

                    return {
                        ...store.requestMetadata(),
                        variables: {
                            ...store.requestMetadata().variables,
                            url,
                            mode,
                            languageId: language_id,
                            personaId: params[PERSONA_KEY],
                            variantName
                        }
                    };
                })
            };
        })
    );
}
