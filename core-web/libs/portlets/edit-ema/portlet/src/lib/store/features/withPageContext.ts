import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { withFlags } from './flags/withFlags';

import { UVE_FEATURE_FLAGS } from '../../shared/consts';
import { computeIsPageLocked } from '../../utils';
import { UVEState } from '../models';

export interface PageContextComputed {
    // Note: page, site, viewAs, template, layout, urlContentMap, containers, vanityUrl
    // are now direct state properties (Signal<any>) available on the store, not computed

    // Computed properties
    $isEditMode: Signal<boolean>;
    $isPageLocked: Signal<boolean>;
    $isLockFeatureEnabled: Signal<boolean>;
    $isStyleEditorEnabled: Signal<boolean>;
    $hasAccessToEditMode: Signal<boolean>;
    $languageId: Signal<number>;
    $currentLanguage: Signal<any>;
    $isPreviewMode: Signal<boolean>;
    $isLiveMode: Signal<boolean>;
    $pageURI: Signal<string>;
    $variantId: Signal<string>;
    $canEditPage: Signal<boolean>;
    $canEditLayout: Signal<boolean>;
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
        withFlags(UVE_FEATURE_FLAGS),
        withComputed(
            ({
                page,
                viewAs,
                template,
                pageParams,
                flags,
                experiment,
                currentUser,
                isTraditionalPage
            }) => {
                // Note: page, site, viewAs, template, layout, urlContentMap, containers, vanityUrl
                // are now direct state properties, passed through as-is

                const $isPreviewMode = computed(() => pageParams()?.mode === UVE_MODE.PREVIEW);
                const $isLiveMode = computed(() => pageParams()?.mode === UVE_MODE.LIVE);
                const $isEditMode = computed(() => pageParams()?.mode === UVE_MODE.EDIT);
                const $isLockFeatureEnabled = computed(() => flags().FEATURE_FLAG_UVE_TOGGLE_LOCK);
                const $isStyleEditorEnabled = computed(() => {
                    const isHeadless = !isTraditionalPage();
                    return flags().FEATURE_FLAG_UVE_STYLE_EDITOR && isHeadless;
                });
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

                return {
                    // State properties (passed through as-is - already in state)
                    // page, site, viewAs, template, layout, urlContentMap, containers, vanityUrl
                    // are available directly from store state, no need to re-export

                    // Existing computed properties
                    $isLiveMode,
                    $isEditMode,
                    $isPreviewMode,
                    $isPageLocked,
                    $isLockFeatureEnabled,
                    $isStyleEditorEnabled,
                    $hasAccessToEditMode,
                    $languageId: computed(() => viewAs()?.language?.id || 1),
                    $currentLanguage: computed(() => viewAs()?.language),
                    $pageURI: computed(() => page()?.pageURI ?? ''),
                    $variantId: computed(() => pageParams()?.variantId ?? ''),
                    $canEditPage: computed(() => $hasAccessToEditMode() && $isEditMode()),
                    $canEditLayout: computed(() => {
                        const pageData = page();
                        const templateData = template();
                        return pageData?.canEdit || templateData?.drawed;
                    })
                } satisfies PageContextComputed;
            }
        )
    );
}
