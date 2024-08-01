import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';

import { UVEState } from '../../models';

export interface ClientConfigState {
    isClientReady: boolean;
    graphQL: string;
}

const initialState: ClientConfigState = {
    isClientReady: false,
    graphQL: ''
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
        withMethods((store) => {
            return {
                setIsClientReady: (isClientReady: boolean) => {
                    patchState(store, { isClientReady });
                },
                setClientConfiguration: (graphQL: string) => {
                    patchState(store, { graphQL });
                },
                resetClientConfiguration: () => {
                    patchState(store, { ...initialState });
                }
            };
        })
    );
}
