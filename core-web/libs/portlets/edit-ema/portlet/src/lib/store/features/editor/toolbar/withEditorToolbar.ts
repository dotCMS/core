import {
    signalStoreFeature,
    type,
    withState,
    withMethods,
    patchState,
    withComputed
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, DotExperimentStatus } from '@dotcms/dotcms-models';

import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { InfoOptions } from '../../../../shared/models';
import {
    createFavoritePagesURL,
    createPageApiUrlWithQueryParams,
    createFullURL,
    getIsDefaultVariant,
    sanitizeURL,
    computePageIsLocked
} from '../../../../utils';
import { UVEState } from '../../../models';
import { EditorToolbarState, ToolbarProps } from '../models';

const initialState: EditorToolbarState = {
    device: null,
    socialMedia: null,
    isEditState: true
};

/**
 * Add computed properties and methods to the store to handle the Editor Toolbar UI
 *
 * @export
 * @return {*}
 */
export function withEditorToolbar() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<EditorToolbarState>(initialState),
        withComputed((store) => ({
            $toolbarProps: computed<ToolbarProps>(() => {
                const params = store.params();
                const url = sanitizeURL(params?.url);

                const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
                const pageAPIResponse = store.pageAPIResponse();
                const experiment = store.experiment?.();

                const pageAPI = `/api/v1/page/${
                    store.isTraditionalPage() ? 'render' : 'json'
                }/${pageAPIQueryParams}`;

                const isExperimentRunning = experiment?.status === DotExperimentStatus.RUNNING;
                const isPageLocked = computePageIsLocked(
                    pageAPIResponse?.page,
                    store.currentUser()
                );
                const shouldShowUnlock = isPageLocked && pageAPIResponse?.page.canLock;

                const unlockButton = {
                    inode: pageAPIResponse?.page.inode,
                    loading: store.status() === UVE_STATUS.LOADING
                };

                const shouldShowInfoDisplay =
                    !getIsDefaultVariant(pageAPIResponse?.viewAs.variantId) ||
                    !store.canEditPage() ||
                    isPageLocked ||
                    !!store.device() ||
                    !!store.socialMedia();

                const bookmarksUrl = createFavoritePagesURL({
                    languageId: Number(params?.language_id),
                    pageURI: url,
                    siteId: pageAPIResponse?.site?.identifier
                });
                const clientHost = `${params?.clientHost ?? window.location.origin}`;
                const siteId = pageAPIResponse?.site?.identifier;

                return {
                    bookmarksUrl,
                    copyUrl: createFullURL(params, siteId),
                    apiUrl: pageAPI,
                    currentLanguage: pageAPIResponse?.viewAs.language,
                    urlContentMap: store.isEditState()
                        ? (pageAPIResponse?.urlContentMap ?? null)
                        : null,
                    runningExperiment: isExperimentRunning ? experiment : null,
                    workflowActionsInode: store.canEditPage() ? pageAPIResponse?.page.inode : null,
                    unlockButton: shouldShowUnlock ? unlockButton : null,
                    showInfoDisplay: shouldShowInfoDisplay,
                    deviceSelector: {
                        apiLink: `${clientHost}${pageAPI}`,
                        hideSocialMedia: !store.isTraditionalPage()
                    },
                    personaSelector: {
                        pageId: pageAPIResponse?.page.identifier,
                        value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
                    }
                };
            }),
            $infoDisplayOptions: computed<InfoOptions>(() => {
                const pageAPIResponse = store.pageAPIResponse();
                const canEditPage = store.canEditPage();
                const device = store.device();
                const socialMedia = store.socialMedia();

                if (device) {
                    return {
                        icon: device.icon,
                        info: {
                            message: `${device.name} ${device.cssWidth} x ${device.cssHeight}`,
                            args: []
                        },
                        id: 'device',
                        actionIcon: 'pi pi-times'
                    };
                } else if (socialMedia) {
                    return {
                        icon: `pi pi-${socialMedia.toLowerCase()}`,
                        id: 'socialMedia',
                        info: {
                            message: `Viewing <b>${socialMedia}</b> social media preview`,
                            args: []
                        },
                        actionIcon: 'pi pi-times'
                    };
                } else if (!getIsDefaultVariant(pageAPIResponse?.viewAs.variantId)) {
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

                if (pageAPIResponse?.page.locked) {
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
            })
        })),
        withMethods((store) => {
            return {
                setDevice: (device: DotDevice) => {
                    patchState(store, {
                        device,
                        socialMedia: null,
                        isEditState: false
                    });
                },
                setSocialMedia: (socialMedia: string) => {
                    patchState(store, {
                        socialMedia,
                        device: null,
                        isEditState: false
                    });
                },
                clearDeviceAndSocialMedia: () => {
                    patchState(store, initialState);
                }
            };
        })
    );
}
