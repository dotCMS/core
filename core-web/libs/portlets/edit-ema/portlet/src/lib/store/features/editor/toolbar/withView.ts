import {
    signalStoreFeature,
    withMethods,
    withComputed,
    type,
    patchState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';

import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { InfoOptions, ToggleLockOptions, UnlockOptions } from '../../../../shared/models';
import {
    getFullPageURL,
    getIsDefaultVariant,
    getOrientation
} from '../../../../utils';
import { Orientation, PageType, UVEState } from '../../../models';
import { PersonaSelectorProps } from '../models';

/**
 * Dependencies interface for withView
 * These are methods/computeds from other features that withView needs
 */
export interface WithViewDeps {
    $isPageLocked: () => boolean;
}

/**
 * Manages editor view modes (edit vs preview) and preview configuration.
 *
 * Responsibilities:
 * - Device preview mode (setDevice, setOrientation)
 * - SEO/social media preview mode (setSEO)
 * - Edit vs preview state toggle (isEditState)
 * - Lock UI props for toolbar display
 * - View parameters synchronization
 *
 * View state is nested under store.view()
 */
export function withView(deps: WithViewDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withComputed((store) => ({
            $urlContentMap: computed<DotCMSURLContentMap>(() => {
                return store.urlContentMap();
            }),
            $unlockButton: computed<UnlockOptions | null>(() => {
                const isToggleUnlockEnabled = store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK;

                if (isToggleUnlockEnabled) {
                    return null;
                }

                const page = store.page();
                const isLocked = deps.$isPageLocked();

                const info = {
                    message: page.canLock
                        ? 'editpage.toolbar.page.release.lock.locked.by.user'
                        : 'editpage.locked-by',
                    args: [page.lockedByName]
                };

                const disabled = !page.canLock;

                return isLocked
                    ? {
                          inode: page.inode,
                          loading: store.status() === UVE_STATUS.LOADING,
                          info,
                          disabled
                      }
                    : null;
            }),
            $toggleLockOptions: computed<ToggleLockOptions | null>(() => {
                const page = store.page();
                const currentUser = store.currentUser();

                // Only show lock controls when feature flag is enabled AND in edit mode
                const isToggleUnlockEnabled = store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK;
                const isDraftMode = store.pageParams()?.mode === UVE_MODE.EDIT;

                if (!isToggleUnlockEnabled || !isDraftMode) {
                    return null;
                }

                const isLocked = !!page.locked;
                const isLockedByCurrentUser = page.lockedBy === currentUser?.userId;

                // Show overlay when page is unlocked or locked by another user
                const showOverlay = !isLocked || !isLockedByCurrentUser;

                // Show banner when page is locked by another user
                const showBanner = isLocked && !isLockedByCurrentUser;

                return {
                    inode: page.inode,
                    isLocked,
                    lockedBy: page.lockedByName,
                    canLock: page.canLock ?? false,
                    isLockedByCurrentUser,
                    showBanner: showBanner,
                    showOverlay
                };
            }),
            $personaSelector: computed<PersonaSelectorProps>(() => {
                const page = store.page();
                const viewAs = store.viewAs();

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
                const viewAs = store.viewAs();
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

                const viewAs = store.viewAs();
                const isDefaultVariant = getIsDefaultVariant(viewAs?.variantId);

                return !isPreviewMode && !isLiveMode && isDefaultVariant;
            })
        })),
        withMethods((store) => ({
            setDevice: (device: DotDevice, orientation?: Orientation) => {
                const view = store.view();
                const isValidOrientation = Object.values(Orientation).includes(orientation);

                const newOrientation = isValidOrientation ? orientation : getOrientation(device);
                patchState(store, {
                    view: {
                        ...view,
                        device,
                        socialMedia: null,
                        isEditState: false,
                        orientation: newOrientation,
                        viewParams: {
                            ...(view.viewParams || {}),
                            device: device.inode,
                            orientation: newOrientation,
                            seo: null
                        }
                    }
                });
            },
            setOrientation: (orientation: Orientation) => {
                const view = store.view();
                patchState(store, {
                    view: {
                        ...view,
                        orientation,
                        viewParams: view.viewParams ? {
                            ...view.viewParams,
                            orientation
                        } : view.viewParams
                    }
                });
            },
            setSEO: (socialMedia: string | null) => {
                const view = store.view();
                patchState(store, {
                    view: {
                        ...view,
                        device: null,
                        orientation: null,
                        socialMedia,
                        isEditState: false,
                        viewParams: {
                            ...(view.viewParams || {}),
                            device: null,
                            orientation: null,
                            seo: socialMedia
                        }
                    }
                });
            },
            clearDeviceAndSocialMedia: () => {
                const view = store.view();
                patchState(store, {
                    view: {
                        ...view,
                        device: null,
                        socialMedia: null,
                        isEditState: true,
                        orientation: null,
                        viewParams: {
                            ...(view.viewParams || {}),
                            device: null,
                            orientation: null,
                            seo: null
                        }
                    }
                });
            },
            setOGTagResults: (ogTagsResults: SeoMetaTagsResult[]) => {
                const view = store.view();
                patchState(store, {
                    view: {
                        ...view,
                        ogTagsResults
                    }
                });
            }
        }))
    );
}
