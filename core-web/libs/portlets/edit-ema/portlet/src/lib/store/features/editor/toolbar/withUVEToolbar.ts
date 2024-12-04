import {
    signalStoreFeature,
    withMethods,
    withComputed,
    withState,
    type,
    patchState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotExperimentStatus } from '@dotcms/dotcms-models';

import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import {
    computePageIsLocked,
    createFavoritePagesURL,
    createFullURL,
    createPageApiUrlWithQueryParams,
    getIsDefaultVariant,
    sanitizeURL
} from '../../../../utils';
import { UVEState } from '../../../models';
import { EditorToolbarState, UVEToolbarProps } from '../models';

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
    device: null,
    socialMedia: null,
    isEditState: true,
    isPreviewModeActive: false
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

                const shouldShowInfoDisplay =
                    !getIsDefaultVariant(pageAPIResponse?.viewAs.variantId) ||
                    !store.canEditPage() ||
                    isPageLocked ||
                    !!store.device() ||
                    !!store.socialMedia();

                const siteId = pageAPIResponse?.site?.identifier;
                const clientHost = `${params?.clientHost ?? window.location.origin}`;

                return {
                    editor: {
                        bookmarksUrl,
                        copyUrl: createFullURL(params, siteId),
                        apiUrl: pageAPI
                    },
                    preview: {
                        deviceSelector: {
                            apiLink: `${clientHost}${pageAPI}`,
                            hideSocialMedia: !store.isTraditionalPage()
                        }
                    },
                    currentLanguage: pageAPIResponse?.viewAs.language,
                    urlContentMap: store.isEditState()
                        ? (pageAPIResponse?.urlContentMap ?? null)
                        : null,
                    runningExperiment: isExperimentRunning ? experiment : null,
                    workflowActionsInode: store.canEditPage() ? pageAPIResponse?.page.inode : null,
                    personaSelector: {
                        pageId: pageAPIResponse?.page.identifier,
                        value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
                    },
                    unlockButton: shouldShowUnlock ? unlockButton : null,
                    showInfoDisplay: shouldShowInfoDisplay
                };
            }),
            $apiURL: computed<string>(() => {
                const pageParams = store.pageParams();
                const url = sanitizeURL(pageParams?.url);
                const params = createPageApiUrlWithQueryParams(url, pageParams);
                const pageType = store.isTraditionalPage() ? 'render' : 'json';
                const pageAPI = `/api/v1/page/${pageType}/${params}`;

                return pageAPI;
            })
        })),
        withMethods((store) => ({
            // Fake method to toggle preview mode
            // This method should be implemented in the real application
            togglePreviewMode: (preview) => {
                patchState(store, {
                    isPreviewModeActive: preview
                });
            }
        }))
    );
}
