import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { withFlags } from './flags/withFlags';

import { UVE_FEATURE_FLAGS } from '../../shared/consts';
import { computePageIsLocked } from '../../utils';
import { UVEState } from '../models';

export interface PageContextComputed {
    $isEditMode: Signal<boolean>;
    $pageIsLocked: Signal<boolean>;
    $isLockFeatureEnabled: Signal<boolean>;
    $hasAccessToEditMode: Signal<boolean>;
    $languageId: Signal<number>;
    $isPreviewMode: Signal<boolean>;
    $isLiveMode: Signal<boolean>;
    $pageURI: Signal<string>;
    $variantId: Signal<string>;
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
        {
            state: type<UVEState>(),
            props: type<PageContextComputed>()
        },
        withFlags(UVE_FEATURE_FLAGS),
        withComputed((store) => {
            const pageEntity = store.pageAPIResponse;
            const params = store.pageParams;
            const experiment = store.experiment;
            const page = computed(() => pageEntity()?.page);
            const $isEditMode = computed(() => store.pageParams()?.mode === UVE_MODE.EDIT);
            const $isLockFeatureEnabled = computed(
                () => store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK
            );
            const $hasAccessToEditMode = computed(() => {
                const isPageEditable = page()?.canEdit;
                const isExperimentRunning = [
                    DotExperimentStatus.RUNNING,
                    DotExperimentStatus.SCHEDULED
                ].includes(experiment()?.status);
                return isPageEditable && !isExperimentRunning && !$pageIsLocked();
            });
            const $pageIsLocked = computed(() => {
                return computePageIsLocked(page(), store.currentUser(), $isLockFeatureEnabled());
            });
            return {
                $isEditMode,
                $pageIsLocked,
                $isLockFeatureEnabled,
                $hasAccessToEditMode,
                $languageId: computed<number>(() => pageEntity()?.viewAs.language?.id || 1),
                $isPreviewMode: computed<boolean>(() => params()?.mode === UVE_MODE.PREVIEW),
                $isLiveMode: computed<boolean>(() => params()?.mode === UVE_MODE.LIVE),
                $pageURI: computed(() => page()?.pageURI ?? ''),
                $variantId: computed(() => params()?.variantId ?? ''),
                $canEditPage: computed(() => $hasAccessToEditMode() && $isEditMode())
            } satisfies PageContextComputed;
        })
    );
}
