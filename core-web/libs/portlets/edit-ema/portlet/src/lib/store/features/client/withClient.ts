import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotPageApiResponse } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { UVEState } from '../../models';

/**
 * Client configuration state
 *
 * @export
 * @interface ClientConfigState
 */
export interface ClientConfigState {
    shouldReturnFullGraphqlResponse: boolean;
    isClientReady: boolean;
    graphql: {
        query: string;
        variables: Record<string, string>;
    };
    graphqlResponse: {
        page: DotPageApiResponse;
        content?: Record<string, unknown>;
    };
}

const clientState: ClientConfigState = {
    graphql: null,
    graphqlResponse: null,
    isClientReady: false,
    shouldReturnFullGraphqlResponse: false
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
        withMethods((store) => {
            return {
                setIsClientReady: (isClientReady: boolean) => {
                    patchState(store, { isClientReady });
                },
                setCustomGraphQL: ({ query, variables }, shouldReturnFullGraphqlResponse) => {
                    patchState(store, {
                        isClientReady: true,
                        shouldReturnFullGraphqlResponse,
                        graphql: {
                            query,
                            variables
                        }
                    });
                },
                setGraphqlResponse: (graphqlResponse) => {
                    patchState(store, { graphqlResponse });
                },
                resetClientConfiguration: () => {
                    patchState(store, { ...clientState });
                }
            };
        }),
        withComputed((store) => {
            return {
                $customGraphqlResponse: computed(() => {
                    if (!store.graphqlResponse()) {
                        return null;
                    }

                    if (store.shouldReturnFullGraphqlResponse()) {
                        return {
                            ...store.graphqlResponse(),
                            grapql: store.graphql()
                        };
                    }

                    return store.graphqlResponse()?.page;
                }),
                $graphql: computed(() => {
                    if (!store.graphql()) {
                        return null;
                    }

                    const params = store.pageParams();
                    const { mode, language_id, url } = params;

                    return {
                        ...store.graphql(),
                        variables: {
                            ...store.graphql().variables,
                            url,
                            mode,
                            languageId: language_id,
                            personaId: params[PERSONA_KEY]
                        }
                    };
                })
            };
        })
    );
}
