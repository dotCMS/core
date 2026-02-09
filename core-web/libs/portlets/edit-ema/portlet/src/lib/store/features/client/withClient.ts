import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSPageAsset } from '@dotcms/types';

import { PERSONA_KEY } from '../../../shared/consts';
import { UVEState } from '../../models';

/**
 * Client configuration state
 *
 * @export
 * @interface ClientConfigState
 */
export interface ClientConfigState {
    legacyGraphqlResponse: boolean;
    isClientReady: boolean;
    graphql: {
        query: string;
        variables: Record<string, string>;
    };
    graphqlResponse: {
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
    };
}

const clientState: ClientConfigState = {
    graphql: null,
    graphqlResponse: null,
    isClientReady: false,
    legacyGraphqlResponse: false
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
                setCustomGraphQL: ({ query, variables }, legacyGraphqlResponse) => {
                    patchState(store, {
                        legacyGraphqlResponse,
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

                    // Old customers using graphQL expect only the page.
                    // We can remove this once we are in stable and tell the devs this won't work in new dotCMS versions.
                    if (store.legacyGraphqlResponse()) {
                        return store.graphqlResponse()?.pageAsset;
                    }

                    return {
                        ...store.graphqlResponse(),
                        grapql: store.graphql()
                    };
                }),
                $graphqlWithParams: computed(() => {
                    if (!store.graphql()) {
                        return null;
                    }

                    const params = store.pageParams();
                    const { mode, language_id, url, variantName } = params;

                    return {
                        ...store.graphql(),
                        variables: {
                            ...store.graphql().variables,
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
