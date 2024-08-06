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

export interface clientRequestProps {
    params: Partial<DotPageApiParams>;
    query: string;
}

export interface ClientConfigState {
    isClientReady: boolean;
    clientRequestProps: clientRequestProps;
}

const initialState: ClientConfigState = {
    isClientReady: false,
    clientRequestProps: {
        params: {},
        query: ''
    }
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
        withState<ClientConfigState>(initialState),
        withComputed((store) => {
            return {
                $clientRequestProps: computed(() => {
                    const { query, params } = store.clientRequestProps();

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
                setClientConfiguration: (clientRequestProps: Partial<clientRequestProps>) => {
                    patchState(store, {
                        clientRequestProps: {
                            ...store.clientRequestProps(),
                            ...clientRequestProps
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
