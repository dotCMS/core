import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal, untracked } from '@angular/core';

import { DEFAULT_VARIANT_ID, DotExperimentStatus } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { computeIsPageLocked, normalizeQueryParams } from '../../utils';
import { PageType, TranslateProps, UVEState } from '../models';

import type { PageAssetComputed } from './withPageAsset';

/**
 * Shared computed properties available to all features in the UVE store.
 *
 * Properties use domain prefixes for clear ownership and better discoverability:
 * - view* - View mode and display state
 * - editor* - Editor capabilities and permissions
 * - workflow* - Workflow and lock status
 * - page* - Page context and metadata
 * - system* - System flags and enterprise features
 *
 * @remarks
 * Type store.editor to see all editor-related APIs grouped together in IntelliSense.
 * Type store.page to see all page-related APIs grouped together.
 *
 * @example
 * // ✅ CORRECT: Use domain-prefixed computed signals
 * export function withMyFeature() {
 *   return signalStoreFeature(
 *     { state: type<UVEState>(), props: type<PageContextComputed>() },
 *     withComputed((store) => ({
 *       myComputed: computed(() => {
 *         return store.editorCanEditContent() && someCondition;
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

    // ============ Editor Domain ============

    /**
     * Whether the user can edit page content right now.
     * Combines access permissions with current mode being EDIT.
     */
    editorCanEditContent: Signal<boolean>;

    /**
     * Whether the user can edit page layout right now.
     * Checks template permissions, experiments, lock status, and edit mode.
     */
    editorCanEditLayout: Signal<boolean>;

    /**
     * Whether the user can edit styles right now.
     * Requires style editor feature flag, headless page, permissions, and edit mode.
     */
    editorCanEditStyles: Signal<boolean>;

    /**
     * Whether inline editing is enabled for the current page.
     * Requires editor in edit state and enterprise license.
     */
    editorEnableInlineEdit: Signal<boolean>;

    /**
     * Whether the current user has access to edit mode.
     * Checks page permissions, experiments, and lock status.
     */
    editorHasAccessToEditMode: Signal<boolean>;

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
 * A single feature that starts organizing the store by grouping the common
 * computed signals shared across the other signal features.
 *
 * @remarks Dev team note: keep any new shared computeds inside this feature so
 * everything stays discoverable in one place.
 *
 * @export
 */
export function withPageContext() {
    return signalStoreFeature(
        // Note: This feature requires withPageAsset to be composed before it
        {
            state: type<UVEState>(),
            props: type<PageAssetComputed>()
        },
        withComputed(
            (store) => {
                // Access computed signals from withPageAsset (already available via props)
                // Type system knows these exist because of props: type<PageAssetComputed>()

                // ============ View Domain ============
                const viewMode = computed(() => store.pageParams()?.mode ?? UVE_MODE.UNKNOWN);

                // ============ System Domain ============
                const systemIsLockFeatureEnabled = computed(() => store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK);
                const styleEditorFeatureEnabled = computed(() => {
                    const isHeadless = store.pageType() === PageType.HEADLESS;
                    return store.flags().FEATURE_FLAG_UVE_STYLE_EDITOR && isHeadless;
                });

                // ============ Workflow Domain ============
                const workflowIsPageLocked = computed(() => {
                    return computeIsPageLocked(store.pageData(), store.currentUser());
                });

                // ============ Editor Domain - Permissions ============
                const editorHasAccessToEditMode = computed(() => {
                    const isPageEditable = store.pageData()?.canEdit;
                    const isExperimentRunning = [
                        DotExperimentStatus.RUNNING,
                        DotExperimentStatus.SCHEDULED
                    ].includes(store.experiment()?.status);

                    if (!isPageEditable || isExperimentRunning) {
                        return false;
                    }

                    // When feature flag is enabled, always allow access (user can toggle lock)
                    if (systemIsLockFeatureEnabled()) {
                        return true;
                    }

                    // Legacy behavior: block access if page is locked
                    return !workflowIsPageLocked();
                });

                const hasPermissionToEditLayout = computed(() => {
                    const canEditPage = store.pageData()?.canEdit;
                    const canDrawTemplate = store.pageTemplate()?.drawed;
                    const isExperimentRunning = [
                        DotExperimentStatus.RUNNING,
                        DotExperimentStatus.SCHEDULED
                    ].includes(store.experiment()?.status);

                    return (canEditPage || canDrawTemplate) && !isExperimentRunning && !workflowIsPageLocked();
                });

                const hasPermissionToEditStyles = computed(() => {
                    const canEditPage = store.pageData()?.canEdit;
                    const isExperimentRunning = [
                        DotExperimentStatus.RUNNING,
                        DotExperimentStatus.SCHEDULED
                    ].includes(store.experiment()?.status);

                    return canEditPage && !isExperimentRunning && !workflowIsPageLocked();
                });

                // ============ Editor Domain - Capabilities ============
                const editorCanEditContent = computed(() => {
                    return editorHasAccessToEditMode() && viewMode() === UVE_MODE.EDIT;
                });

                const editorCanEditLayout = computed(() => {
                    return hasPermissionToEditLayout() && viewMode() === UVE_MODE.EDIT;
                });

                const editorCanEditStyles = computed(() => {
                    return styleEditorFeatureEnabled() && hasPermissionToEditStyles() && viewMode() === UVE_MODE.EDIT;
                });

                const editorEnableInlineEdit = computed(() => {
                    return store.viewIsEditState() && store.isEnterprise();
                });

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

                    // ============ Editor Domain ============
                    editorCanEditContent,
                    editorCanEditLayout,
                    editorCanEditStyles,
                    editorEnableInlineEdit,
                    editorHasAccessToEditMode,

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
