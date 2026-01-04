import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { computeIsPageLocked } from '../../utils';
import { PageType, UVEState } from '../models';

/**
 * Shared computed properties available to all features in the UVE store.
 *
 * These properties form the "public API" that other features can safely depend on.
 * Features should use these shared computeds instead of duplicating logic or accessing
 * raw state directly.
 *
 * @remarks
 * IMPORTANT: Properties in this interface are safe to access from other features.
 * They represent cross-cutting concerns that multiple features need.
 *
 * When adding new features that need cross-feature data, prefer:
 * 1. Adding to this shared contract if truly needed by multiple features
 * 2. Using explicit dependency injection via factory parameters (WithXxxDeps interfaces)
 *
 * @example
 * // ✅ CORRECT: Access shared computed from feature
 * export function withMyFeature() {
 *   return signalStoreFeature(
 *     { state: type<UVEState>(), props: type<PageContextComputed>() },
 *     withComputed((store) => ({
 *       myComputed: computed(() => {
 *         return store.$canEditPageContent() && someCondition;
 *       })
 *     }))
 *   );
 * }
 *
 * @example
 * // ✅ CORRECT: Pass as explicit dependency
 * export interface WithMyFeatureDeps {
 *   $isPageLocked: () => boolean;
 * }
 * export function withMyFeature(deps: WithMyFeatureDeps) {
 *   // Use deps.$isPageLocked()
 * }
 */
export interface PageContextComputed {
    // Note: page, site, viewAs, template, layout, urlContentMap, containers, vanityUrl
    // are now direct state properties (Signal<any>) available on the store, not computed

    // ============ Mode State (Single Source of Truth) ============

    /**
     * Current UVE mode (EDIT, PREVIEW, LIVE, or UNKNOWN).
     *
     * This is the single source of truth for which mode the editor is in.
     * Features should use this instead of accessing pageParams.mode directly.
     *
     * @public Shared API - safe for all features to access
     */
    $mode: Signal<UVE_MODE>;

    // ============ Permission Signals (Used by Components & Features) ============

    /**
     * Whether the toggle lock feature flag is enabled.
     *
     * Controls whether the new toggle lock UI is shown vs. old unlock button.
     *
     * @public Shared API - safe for all features to access
     */
    $isLockFeatureEnabled: Signal<boolean>;

    /**
     * Whether the current page is locked (by any user, including current user).
     *
     * This is the single source of truth for page lock status. Use this instead
     * of recalculating with computeIsPageLocked().
     *
     * @public Shared API - safe for all features to access
     * @see computeIsPageLocked for the underlying computation logic
     */
    $isPageLocked: Signal<boolean>;

    /**
     * Whether the current user has access to edit mode.
     *
     * Takes into account:
     * - Page edit permissions (canEdit)
     * - Running/scheduled experiments (blocks editing)
     * - Page lock status
     *
     * @public Shared API - safe for all features to access
     */
    $hasAccessToEditMode: Signal<boolean>;

    // ============ Capability Signals (Used by Editor Features) ============

    /**
     * Whether the user can edit page content right now.
     *
     * Combines hasAccessToEditMode with current mode being EDIT.
     * Used by editor features to enable/disable contentlet editing.
     *
     * @public Shared API - safe for all features to access
     */
    $canEditPageContent: Signal<boolean>;

    /**
     * Whether the user can edit page layout right now.
     *
     * Takes into account:
     * - Edit or draw template permissions
     * - Running/scheduled experiments
     * - Page lock status
     * - Current mode being EDIT
     *
     * @public Shared API - safe for all features to access
     */
    $canEditLayout: Signal<boolean>;

    /**
     * Whether the user can edit styles right now.
     *
     * Requires:
     * - Style editor feature flag enabled
     * - Headless page type
     * - Page edit permissions
     * - No running/scheduled experiments
     * - Page not locked
     * - Current mode being EDIT
     *
     * @public Shared API - safe for all features to access
     */
    $canEditStyles: Signal<boolean>;

    // ============ Context Properties ============

    /**
     * Current language ID for the page.
     *
     * @public Shared API - safe for all features to access
     */
    $languageId: Signal<number>;

    /**
     * Current language object for the page.
     *
     * @public Shared API - safe for all features to access
     */
    $currentLanguage: Signal<any>;

    /**
     * Current page URI.
     *
     * @public Shared API - safe for all features to access
     */
    $pageURI: Signal<string>;

    /**
     * Current variant ID for the page.
     *
     * @public Shared API - safe for all features to access
     */
    $variantId: Signal<string>;

    /**
     * Whether inline editing is enabled for the current page.
     *
     * Requires:
     * - Editor is in edit state (not device/SEO preview)
     * - Enterprise license is active
     *
     * Used by editor components to enable/disable inline editing features.
     *
     * @public Shared API - safe for all features to access
     */
    $enableInlineEdit: Signal<boolean>;
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
        { state: type<UVEState>() },
        withComputed(
            ({
                page,
                viewAs,
                template,
                pageParams,
                flags,
                experiment,
                currentUser,
                pageType,
                view,
                isEnterprise
            }) => {
                // Note: page, site, viewAs, template, layout, urlContentMap, containers, vanityUrl
                // are now direct state properties, passed through as-is

                // ============ Mode State (Single Source of Truth) ============
                const $mode = computed(() => pageParams()?.mode ?? UVE_MODE.UNKNOWN);

                // ============ Feature Flags ============
                const $isLockFeatureEnabled = computed(() => flags().FEATURE_FLAG_UVE_TOGGLE_LOCK);
                const $styleEditorFeatureEnabled = computed(() => {
                    const isHeadless = pageType() === PageType.HEADLESS;
                    return flags().FEATURE_FLAG_UVE_STYLE_EDITOR && isHeadless;
                });

                // ============ Permission Signals ============
                const $isPageLocked = computed(() => {
                    return computeIsPageLocked(page(), currentUser(), $isLockFeatureEnabled());
                });

                const $hasAccessToEditMode = computed(() => {
                    const isPageEditable = page()?.canEdit;
                    const isExperimentRunning = [
                        DotExperimentStatus.RUNNING,
                        DotExperimentStatus.SCHEDULED
                    ].includes(experiment()?.status);
                    return isPageEditable && !isExperimentRunning && !$isPageLocked();
                });

                const $hasPermissionToEditLayout = computed(() => {
                    const canEditPage = page()?.canEdit;
                    const canDrawTemplate = template()?.drawed;
                    const isExperimentRunning = [
                        DotExperimentStatus.RUNNING,
                        DotExperimentStatus.SCHEDULED
                    ].includes(experiment()?.status);

                    return (canEditPage || canDrawTemplate) && !isExperimentRunning && !$isPageLocked();
                });

                const $hasPermissionToEditStyles = computed(() => {
                    const canEditPage = page()?.canEdit;
                    const isExperimentRunning = [
                        DotExperimentStatus.RUNNING,
                        DotExperimentStatus.SCHEDULED
                    ].includes(experiment()?.status);

                    return canEditPage && !isExperimentRunning && !$isPageLocked();
                });

                // ============ Capability Signals ============
                const $canEditPageContent = computed(() => {
                    return $hasAccessToEditMode() && $mode() === UVE_MODE.EDIT;
                });

                const $canEditLayout = computed(() => {
                    return $hasPermissionToEditLayout() && $mode() === UVE_MODE.EDIT;
                });

                const $canEditStyles = computed(() => {
                    return $styleEditorFeatureEnabled() && $hasPermissionToEditStyles() && $mode() === UVE_MODE.EDIT;
                });

                const $enableInlineEdit = computed(() => {
                    return view().isEditState && isEnterprise();
                });

                return {
                    // ============ Mode State ============
                    $mode,

                    // ============ Permission Signals (Used by Components) ============
                    $isLockFeatureEnabled,
                    $isPageLocked,
                    $hasAccessToEditMode,

                    // ============ Capability Signals (Used by Components) ============
                    $canEditPageContent,
                    $canEditLayout,
                    $canEditStyles,
                    $enableInlineEdit,

                    // ============ Other Computed Properties ============
                    $languageId: computed(() => viewAs()?.language?.id || 1),
                    $currentLanguage: computed(() => viewAs()?.language),
                    $pageURI: computed(() => page()?.pageURI ?? ''),
                    $variantId: computed(() => pageParams()?.variantId ?? '')
                } satisfies PageContextComputed;
            }
        )
    );
}
