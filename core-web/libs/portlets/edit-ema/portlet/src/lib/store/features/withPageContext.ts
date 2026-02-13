import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal, untracked } from '@angular/core';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { normalizeQueryParams } from '../../utils';
import { TranslateProps, UVEState } from '../models';

import type { PageAssetComputed } from './client/withClient';

/**
 * Phase 6.2: Reduced scope - lock computeds moved to withWorkflow
 */
export interface PageContextComputed {
    viewMode: Signal<UVE_MODE>;

    // NOTE: Phase 6.2 - workflowIsPageLocked moved to withWorkflow
    // NOTE: Phase 6.2 - systemIsLockFeatureEnabled moved to withWorkflow

    pageLanguageId: Signal<number>;
    pageLanguage: Signal<any>;
    pageURI: Signal<string>;
    pageVariantId: Signal<string>;
    pageTranslateProps: Signal<TranslateProps>;
    pageFriendlyParams: Signal<Record<string, string>>;
}

export function withPageContext() {
    return signalStoreFeature(
        // Note: This feature requires withClient to be composed before it
        {
            state: type<UVEState>(),
            props: type<PageAssetComputed>()
        },
        withComputed(
            (store) => {
                // ============ View Domain ============
                const viewMode = computed(() => store.pageParams()?.mode ?? UVE_MODE.UNKNOWN);

                // ============ Page Domain ============
                const pageTranslateProps = computed<TranslateProps>(() => {
                    const pageData = store.pageData();
                    const viewAsData = store.pageViewAs();
                    const languageId = viewAsData?.language?.id;
                    const translatedLanguages = untracked(() => store.languages());
                    const currentLanguage = translatedLanguages.find(
                        (lang) => lang.id === languageId
                    );

                    return {
                        page: pageData,
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

                const pageLanguageId = computed(() => store.pageViewAs()?.language?.id || 1);
                const pageLanguage = computed(() => store.pageViewAs()?.language);
                const pageURI = computed(() => store.pageData()?.pageURI ?? '');
                const pageVariantId = computed(() => store.pageParams()?.variantName ?? DEFAULT_VARIANT_ID);

                return {
                    // View
                    viewMode,
                    // Page
                    pageLanguageId,
                    pageLanguage,
                    pageURI,
                    pageVariantId,
                    pageTranslateProps,
                    pageFriendlyParams
                } satisfies PageContextComputed;
            }
        )
    );
}
