import {
    signalStoreFeature,
    type,
    withState,
    withMethods,
    patchState,
    withComputed
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotExperimentStatus } from '@dotcms/dotcms-models';

import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
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
                const params = store.pageParams();
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
            })
        })),
        withMethods((store) => {
            return {
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
