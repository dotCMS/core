import {
    signalStoreFeature,
    withMethods,
    withComputed,
    withState,
    type,
    patchState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, DotExperimentStatus, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';

import { DEFAULT_DEVICE, DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { InfoOptions, UnlockOptions } from '../../../../shared/models';
import {
    computePageIsLocked,
    createFavoritePagesURL,
    createFullURL,
    getFullPageURL,
    getIsDefaultVariant,
    getOrientation
} from '../../../../utils';
import { Orientation, UVEState } from '../../../models';
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
        withComputed((store) => ({
            $uveToolbar: computed<UVEToolbarProps>(() => {
                const params = store.pageParams();
                const url = params?.url;

                const experiment = store.experiment?.();
                const pageAPIResponse = store.pageAPIResponse();
                const pageAPIQueryParams = getFullPageURL({ url, params });

                const pageAPI = `/api/v1/page/${
                    store.isTraditionalPage() ? 'render' : 'json'
                }/${pageAPIQueryParams}`;

                const bookmarksUrl = createFavoritePagesURL({
                    languageId: Number(params?.language_id),
                    pageURI: url,
                    siteId: pageAPIResponse?.site?.identifier
                });

                const isPageLocked = computePageIsLocked(
                    pageAPIResponse?.page,
                    store.currentUser()
                );
                const shouldShowUnlock = isPageLocked && pageAPIResponse?.page.canLock;
                const isExperimentRunning = experiment?.status === DotExperimentStatus.RUNNING;

                const unlockButton = {
                    inode: pageAPIResponse?.page.inode,
                    loading: store.status() === UVE_STATUS.LOADING
                };

                const siteId = pageAPIResponse?.site?.identifier;
                const clientHost = `${params?.clientHost ?? window.location.origin}`;

                const isPreview = params?.mode === UVE_MODE.PREVIEW;
                const prevewItem = isPreview
                    ? {
                          deviceSelector: {
                              apiLink: `${clientHost}${pageAPI}`,
                              hideSocialMedia: !store.isTraditionalPage()
                          }
                      }
                    : null;

                return {
                    editor: {
                        bookmarksUrl,
                        copyUrl: createFullURL(params, siteId),
                        apiUrl: pageAPI
                    },
                    preview: prevewItem,
                    currentLanguage: pageAPIResponse?.viewAs.language,
                    urlContentMap: store.isEditState()
                        ? (pageAPIResponse?.urlContentMap ?? null)
                        : null,
                    runningExperiment: isExperimentRunning ? experiment : null,
                    unlockButton: shouldShowUnlock ? unlockButton : null
                };
            }),
            $urlContentMap: computed<DotCMSURLContentMap>(() => {
                return store.pageAPIResponse()?.urlContentMap;
            }),
            $unlockButton: computed<UnlockOptions | null>(() => {
                const pageAPIResponse = store.pageAPIResponse();
                const currentUser = store.currentUser();
                const isLocked = computePageIsLocked(pageAPIResponse.page, currentUser);
                const info = {
                    message: pageAPIResponse.page.canLock
                        ? 'editpage.toolbar.page.release.lock.locked.by.user'
                        : 'editpage.locked-by',
                    args: [pageAPIResponse.page.lockedByName]
                };

                const disabled = !pageAPIResponse.page.canLock;

                return isLocked
                    ? {
                          inode: pageAPIResponse.page.inode,
                          loading: store.status() === UVE_STATUS.LOADING,
                          info,
                          disabled
                      }
                    : null;
            }),
            $personaSelector: computed<PersonaSelectorProps>(() => {
                const pageAPIResponse = store.pageAPIResponse();

                return {
                    pageId: pageAPIResponse?.page.identifier,
                    value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
                };
            }),
            $apiURL: computed<string>(() => {
                const params = store.pageParams();
                const pageURL = getFullPageURL({ url: params.url, params });

                const pageType = store.isTraditionalPage() ? 'render' : 'json';
                const pageAPI = `/api/v1/page/${pageType}/${pageURL}`;

                return pageAPI;
            }),
            $infoDisplayProps: computed<InfoOptions>(() => {
                const pageAPIResponse = store.pageAPIResponse();
                const mode = store.pageParams()?.mode;

                if (!getIsDefaultVariant(pageAPIResponse?.viewAs.variantId)) {
                    const variantId = pageAPIResponse.viewAs.variantId;

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
                    store.pageAPIResponse()?.viewAs.variantId
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
