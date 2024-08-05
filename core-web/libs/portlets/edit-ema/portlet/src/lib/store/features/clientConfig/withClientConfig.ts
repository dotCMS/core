import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { UVEState } from '../../models';

export interface ClientCustomPayload {
    params: Partial<DotPageApiParams>;
    query: string;
}

export interface ClientConfigState {
    isClientReady: boolean;
    clientCustomPayload: ClientCustomPayload;
}

const initialState: ClientConfigState = {
    isClientReady: false,
    clientCustomPayload: {
        params: {},
        query: ''
    }
};

/**
 * Add computed properties and methods to the store to handle the Editor Toolbar UI
 *
 * @export
 * @return {*}
 */
export function withClientConfig() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<ClientConfigState>(initialState),
        withComputed((store) => {
            return {
                $clientPayload: computed(() => {
                    const { query, params } = store.clientCustomPayload();

                    return {
                        query,
                        params: {
                            ...store.params(),
                            ...params
                        }
                    };
                })
            };
        }),
        withMethods((store) => {
            return {
                setIsClientReady: (isClientReady: boolean) => {
                    patchState(store, { isClientReady });
                },
                setClientConfiguration: (customPayload: Partial<ClientCustomPayload>) => {
                    patchState(store, {
                        clientCustomPayload: {
                            ...store.clientCustomPayload(),
                            ...customPayload
                        }
                    });
                },
                resetClientConfiguration: () => {
                    patchState(store, { ...initialState });
                }
            };
        })
    );
}
