import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';

import { CustomClientParams } from '@dotcms/client';

import { UVEState } from '../../models';

/**
 * Client request properties
 *
 * @export
 * @interface ClientRequestProps
 */
export interface ClientRequestProps {
    params?: CustomClientParams;
    query?: string;
}

/**
 * Client configuration state
 *
 * @export
 * @interface ClientConfigState
 */
export interface ClientConfigState {
    isClientReady: boolean;
    clientRequestProps: ClientRequestProps;
}

const initialState: ClientConfigState = {
    isClientReady: false,
    clientRequestProps: {
        params: null,
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
        withMethods((store) => {
            return {
                setIsClientReady: (isClientReady: boolean) => {
                    patchState(store, { isClientReady });
                },
                setClientConfiguration: ({ query, params }: ClientRequestProps) => {
                    patchState(store, {
                        clientRequestProps: {
                            query,
                            params
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
