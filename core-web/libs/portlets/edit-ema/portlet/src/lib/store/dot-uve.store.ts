import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed, untracked } from '@angular/core';

import { DotCMSPageAsset } from '@dotcms/types';

import { withSave } from './features/editor/save/withSave';
import { withEditor } from './features/editor/withEditor';
import { withLock } from './features/editor/withLock';
import { withFlags } from './features/flags/withFlags';
import { withLayout } from './features/layout/withLayout';
import { withTrack } from './features/track/withTrack';
import { withPageContext } from './features/withPageContext';
import { TranslateProps, UVEState, Orientation, PageType } from './models';

import { DEFAULT_DEVICE, UVE_FEATURE_FLAGS } from '../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import { ClientData } from '../shared/models';
import { normalizeQueryParams } from '../utils';

// Some properties can be computed
// Ticket: https://github.com/dotCMS/core/issues/30760
const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: null,
    status: UVE_STATUS.LOADING,
    pageType: PageType.TRADITIONAL,
    // Normalized page response properties
    page: null,
    site: null,
    viewAs: null,
    template: null,
    layout: null,
    urlContentMap: null,
    containers: null,
    vanityUrl: null,
    numberContents: null,
    // Phase 3.2: Nested UI state
    editor: {
        dragItem: null,
        bounds: [],
        state: EDITOR_STATE.IDLE,
        activeContentlet: null,
        contentArea: null,
        selectedContentlet: null,
        panels: {
            palette: {
                open: true
            },
            rightSidebar: {
                open: false
            }
        },
        ogTags: null,
        styleSchemas: []
    },
    toolbar: {
        device: DEFAULT_DEVICE,
        orientation: Orientation.LANDSCAPE,
        socialMedia: null,
        viewParams: null,
        isEditState: true,
        isPreviewModeActive: false,
        ogTagsResults: null
    }
};

export const UVEStore = signalStore(
    { protectedState: false }, // TODO: remove when the unit tests are fixed
    withState<UVEState>(initialState),
    // Make common computed available through all the features
    withPageContext(),
    withMethods((store) => {
        return {
            setUveStatus(status: UVE_STATUS) {
                patchState(store, {
                    status
                });
            },
            updatePageResponse(pageAPIResponse: DotCMSPageAsset) {
                patchState(store, {
                    status: UVE_STATUS.LOADED,
                    page: pageAPIResponse?.page,
                    site: pageAPIResponse?.site,
                    viewAs: pageAPIResponse?.viewAs,
                    template: pageAPIResponse?.template,
                    layout: pageAPIResponse?.layout,
                    urlContentMap: pageAPIResponse?.urlContentMap,
                    containers: pageAPIResponse?.containers,
                    vanityUrl: pageAPIResponse?.vanityUrl,
                    numberContents: pageAPIResponse?.numberContents
                });
            },
            setSelectedContentlet(selectedContentlet: Pick<ClientData, 'container' | 'contentlet'> | undefined) {
                const editor = store.editor();

                patchState(store, {
                    editor: {
                        ...editor,
                        selectedContentlet: selectedContentlet ?? null
                    }
                });
            }
        };
    }),
    withSave(),
    withLayout(),
    withEditor(),
    withTrack(),
    withFlags(UVE_FEATURE_FLAGS),
    withLock(),
    withComputed(
        ({
            page,
            viewAs,
            pageParams,
            toolbar,
            languages,
        }) => {
            return {
                $translateProps: computed<TranslateProps>(() => {
                    const pageData = page();
                    const viewAsData = viewAs();
                    const languageId = viewAsData?.language?.id;
                    const translatedLanguages = untracked(() => languages());
                    const currentLanguage = translatedLanguages.find(
                        (lang) => lang.id === languageId
                    );

                    return {
                        page: pageData,
                        currentLanguage
                    };
                }),
                $friendlyParams: computed(() => {
                    const params = {
                        ...(pageParams() ?? {}),
                        ...(toolbar().viewParams ?? {})
                    };

                    return normalizeQueryParams(params);
                })
            };
        }
    )
);
