import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal, untracked } from '@angular/core';

import { DEFAULT_VARIANT_ID, DotExperimentStatus } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { computeIsPageLocked, normalizeQueryParams } from '../../utils';
import { PageType, TranslateProps, UVEState } from '../models';

import type { PageAssetComputed } from './client/withClient';

/**
 * Shared cross-cutting computed properties available to all features in the UVE store.
 *
 * Phase 6.2: Editor computeds moved to withEditor - this now contains ONLY truly
 * cross-cutting concerns that don't belong to a specific domain.
 *
 * Properties use domain prefixes for clear ownership:
 * - view* - View mode and display state
 * - workflow* - Workflow and lock status
 * - page* - Page context and metadata
 * - system* - System flags and enterprise features
 *
 * @remarks
 * Type store.page to see all page-related APIs grouped together.
 * For editor APIs, see withEditor feature.
 *
 * @example
 * // ✅ CORRECT: Use domain-prefixed computed signals
 * export function withMyFeature() {
 *   return signalStoreFeature(
 *     { state: type<UVEState>(), props: type<PageContextComputed>() },
 *     withComputed((store) => ({
 *       myComputed: computed(() => {
 *         return store.viewMode() === UVE_MODE.EDIT && someCondition;
 *       })
 *     }))
 *   );
 * }
 */
export interface PageContextComputed {
    // ============ View Domain ============

    /**
     * Current UVE mode (EDIT, PREVIEW, LIVE, or UNKNOWN).
     * Single source of truth for which mode the editor is in.
     */
    viewMode: Signal<UVE_MODE>;

    // ============ Workflow Domain ============

    /**
     * Whether the current page is locked by any user.
     * Single source of truth for page lock status.
     */
    workflowIsPageLocked: Signal<boolean>;

    // ============ Page Domain ============

    /**
     * Current language ID for the page.
     */
    pageLanguageId: Signal<number>;

    /**
     * Current language object for the page.
     */
    pageLanguage: Signal<any>;

    /**
     * Current page URI.
     */
    pageURI: Signal<string>;

    /**
     * Current variant ID for the page.
     */
    pageVariantId: Signal<string>;

    /**
     * Translation properties for the current page and language.
     */
    pageTranslateProps: Signal<TranslateProps>;

    /**
     * Normalized friendly parameters combining pageParams and viewParams.
     */
    pageFriendlyParams: Signal<Record<string, string>>;

    // ============ System Domain ============

    /**
     * Whether the toggle lock feature flag is enabled.
     * Controls new toggle lock UI vs. old unlock button.
     */
    systemIsLockFeatureEnabled: Signal<boolean>;
}

/**
 * Cross-cutting computed properties shared across multiple features.
 *
 * Phase 6.2: Reduced scope - editor computeds moved to withEditor.
 * This feature now contains ONLY truly cross-cutting concerns:
 * - View mode (used by editor, workflow, etc.)
 * - Page lock status (used by editor, workflow, etc.)
 * - Page metadata (language, URI, variant, etc.)
 * - System flags
 *
 * @remarks Dev team note: Keep only cross-cutting computeds here.
 * Domain-specific logic belongs in domain features (editor, workflow, etc.)
 *
 * @export
 */
export function withPageContext() {
    return signalStoreFeature(
        // Note: This feature requires withClient to be composed before it
        {
            state: type<UVEState>(),
            props: type<PageAssetComputed>()
        },
        withComputed(
            (store) => {
                // Access computed signals from withClient (already available via props)
                // Type system knows these exist because of props: type<PageAssetComputed>()

                // ============ View Domain ============
                const viewMode = computed(() => store.pageParams()?.mode ?? UVE_MODE.UNKNOWN);

                // ============ Workflow Domain ============
                const workflowIsPageLocked = computed(() => {
                    return computeIsPageLocked(store.pageData(), store.currentUser());
                });

                // ============ System Domain ============
                const systemIsLockFeatureEnabled = computed(() => store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK);

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
                    // ============ View Domain ============
                    viewMode,

                    // ============ Workflow Domain ============
                    workflowIsPageLocked,

                    // ============ Page Domain ============
                    pageLanguageId,
                    pageLanguage,
                    pageURI,
                    pageVariantId,
                    pageTranslateProps,
                    pageFriendlyParams,

                    // ============ System Domain ============
                    systemIsLockFeatureEnabled
                } satisfies PageContextComputed;
            }
        )
    );
}
