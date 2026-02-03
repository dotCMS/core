import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, DotExperimentStatus, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';

import { DEFAULT_DEVICE, DEFAULT_PERSONA, UVE_FEATURE_FLAGS } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { InfoOptions, ToggleLockOptions, UnlockOptions } from '../../../../shared/models';
import {
    computeIsPageLocked,
    createFavoritePagesURL,
    getFullPageURL,
    getIsDefaultVariant,
    getOrientation
} from '../../../../utils';
import { Orientation, PageType, UVEState } from '../../../models';
import { withFlags } from '../../flags/withFlags';
import { EditorToolbarState, PersonaSelectorProps, UVEToolbarProps } from '../models';

/**
 * The initial state for the editor toolbar.
 *
 * @property {EditorToolbarState} initialState - The initial state object for the editor toolbar.
 * @property {string | null} initialState.device - The current device being used, or null if not set.
 * @property {string | null} initialState.socialMedia - The current social media platform being used, or null if not set.
 * @property {boolean} initialState.isEditState - Flag indicating whether the editor is in edit mode.
 * @property {boolean} initialState.isPreviewModeActive - Flag indicating whether the preview mode is active.
 */
const initialState: EditorToolbarState = {
    device: DEFAULT_DEVICE,
    socialMedia: null,
    isEditState: true,
    isPreviewModeActive: false,
    orientation: Orientation.LANDSCAPE,
    ogTagsResults: null
};

export function withUVEToolbar() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<EditorToolbarState>(initialState),
        withFlags(UVE_FEATURE_FLAGS),
        withComputed((store) => ({
            $uveToolbar: computed<UVEToolbarProps>(() => {
                const params = store.pageParams();
                const url = params?.url;

                const experiment = store.experiment?.();
                const pageAPIQueryParams = getFullPageURL({ url, params });

                const pageAPI = `/api/v1/page/${
                    store.pageType() === PageType.TRADITIONAL ? 'render' : 'json'
                }/${pageAPIQueryParams}`;

                const bookmarksUrl = createFavoritePagesURL({
                    languageId: Number(params?.language_id),
                    pageURI: url,
                    siteId: store.site()?.identifier
                });

                const isPageLocked = computeIsPageLocked(
                    store.page() ?? null,
                    store.currentUser()
                );
                const shouldShowUnlock = isPageLocked && store.page()?.canLock;
                const isExperimentRunning = experiment?.status === DotExperimentStatus.RUNNING;

                const unlockButton = {
                    inode: store.page()?.inode,
                    loading: store.status() === UVE_STATUS.LOADING
                };

                const clientHost = `${params?.clientHost ?? window.location.origin}`;

                const isPreview = params?.mode === UVE_MODE.PREVIEW;
                const prevewItem = isPreview
                    ? {
                          deviceSelector: {
                              apiLink: `${clientHost}${pageAPI}`,
                              hideSocialMedia: store.pageType() === PageType.HEADLESS
                          }
                      }
                    : null;

                return {
                    editor: {
                        bookmarksUrl,
                        apiUrl: pageAPI
                    },
                    preview: prevewItem,
                    currentLanguage: store.viewAs()?.language,
                    urlContentMap: store.isEditState()
                        ? (store.urlContentMap() ?? null)
                        : null,
                    runningExperiment: isExperimentRunning ? experiment : null,
                    unlockButton: shouldShowUnlock ? unlockButton : null
                };
            }),
            $urlContentMap: computed<DotCMSURLContentMap>(() => {
                return store.urlContentMap() ?? null;
            }),
            $unlockButton: computed<UnlockOptions | null>(() => {
                const isToggleUnlockEnabled = store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK;

                if (isToggleUnlockEnabled) {
                    return null;
                }

                const currentUser = store.currentUser();

                const isLocked = computeIsPageLocked(store.page() ?? null, currentUser);
                const info = {
                    message: store.page()?.canLock
                        ? 'editpage.toolbar.page.release.lock.locked.by.user'
                        : 'editpage.locked-by',
                    args: [store.page()?.lockedByName]
                };

                const disabled = !store.page()?.canLock;

                return isLocked
                    ? {
                          inode: store.page()?.inode,
                          loading: store.status() === UVE_STATUS.LOADING,
                          info,
                          disabled
                      }
                    : null;
            }),
            $toggleLockOptions: computed<ToggleLockOptions | null>(() => {
                const page = store.page() ?? null;
                const currentUser = store.currentUser();

                // Only show lock controls when feature flag is enabled AND in edit mode
                const isToggleUnlockEnabled = store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK;
                const isDraftMode = store.pageParams()?.mode === UVE_MODE.EDIT;

                if (!isToggleUnlockEnabled || !isDraftMode) {
                    return null;
                }

                const isLocked = !!page?.locked;
                const isLockedByCurrentUser = page?.lockedBy === currentUser?.userId;

                // Show overlay when page is unlocked or locked by another user
                const showOverlay = !isLocked || !isLockedByCurrentUser;

                // Show banner when page is locked by another user
                const showBanner = isLocked && !isLockedByCurrentUser;

                return {
                    inode: page?.inode,
                    isLocked,
                    lockedBy: page.lockedByName,
                    canLock: page.canLock ?? false,
                    isLockedByCurrentUser,
                    showBanner: showBanner,
                    showOverlay
                };
            }),
            $personaSelector: computed<PersonaSelectorProps>(() => {

                return {
                    pageId: store.page()?.identifier,
                    value: store.viewAs()?.persona ?? DEFAULT_PERSONA
                };
            }),
            $apiURL: computed<string>(() => {
                const params = store.pageParams();
                const pageURL = getFullPageURL({ url: params.url, params });

                const pageType = store.pageType() === PageType.TRADITIONAL ? 'render' : 'json';
                const pageAPI = `/api/v1/page/${pageType}/${pageURL}`;

                return pageAPI;
            }),
            $infoDisplayProps: computed<InfoOptions>(() => {
                const mode = store.pageParams()?.mode;

                if (!getIsDefaultVariant(store.viewAs()?.variantId)) {
                    const variantId = store.viewAs()?.variantId;

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

                const isDefaultVariant = getIsDefaultVariant(
                    store.viewAs()?.variantId
                );

                return !isPreviewMode && !isLiveMode && isDefaultVariant;
            })
        })),
        withMethods((store) => ({
            setDevice: (device: DotDevice, orientation?: Orientation) => {
                const isValidOrientation = Object.values(Orientation).includes(orientation);

                const newOrientation = isValidOrientation ? orientation : getOrientation(device);
                patchState(store, {
                    device,
                    viewParams: {
                        ...store.viewParams(),
                        device: device.inode,
                        orientation: newOrientation,
                        seo: null
                    },
                    socialMedia: null,
                    isEditState: false,
                    orientation: newOrientation
                });
            },
            setOrientation: (orientation: Orientation) => {
                patchState(store, {
                    orientation,
                    viewParams: {
                        ...store.viewParams(),
                        orientation
                    }
                });
            },
            setSEO: (socialMedia: string | null) => {
                patchState(store, {
                    device: null,
                    orientation: null,
                    socialMedia,
                    viewParams: {
                        ...store.viewParams(),
                        device: null,
                        orientation: null,
                        seo: socialMedia
                    },
                    isEditState: false
                });
            },
            clearDeviceAndSocialMedia: () => {
                patchState(store, {
                    device: null,
                    socialMedia: null,
                    isEditState: true,
                    orientation: null,
                    viewParams: {
                        ...store.viewParams(),
                        device: null,
                        orientation: null,
                        seo: null
                    }
                });
            },
            setOGTagResults: (ogTagsResults: SeoMetaTagsResult[]) => {
                patchState(store, { ogTagsResults });
            }
        }))
    );
}
