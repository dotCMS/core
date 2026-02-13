import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, Signal, untracked } from '@angular/core';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
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
import { normalizeQueryParams } from '../../../utils';
import { TranslateProps, UVEState } from '../../models';
import { withTimeMachine } from '../timeMachine/withTimeMachine';

/**
 * Page loading configuration state
 *
 * Manages the configuration and state for loading page data from the server.
 * This includes request metadata (query/variables), response format, and the loaded page asset.
 *
 * @export
 * @interface PageLoadingConfigState
 */
export interface PageLoadingConfigState {
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

export interface PageAssetComputed {
    // Phase 6.3: Merged from withPageContext - all page-related computeds in one place
    pageData: Signal<DotCMSPage | null>;
    pageSite: Signal<DotCMSSite | null>;

    pageContainers: Signal<DotCMSPageAssetContainers | null>;

    pageTemplate: Signal<DotCMSTemplate | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'> | null>;

    pageLayout: Signal<DotCMSLayout | null>;

    pageViewAs: Signal<DotCMSViewAs | null>;

    pageVanityUrl: Signal<DotCMSVanityUrl | null>;

    pageUrlContentMap: Signal<DotCMSURLContentMap | null>;

    pageNumberContents: Signal<number | null>;

    pageClientResponse: Signal<any>;

    // Page context properties (merged from withPageContext)
    pageLanguageId: Signal<number>;
    pageLanguage: Signal<any>;
    pageURI: Signal<string>;
    pageVariantId: Signal<string>;
    pageTranslateProps: Signal<TranslateProps>;
    pageFriendlyParams: Signal<Record<string, string>>;
}

/**
 * Complete interface for withPage feature
 * Combines page data access, page loading configuration, and page operations
 */
export interface WithPageMethods extends PageAssetComputed {
    // Page loading configuration state
    requestMetadata: () => { query: string; variables: Record<string, string> } | null;
    pageAssetResponse: () => { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> } | null;
    isClientReady: () => boolean;
    legacyResponseFormat: () => boolean;

    // Page loading configuration methods
    setIsClientReady: (isClientReady: boolean) => void;
    setCustomClient: (requestMetadata: { query: string; variables: Record<string, string> }, legacyResponseFormat: boolean) => void;
    setPageAssetResponse: (pageAssetResponse: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    resetClientConfiguration: () => void;

    // Computed
    $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null>;
}

const pageLoadingConfigState: PageLoadingConfigState = {
    requestMetadata: null,
    pageAssetResponse: null,
    isClientReady: false,
    legacyResponseFormat: false
};

/**
 * Page data and loading configuration feature
 *
 * Manages all page-related data and the configuration for loading pages from the server.
 * This is the primary feature for accessing page information throughout the UVE store.
 *
 * Responsibilities:
 * - Page asset data (page, site, containers, template, layout, viewAs, etc.)
 * - Page context (language, URI, variant, translation props, friendly params)
 * - Page loading configuration (request metadata, response format, client readiness)
 * - Time machine for undo/redo of page changes
 *
 * @returns Signal store feature with page data and loading configuration
 */
export function withPage() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<PageLoadingConfigState>(pageLoadingConfigState),
        // Add time machine to track pageAssetResponse history for optimistic updates
        withTimeMachine<PageLoadingConfigState['pageAssetResponse']>({
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
                setPageAssetResponseOptimistic: (
                    pageAssetResponse: PageLoadingConfigState['pageAssetResponse']
                ) => {
                    const currentResponse = store.pageAssetResponse();
                    // Save snapshot before updating (for optimistic updates rollback)
                    if (currentResponse) {
                        store.addHistory(currentResponse);
                    }
                    patchState(store, { pageAssetResponse });
                },
                rollbackPageAssetResponse: (): boolean => {
                    const previousState = store.undo();
                    if (previousState !== null) {
                        patchState(store, { pageAssetResponse: previousState });
                        return true;
                    }
                    return false;
                },
                resetClientConfiguration: () => {
                    patchState(store, { ...pageLoadingConfigState });
                    store.clearHistory();
                }
            };
        }),
        withComputed((store) => {
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

            // Phase 6.3: Page context computeds (merged from withPageContext)
            const pageLanguageId = computed(() => pageViewAs()?.language?.id || 1);
            const pageLanguage = computed(() => pageViewAs()?.language);
            const pageURI = computed(() => pageData()?.pageURI ?? '');
            const pageVariantId = computed(() => store.pageParams()?.variantName ?? DEFAULT_VARIANT_ID);

            const pageTranslateProps = computed<TranslateProps>(() => {
                const pageDataValue = pageData();
                const viewAsData = pageViewAs();
                const languageId = viewAsData?.language?.id;
                const translatedLanguages = untracked(() => store.languages());
                const currentLanguage = translatedLanguages.find(
                    (lang) => lang.id === languageId
                );

                return {
                    page: pageDataValue,
                    currentLanguage
                };
            });

            const pageFriendlyParams = computed(() => {
                const params = {
                    ...(store.pageParams() ?? {}),
                    ...(store.viewParams() ?? {})
                };

                return normalizeQueryParams(params);
            });

            return {
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
                $requestWithParams,
                // Page context computeds
                pageLanguageId,
                pageLanguage,
                pageURI,
                pageVariantId,
                pageTranslateProps,
                pageFriendlyParams
            } satisfies Omit<PageAssetComputed, 'pageLayout'> & { pageLayout: Signal<DotCMSLayout | null>, $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null> };
        })
    );
}
