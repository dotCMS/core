import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { withFlags } from './flags/withFlags';

import { UVE_FEATURE_FLAGS } from '../../shared/consts';
import { computeIsPageLocked } from '../../utils';
import { UVEState } from '../models';

export interface PageContextComputed {
    $isEditMode: Signal<boolean>;
    $isPageLocked: Signal<boolean>;
    $isLockFeatureEnabled: Signal<boolean>;
    $isStyleEditorEnabled: Signal<boolean>;
    $hasAccessToEditMode: Signal<boolean>;
    $languageId: Signal<number>;
    $isPreviewMode: Signal<boolean>;
    $isLiveMode: Signal<boolean>;
    $pageURI: Signal<string>;
    $variantId: Signal<string | undefined>;
    $canEditPage: Signal<boolean>;
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
                pageAPIResponse,
                pageParams,
                flags,
                experiment,
                currentUser,
                isTraditionalPage
            }) => {
                const page = computed(() => pageAPIResponse()?.page);
                const viewAs = computed(() => pageAPIResponse()?.viewAs);
                const $isPreviewMode = computed(() => pageParams()?.mode === UVE_MODE.PREVIEW);
                const $isLiveMode = computed(() => pageParams()?.mode === UVE_MODE.LIVE);
                const $isEditMode = computed(() => pageParams()?.mode === UVE_MODE.EDIT);
                const $isLockFeatureEnabled = computed(() => flags().FEATURE_FLAG_UVE_TOGGLE_LOCK);
                const $isStyleEditorEnabled = computed(() => {
                    const isHeadless = !isTraditionalPage();
                    return flags().FEATURE_FLAG_UVE_STYLE_EDITOR && isHeadless;
                });
                const $isPageLocked = computed(() => {
                    return computeIsPageLocked(page(), currentUser());
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
                    $isLiveMode,
                    $isEditMode,
                    $isPreviewMode,
                    $isPageLocked,
                    $isLockFeatureEnabled,
                    $isStyleEditorEnabled,
                    $hasAccessToEditMode,
                    $languageId: computed(() => viewAs()?.language?.id || 1),
                    $pageURI: computed(() => page()?.pageURI ?? ''),
                    $variantId: computed(() => pageParams()?.variantId ?? undefined), // Passing undefined because sending an empty string causes API errors.
                    $canEditPage: computed(() => $hasAccessToEditMode() && $isEditMode())
                } satisfies PageContextComputed;
            }
        )
    );
}
