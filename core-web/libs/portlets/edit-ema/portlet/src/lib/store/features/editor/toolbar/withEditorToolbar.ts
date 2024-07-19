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
import {
    createFavoritePagesURL,
    createPageApiUrlWithQueryParams,
    createPureURL,
    sanitizeURL
} from '../../../../utils';
import { EditorToolbarState, UVEState } from '../../../models';

const initialState: EditorToolbarState = {
    device: undefined,
    socialMedia: undefined,
    isEditState: true
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
            toolbarState: computed(() => {
                const params = store.params();
                const url = sanitizeURL(params.url);

                const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
                const pageAPIResponse = store.pageAPIResponse();
                const experiment = store.experiment?.();

                const pageAPI = `/api/v1/page/${
                    store.isLegacyPage() ? 'render' : 'json'
                }/${pageAPIQueryParams}`;

                return {
                    deviceSelector: {
                        apiLink: `${params.clientHost ?? window.location.origin}${pageAPI}`,
                        hideSocialMedia: !store.isLegacyPage()
                    },
                    urlContentMap: store.isEditState() && pageAPIResponse.urlContentMap,
                    bookmarks: {
                        url: createFavoritePagesURL({
                            languageId: Number(params.language_id),
                            pageURI: url,
                            siteId: pageAPIResponse.site.identifier
                        })
                    },
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
                    workflowActions: store.canEditPage() && {
                        inode: pageAPIResponse.page.inode
                    },
                    unlockButton: store.pageIsLocked() &&
                        pageAPIResponse.page.canLock && {
                            inode: pageAPIResponse.page.inode
                        },
                    showInfoDisplay: !store.canEditPage() || store.device() || store.socialMedia()
                };
            })
        })),
        withMethods((store) => {
            return {
                setDevice: (device: DotDevice) => {
                    patchState(store, { device, socialMedia: undefined, isEditState: false });
                },
                setSocialMedia: (socialMedia: string) => {
                    patchState(store, { socialMedia, device: undefined, isEditState: false });
                },
                clearDeviceAndSocialMedia: () => {
                    patchState(store, initialState);
                }
            };
        })
    );
}
