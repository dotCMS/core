import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import {
    DotCMSLayout,
    DotCMSPage,
    DotCMSPageAsset,
    DotCMSPageAssetContainers,
    DotCMSSite,
    DotCMSTemplate,
    DotCMSURLContentMap,
    DotCMSVanityUrl,
    DotCMSViewAs
} from '@dotcms/types';

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
 * Computed signals for accessing pageAsset properties.
 *
 * Phase 6.1: Merged from withPageAsset - eliminates duplicate feature.
 * This interface provides structured access to pageAsset data from the client state.
 * All properties use 'page*' prefix for clear domain ownership.
 *
 * @remarks
 * All page-related APIs use 'page*' prefix for better discoverability.
 * Type store.page to see all page-related APIs grouped together in IntelliSense.
 *
 * @example
 * // ✅ CORRECT: Use domain-prefixed computed signals
 * const page = store.pageData();
 * const site = store.pageSite();
 * const containers = store.pageContainers();
 *
 * @public Shared API - safe for all features to access
 */
export interface PageAssetComputed {
    /**
     * Current page data from pageAsset.
     * Provides access to page properties like title, identifier, canEdit, etc.
     */
    pageData: Signal<DotCMSPage | null>;

    /**
     * Current site data from pageAsset.
     * Provides access to site properties like identifier, hostname, etc.
     */
    pageSite: Signal<DotCMSSite | null>;

    /**
     * Containers structure from pageAsset.
     * Maps container identifiers to their structure and contentlets.
     */
    pageContainers: Signal<DotCMSPageAssetContainers | null>;

    /**
     * Template data from pageAsset.
     * Provides access to template properties like title, layout, drawed, etc.
     */
    pageTemplate: Signal<DotCMSTemplate | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'> | null>;

    /**
     * Layout data from pageAsset.
     * Provides access to layout structure (rows, columns, containers).
     */
    pageLayout: Signal<DotCMSLayout | null>;

    /**
     * ViewAs configuration from pageAsset.
     * Contains language, persona, and visitor context.
     */
    pageViewAs: Signal<DotCMSViewAs | null>;

    /**
     * Vanity URL data from pageAsset.
     * Contains vanity URL configuration if applicable.
     */
    pageVanityUrl: Signal<DotCMSVanityUrl | null>;

    /**
     * URL content map from pageAsset.
     * Maps URL paths to contentlets.
     */
    pageUrlContentMap: Signal<DotCMSURLContentMap | null>;

    /**
     * Number of contentlets on the page.
     * Used for determining if content deletion is allowed.
     */
    pageNumberContents: Signal<number | null>;

    /**
     * Complete client response data.
     * Provides full response including pageAsset, content, and request metadata.
     *
     * Modes:
     * - Legacy mode: pageAsset only (for old clients)
     * - Modern mode: { pageAsset, content, requestMetadata }
     */
    pageClientResponse: Signal<any>;
}

/**
 * Interface defining the state, methods, and computed properties provided by withClient
 * Use this as props type in dependent features
 *
 * @export
 * @interface WithClientMethods
 */
export interface WithClientMethods extends PageAssetComputed {
    // State (added via withState, available as signals on the store)
    requestMetadata: () => { query: string; variables: Record<string, string> } | null;
    pageAssetResponse: () => { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> } | null;
    isClientReady: () => boolean;
    legacyResponseFormat: () => boolean;

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
 * Client configuration and page asset data management.
 *
 * Phase 6.1: Consolidated withPageAsset into withClient to eliminate duplication.
 * The Client represents the page host. In the context of self-hosted pages, dotCMS acts as the client.
 *
 * This feature provides:
 * - Client configuration state (requestMetadata, legacyResponseFormat)
 * - PageAsset response storage with time machine (undo/redo)
 * - All pageAsset computed properties (page, site, containers, template, etc.)
 *
 * @description Single source of truth for client configuration and page data
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
            // ============ PageAsset Properties (Single Source of Truth) ============
            // Phase 6.1: Moved from withPageAsset to eliminate duplicate feature
            const pageData = computed<DotCMSPage | null>(
                () => store.pageAssetResponse()?.pageAsset?.page ?? null
            );

            const pageSite = computed<DotCMSSite | null>(
                () => store.pageAssetResponse()?.pageAsset?.site ?? null
            );

            const pageContainers = computed<DotCMSPageAssetContainers | null>(
                () => store.pageAssetResponse()?.pageAsset?.containers ?? null
            );

            const pageTemplate = computed<DotCMSTemplate | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'> | null>(
                () => store.pageAssetResponse()?.pageAsset?.template ?? null
            );

            const pageLayout = computed<DotCMSLayout | null>(
                () => store.pageAssetResponse()?.pageAsset?.layout ?? null
            );

            const pageViewAs = computed<DotCMSViewAs | null>(
                () => store.pageAssetResponse()?.pageAsset?.viewAs ?? null
            );

            const pageVanityUrl = computed<DotCMSVanityUrl | null>(
                () => store.pageAssetResponse()?.pageAsset?.vanityUrl ?? null
            );

            const pageUrlContentMap = computed<DotCMSURLContentMap | null>(
                () => store.pageAssetResponse()?.pageAsset?.urlContentMap ?? null
            );

            const pageNumberContents = computed<number | null>(
                () => store.pageAssetResponse()?.pageAsset?.numberContents ?? null
            );

            // ============ Client Response (External Integration) ============
            const pageClientResponse = computed(() => {
                if (!store.pageAssetResponse()) {
                    return null;
                }

                // Old customers using graphQL expect only the page.
                // We can remove this once we are in stable and tell the devs this won't work in new dotCMS versions.
                if (store.legacyResponseFormat()) {
                    return store.pageAssetResponse()?.pageAsset;
                }

                return {
                    ...store.pageAssetResponse(),
                    requestMetadata: store.requestMetadata()
                };
            });

            const $requestWithParams = computed(() => {
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
            });

            return {
                // PageAsset computed properties
                pageData,
                pageSite,
                pageContainers,
                pageTemplate,
                pageLayout,
                pageViewAs,
                pageVanityUrl,
                pageUrlContentMap,
                pageNumberContents,
                pageClientResponse,
                // Client computed properties
                $requestWithParams
            } satisfies Omit<PageAssetComputed, 'pageLayout'> & { pageLayout: Signal<DotCMSLayout | null>, $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null> };
        })
    );
}
