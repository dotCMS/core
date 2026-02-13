import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, Signal, untracked } from '@angular/core';

import { DEFAULT_VARIANT_ID, DotLanguage } from '@dotcms/dotcms-models';
import { DotCMSPage, DotCMSPageAsset } from '@dotcms/types';

import { PERSONA_KEY } from '../../../shared/consts';
import { normalizeQueryParams } from '../../../utils';
import { TranslateProps, UVEState } from '../../models';
import { withHistory } from '../history/withHistory';

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

export type PageClientResponse =
    | DotCMSPageAsset
    | {
          pageAsset: DotCMSPageAsset;
          content?: Record<string, unknown>;
          requestMetadata: { query: string; variables: Record<string, string> } | null;
      };

export type PageSnapshot = (DotCMSPageAsset & {
    content?: Record<string, unknown>;
    requestMetadata?: { query: string; variables: Record<string, string> } | null;
    clientResponse?: PageClientResponse | null;
}) | null;

export interface PageComputed {
    page: Signal<PageSnapshot>;

    // Page context properties (merged from withPageContext)
    pageLanguageId: Signal<number>;
    pageLanguage: Signal<DotLanguage | undefined>;
    pageURI: Signal<string>;
    pageVariantId: Signal<string>;
    pageTranslateProps: Signal<TranslateProps>;
    pageFriendlyParams: Signal<Record<string, string>>;
}

/**
 * Complete interface for withPage feature
 * Combines page data access, page loading configuration, and page operations
 */
export interface WithPageMethods extends PageComputed {
    // Page loading configuration state
    requestMetadata: () => { query: string; variables: Record<string, string> } | null;
    pageAssetResponse: () => { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> } | null;
    isClientReady: () => boolean;
    legacyResponseFormat: () => boolean;

    // Page loading configuration methods
    setIsClientReady: (isClientReady: boolean) => void;
    setCustomClient: (requestMetadata: { query: string; variables: Record<string, string> }, legacyResponseFormat: boolean) => void;
    setPageAssetResponse: (pageAssetResponse: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    setPageAsset: (pageAsset: DotCMSPageAsset) => void;
    resetClientConfiguration: () => void;
    addCurrentPageToHistory: () => void;
    resetHistoryToCurrent: () => void;

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
        // Add history tracking for undo/redo of page changes (style editor, layout, etc.)
        withHistory<PageLoadingConfigState['pageAssetResponse']>({
            selector: (store) => store.pageAssetResponse(),
            maxHistory: 50, // Reasonable limit for undo operations
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
                setPageAsset: (pageAsset) => {
                    const currentResponse = store.pageAssetResponse();
                    const nextResponse = currentResponse
                        ? {
                            ...currentResponse,
                            pageAsset
                        }
                        : { pageAsset };
                    patchState(store, { pageAssetResponse: nextResponse });
                },
                setPageAssetResponseOptimistic: (
                    pageAssetResponse: PageLoadingConfigState['pageAssetResponse']
                ) => {
                    const currentResponse = store.pageAssetResponse();
                    // Save snapshot before updating (for optimistic updates rollback)
                    if (currentResponse) {
                        store.addToHistory(currentResponse);
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
                },
                addCurrentPageToHistory: () => {
                    const currentResponse = store.pageAssetResponse();
                    if (currentResponse) {
                        store.addToHistory(currentResponse);
                    }
                },
                resetHistoryToCurrent: () => {
                    const currentResponse = store.pageAssetResponse();
                    if (currentResponse) {
                        store.clearHistory();
                        store.addToHistory(currentResponse);
                    } else {
                        store.clearHistory();
                    }
                }
            };
        }),
        withComputed((store) => {
            const page = computed<PageSnapshot>(() => {
                const response = store.pageAssetResponse();
                if (!response) {
                    return null;
                }

                const asset = response.pageAsset;
                const requestMetadata = store.requestMetadata();
                const clientResponse = store.legacyResponseFormat()
                    ? asset
                    : {
                        ...response,
                        requestMetadata
                    };

                return {
                    ...asset,
                    content: response.content,
                    requestMetadata,
                    clientResponse
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

            const pageLanguageId = computed(() => page()?.viewAs?.language?.id || 1);
            const pageLanguage = computed(() => page()?.viewAs?.language);
            const pageURI = computed(() => page()?.page?.pageURI ?? '');
            const pageVariantId = computed(() => store.pageParams()?.variantName ?? DEFAULT_VARIANT_ID);

            const pageTranslateProps = computed<TranslateProps>(() => {
                const pageDataValue = page()?.page as DotCMSPage;
                const viewAsData = page()?.viewAs;
                const languageId = viewAsData?.language?.id;
                const translatedLanguages = untracked(() => store.pageLanguages());
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
                page,
                $requestWithParams,
                // Page context computeds
                pageLanguageId,
                pageLanguage,
                pageURI,
                pageVariantId,
                pageTranslateProps,
                pageFriendlyParams
            } satisfies PageComputed & { $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null> };
        })
    );
}
