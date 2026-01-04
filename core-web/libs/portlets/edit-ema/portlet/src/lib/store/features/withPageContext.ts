import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { computeIsPageLocked } from '../../utils';
import { PageType, UVEState } from '../models';

export interface PageContextComputed {
    // Note: page, site, viewAs, template, layout, urlContentMap, containers, vanityUrl
    // are now direct state properties (Signal<any>) available on the store, not computed

    // Mode state (single source of truth)
    $mode: Signal<UVE_MODE>;

    // Permission signals (used by components)
    $isLockFeatureEnabled: Signal<boolean>;
    $hasAccessToEditMode: Signal<boolean>;

    // Capability signals (used by components)
    $canEditPageContent: Signal<boolean>;
    $canEditLayout: Signal<boolean>;
    $canEditStyles: Signal<boolean>;

    // Other computed properties
    $languageId: Signal<number>;
    $currentLanguage: Signal<any>;
    $pageURI: Signal<string>;
    $variantId: Signal<string>;
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
                pageType
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

                return {
                    // ============ Mode State ============
                    $mode,

                    // ============ Permission Signals (Used by Components) ============
                    $isLockFeatureEnabled,
                    $hasAccessToEditMode,

                    // ============ Capability Signals (Used by Components) ============
                    $canEditPageContent,
                    $canEditLayout,
                    $canEditStyles,

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
