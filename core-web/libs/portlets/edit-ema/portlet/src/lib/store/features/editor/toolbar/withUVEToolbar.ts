import { signalStoreFeature, withMethods, withComputed, withState, type, patchState } from "@ngrx/signals";

import { computed } from "@angular/core";

import { DotExperiment, DotPersona, DotLanguage, DotExperimentStatus } from "@dotcms/dotcms-models";

import { DEFAULT_PERSONA } from "../../../../shared/consts";
import { UVE_STATUS } from "../../../../shared/enums";
import { computePageIsLocked, createFavoritePagesURL, createFullURL, createPageApiUrlWithQueryParams, getIsDefaultVariant, sanitizeURL } from "../../../../utils";
import { UVEState } from "../../../models";
import { EditorToolbarState } from "../models";

const initialState: EditorToolbarState = {
        device: null,
        socialMedia: null,
        isEditState: true,
        isPreviewModeActive: false
}

export interface UVEToolbarProps {
    editor: {
        bookmarksUrl: string;
        copyUrl: string;
        apiUrl: string;
    };
    preview?: {
        deviceSelector: {
            apiLink: string;
            hideSocialMedia: boolean;
        };
    };
    personaSelector: {
        pageId: string;
        value: DotPersona;
    };
    runningExperiment?: DotExperiment ;
    currentLanguage: DotLanguage;
    workflowActionsInode?: string ;
    unlockButton?: {
        inode: string;
        loading: boolean;
    };
    showInfoDisplay?: boolean;
}

export function withUVEToolbar() {
    const oldToolbar = signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<EditorToolbarState>(initialState),
        withComputed((store) => ({
            $toolbar: computed<UVEToolbarProps>(() => {
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
                    editor: store.isPreviewModeActive() ? null : {                        
                        bookmarksUrl,
                        copyUrl: createFullURL(params, siteId),
                        apiUrl: pageAPI,
                    },
                    preview: store.isPreviewModeActive() ? {
                        deviceSelector: {
                            apiLink: `${clientHost}${pageAPI}`,
                            hideSocialMedia: !store.isTraditionalPage()
                        },
                    } : null,
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
                    showInfoDisplay: shouldShowInfoDisplay,
                };
            }),
        })),
        withMethods((store) => ({
            // Fake method to toggle preview mode
            // This method should be implemented in the real application
            togglePreviewMode: (preview) => {
                patchState(store, {
                    isPreviewModeActive: preview
                })
            }
        }))
    )

    // const newToolbar = signalStoreFeature(
    //     {
    //         state: type<UVEState>()
    //     },
    //     withState<EditorToolbarState>(initialState),
    //     withComputed((store) => ({
    //         $toolbar: computed<UVEToolbarProps>(() => {
    //             const params = store.pageParams();
    //             const url = sanitizeURL(params?.url);

    //             const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
    //             const pageAPIResponse = store.pageAPIResponse();
    //             const experiment = store.experiment?.();

    //             const pageAPI = `/api/v1/page/${
    //                 store.isTraditionalPage() ? 'render' : 'json'
    //             }/${pageAPIQueryParams}`;

    //             const isExperimentRunning = experiment?.status === DotExperimentStatus.RUNNING;
    //             const isPageLocked = computePageIsLocked(
    //                 pageAPIResponse?.page,
    //                 store.currentUser()
    //             );
    //             const shouldShowUnlock = isPageLocked && pageAPIResponse?.page.canLock;

    //             const unlockButton = {
    //                 inode: pageAPIResponse?.page.inode,
    //                 loading: store.status() === UVE_STATUS.LOADING
    //             };

    //             const shouldShowInfoDisplay =
    //                 !getIsDefaultVariant(pageAPIResponse?.viewAs.variantId) ||
    //                 !store.canEditPage() ||
    //                 isPageLocked ||
    //                 !!store.device() ||
    //                 !!store.socialMedia();

    //             const bookmarksUrl = createFavoritePagesURL({
    //                 languageId: Number(params?.language_id),
    //                 pageURI: url,
    //                 siteId: pageAPIResponse?.site?.identifier
    //             });
    //             const clientHost = `${params?.clientHost ?? window.location.origin}`;
    //             const siteId = pageAPIResponse?.site?.identifier;

    //             return {
    //                 editorToolbar: store.isPreviewMode() ? null : {                        
    //                     bookmarksUrl,
    //                     copyUrl: createFullURL(params, siteId),
    //                     apiUrl: pageAPI,
    //                 },
    //                 previewToolbar: store.isPreviewMode() ? {
    //                     deviceSelector: {
    //                         apiLink: `${clientHost}${pageAPI}`,
    //                         hideSocialMedia: !store.isTraditionalPage()
    //                     },
    //                 } : null,
    //                 currentLanguage: pageAPIResponse?.viewAs.language,
    //                 urlContentMap: store.isEditState()
    //                     ? (pageAPIResponse?.urlContentMap ?? null)
    //                     : null,
    //                 runningExperiment: isExperimentRunning ? experiment : null,
    //                 workflowActionsInode: store.canEditPage() ? pageAPIResponse?.page.inode : null,
    //                 personaSelector: {
    //                     pageId: pageAPIResponse?.page.identifier,
    //                     value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
    //                 },
    //                 unlockButton: shouldShowUnlock ? unlockButton : null,
    //                 showInfoDisplay: shouldShowInfoDisplay,
    //             };
    //         }),
    //     })),
    //     withMethods((store) => ({
    //         // Fake method to toggle preview mode
    //         // This method should be implemented in the real application
    //         togglePreviewMode: (preview) => {
    //             patchState(store, {
    //                 isPreviewMode: preview
    //             })
    //         }
    //     }))
    // )

    return oldToolbar;
}