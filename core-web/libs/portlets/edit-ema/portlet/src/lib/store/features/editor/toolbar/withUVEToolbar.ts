import {
    signalStoreFeature,
    withMethods,
    withComputed,
    withState,
    type,
    patchState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { UVE_MODE } from '@dotcms/client';
import { DotDevice, DotExperimentStatus, SeoMetaTagsResult } from '@dotcms/dotcms-models';

import { DEFAULT_DEVICE, DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { InfoOptions } from '../../../../shared/models';
import {
    computePageIsLocked,
    createFavoritePagesURL,
    createFullURL,
    createPageApiUrlWithQueryParams,
    getIsDefaultVariant,
    getOrientation,
    sanitizeURL
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
                const url = sanitizeURL(params?.url);

                const experiment = store.experiment?.();
                const pageAPIResponse = store.pageAPIResponse();
                const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);

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

                const isPreview = params?.editorMode === UVE_MODE.PREVIEW;
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
            $personaSelector: computed<PersonaSelectorProps>(() => {
                const pageAPIResponse = store.pageAPIResponse();

                return {
                    pageId: pageAPIResponse?.page.identifier,
                    value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
                };
            }),
            $apiURL: computed<string>(() => {
                const pageParams = store.pageParams();
                const url = sanitizeURL(pageParams?.url);
                const params = createPageApiUrlWithQueryParams(url, pageParams);
                const pageType = store.isTraditionalPage() ? 'render' : 'json';
                const pageAPI = `/api/v1/page/${pageType}/${params}`;

                return pageAPI;
            }),
            $infoDisplayProps: computed<InfoOptions>(() => {
                const pageAPIResponse = store.pageAPIResponse();
                const canEditPage = store.canEditPage();
                const socialMedia = store.socialMedia();
                const currentUser = store.currentUser();
                const isPreview = store.pageParams()?.editorMode === UVE_MODE.PREVIEW;

                if (socialMedia && !isPreview) {
                    return {
                        icon: `pi pi-${socialMedia.toLowerCase()}`,
                        id: 'socialMedia',
                        info: {
                            message: `Viewing <b>${socialMedia}</b> social media preview`,
                            args: []
                        },
                        actionIcon: 'pi pi-times'
                    };
                }

                if (!getIsDefaultVariant(pageAPIResponse?.viewAs.variantId)) {
                    const variantId = pageAPIResponse.viewAs.variantId;

                    const currentExperiment = store.experiment?.();

                    const name =
                        currentExperiment?.trafficProportion.variants.find(
                            (variant) => variant.id === variantId
                        )?.name ?? 'Unknown Variant';

                    return {
                        info: {
                            message: canEditPage
                                ? 'editpage.editing.variant'
                                : 'editpage.viewing.variant',
                            args: [name]
                        },
                        icon: 'pi pi-file-edit',
                        id: 'variant',
                        actionIcon: 'pi pi-arrow-left'
                    };
                }

                if (computePageIsLocked(pageAPIResponse.page, currentUser)) {
                    let message = 'editpage.locked-by';

                    if (!pageAPIResponse.page.canLock) {
                        message = 'editpage.locked-contact-with';
                    }

                    return {
                        icon: 'pi pi-lock',
                        id: 'locked',
                        info: {
                            message,
                            args: [pageAPIResponse.page.lockedByName]
                        }
                    };
                }

                if (!canEditPage) {
                    return {
                        icon: 'pi pi-exclamation-circle warning',
                        id: 'no-permission',
                        info: { message: 'editema.dont.have.edit.permission', args: [] }
                    };
                }

                return null;
            }),
            $showWorkflowsActions: computed<boolean>(() => {
                const isPreviewMode = store.pageParams()?.editorMode === UVE_MODE.PREVIEW;

                const isDefaultVariant = getIsDefaultVariant(
                    store.pageAPIResponse()?.viewAs.variantId
                );

                return !isPreviewMode && isDefaultVariant;
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
                        orientation: newOrientation
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
                        device: undefined,
                        orientation: undefined,
                        seo: undefined
                    }
                });
            },
            setOGTagResults: (ogTagsResults: SeoMetaTagsResult[]) => {
                patchState(store, { ogTagsResults });
            }
        }))
    );
}
