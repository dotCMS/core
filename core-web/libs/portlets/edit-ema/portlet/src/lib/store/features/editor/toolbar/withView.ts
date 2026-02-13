import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';

import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { InfoOptions } from '../../../../shared/models';
import {
    getFullPageURL,
    getIsDefaultVariant,
    getOrientation
} from '../../../../utils';
import { PageAssetComputed } from '../../../features/page/withPage';
import { Orientation, PageType, UVEState } from '../../../models';
import { PersonaSelectorProps } from '../models';

/**
 * Dependencies interface for withView
 * These are computed signals from other features that withView needs
 *
 */
export interface WithViewDeps {
    // No dependencies needed - all accessed from store
}

/**
 * Manages editor view modes (edit vs preview) and preview configuration.
 *
 * Responsibilities:
 * - Device preview mode (viewSetDevice, viewSetOrientation)
 * - SEO/social media preview mode (viewSetSEO)
 * - Edit vs preview state toggle (isEditState)
 * - Lock UI props for toolbar display
 * - View parameters synchronization
 *
 * View state is flattened with view* prefix (viewDevice, viewOrientation, etc.)
 */
export function withView(_deps?: WithViewDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>(),
            props: type<PageAssetComputed>()
        },
        withComputed((store) => ({
            viewMode: computed(() => store.pageParams()?.mode ?? UVE_MODE.UNKNOWN),

            $urlContentMap: computed<DotCMSURLContentMap>(() => {
                return store.pageUrlContentMap();
            }),
            $personaSelector: computed<PersonaSelectorProps>(() => {
                const page = store.pageData();
                const viewAs = store.pageViewAs();

                return {
                    pageId: page?.identifier,
                    value: viewAs?.persona ?? DEFAULT_PERSONA
                };
            }),
            $apiURL: computed<string>(() => {
                const params = store.pageParams();
                const pageURL = getFullPageURL({ url: params.url, params });

                const apiPageType = store.pageType() === PageType.TRADITIONAL ? 'render' : 'json';
                const pageAPI = `/api/v1/page/${apiPageType}/${pageURL}`;

                return pageAPI;
            }),
            $infoDisplayProps: computed<InfoOptions>(() => {
                const viewAs = store.pageViewAs();
                const mode = store.pageParams()?.mode;

                if (!getIsDefaultVariant(viewAs?.variantId)) {
                    const variantId = viewAs.variantId;

                    const currentExperiment = store.experiment?.();

                    const name =
                        currentExperiment?.trafficProportion.variants.find(
                            (variant) => variant.id === variantId
                        )?.name ?? 'Unknown Variant';

                    // Now we base on the mode to show the correct message
                    const message =
                        mode === UVE_MODE.PREVIEW || mode === UVE_MODE.LIVE
                            ? 'editpage.viewing.variant'
                            : 'editpage.editing.variant';

                    return {
                        info: {
                            message,
                            args: [name]
                        },
                        icon: 'pi pi-file-edit',
                        id: 'variant',
                        actionIcon: 'pi pi-arrow-left'
                    };
                }

                return null;
            }),
            $showWorkflowsActions: computed<boolean>(() => {
                const isPreviewMode = store.pageParams()?.mode === UVE_MODE.PREVIEW;
                const isLiveMode = store.pageParams()?.mode === UVE_MODE.LIVE;

                const viewAs = store.pageViewAs();
                const isDefaultVariant = getIsDefaultVariant(viewAs?.variantId);

                return !isPreviewMode && !isLiveMode && isDefaultVariant;
            })
        })),
        withMethods((store) => ({
            viewSetDevice: (device: DotDevice, orientation?: Orientation) => {
                const isValidOrientation = Object.values(Orientation).includes(orientation);
                const newOrientation = isValidOrientation ? orientation : getOrientation(device);

                patchState(store, {
                    viewDevice: device,
                    viewSocialMedia: null,
                    viewIsEditState: false,
                    viewOrientation: newOrientation,
                    viewParams: {
                        ...(store.viewParams() || {}),
                        device: device.inode,
                        orientation: newOrientation,
                        seo: null
                    }
                });
            },
            viewSetOrientation: (orientation: Orientation) => {
                patchState(store, {
                    viewOrientation: orientation,
                    viewParams: store.viewParams() ? {
                        ...store.viewParams(),
                        orientation
                    } : store.viewParams()
                });
            },
            viewSetSEO: (socialMedia: string | null) => {
                patchState(store, {
                    viewDevice: null,
                    viewOrientation: null,
                    viewSocialMedia: socialMedia,
                    viewIsEditState: false,
                    viewParams: {
                        ...(store.viewParams() || {}),
                        device: null,
                        orientation: null,
                        seo: socialMedia
                    }
                });
            },
            viewClearDeviceAndSocialMedia: () => {
                patchState(store, {
                    viewDevice: null,
                    viewSocialMedia: null,
                    viewIsEditState: true,
                    viewOrientation: null,
                    viewParams: {
                        ...(store.viewParams() || {}),
                        device: null,
                        orientation: null,
                        seo: null
                    }
                });
            },
            viewSetOGTagResults: (ogTagsResults: SeoMetaTagsResult[]) => {
                patchState(store, {
                    viewOgTagsResults: ogTagsResults
                });
            }
        }))
    );
}
