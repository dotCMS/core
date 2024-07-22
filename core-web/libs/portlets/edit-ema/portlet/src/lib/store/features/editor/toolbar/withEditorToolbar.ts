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
import { InfoOptions } from '../../../../shared/models';
import {
    createFavoritePagesURL,
    createPageApiUrlWithQueryParams,
    createPureURL,
    getIsDefaultVariant,
    sanitizeURL
} from '../../../../utils';
import { EditorToolbarState, UVEState } from '../../../models';

const initialState: EditorToolbarState = {
    $device: null,
    $socialMedia: null,
    $isEditState: true
};

/**
 * Add computed properties to the store to handle the UVE status
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
            $toolbarState: computed(() => {
                const params = store.$params();
                const url = sanitizeURL(params.url);

                const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
                const pageAPIResponse = store.$pageAPIResponse();
                const experiment = store.$experiment?.();

                const pageAPI = `/api/v1/page/${
                    store.$isTraditionalPage() ? 'render' : 'json'
                }/${pageAPIQueryParams}`;

                return {
                    deviceSelector: {
                        apiLink: `${params.clientHost ?? window.location.origin}${pageAPI}`,
                        hideSocialMedia: !store.$isTraditionalPage()
                    },
                    urlContentMap: store.$isEditState() && pageAPIResponse.urlContentMap,
                    bookmarksUrl: createFavoritePagesURL({
                        languageId: Number(params.language_id),
                        pageURI: url,
                        siteId: pageAPIResponse.site.identifier
                    }),
                    copyUrlButton: {
                        pureURL: createPureURL(params)
                    },
                    apiLinkButton: {
                        apiURL: `${params.clientHost ?? window.location.origin}${pageAPI}`
                    },
                    experimentBadge: experiment?.status === DotExperimentStatus.RUNNING && {
                        runningExperiment: experiment
                    },
                    languageSelector: {
                        currentLanguage: pageAPIResponse.viewAs.language
                    },
                    personaSelector: {
                        pageId: pageAPIResponse.page.identifier,
                        value: pageAPIResponse.viewAs.persona ?? DEFAULT_PERSONA
                    },
                    workflowActions: store.$canEditPage() && {
                        inode: pageAPIResponse.page.inode
                    },
                    unlockButton: store.$pageIsLocked() &&
                        pageAPIResponse.page.canLock && {
                            inode: pageAPIResponse.page.inode
                        },
                    showInfoDisplay:
                        !store.$canEditPage() || store.$device() || store.$socialMedia()
                };
            }),
            $infoDisplay: computed<InfoOptions>(() => {
                const pageAPIResponse = store.$pageAPIResponse();
                const canEditPage = store.$canEditPage();
                const device = store.$device();
                const socialMedia = store.$socialMedia();

                if (store.$pageIsLocked()) {
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
                } else if (canEditPage && !getIsDefaultVariant(pageAPIResponse.viewAs.variantId)) {
                    const variantId = pageAPIResponse.viewAs.variantId;

                    const currentExperiment = store.$experiment?.();

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

                if (!canEditPage) {
                    return {
                        icon: 'pi pi-exclamation-circle warning',
                        id: 'no-permission',
                        info: { message: 'editema.dont.have.edit.permission', args: [] }
                    };
                }

                return undefined;
            })
        })),
        withMethods((store) => {
            return {
                setDevice: (device: DotDevice) => {
                    patchState(store, {
                        $device: device,
                        $socialMedia: undefined,
                        $isEditState: false
                    });
                },
                setSocialMedia: (socialMedia: string) => {
                    patchState(store, {
                        $socialMedia: socialMedia,
                        $device: undefined,
                        $isEditState: false
                    });
                },
                clearDeviceAndSocialMedia: () => {
                    patchState(store, initialState);
                }
            };
        })
    );
}
