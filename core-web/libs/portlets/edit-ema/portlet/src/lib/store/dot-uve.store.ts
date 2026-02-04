import { patchState, signalStore, withComputed, withFeature, withMethods, withState } from '@ngrx/signals';

import { computed, untracked } from '@angular/core';

import { DotCMSPageAsset } from '@dotcms/types';

import { withClient } from './features/client/withClient';
import { withSave } from './features/editor/save/withSave';
import { withView } from './features/editor/toolbar/withView';
import { withEditor } from './features/editor/withEditor';
import { withLock } from './features/editor/withLock';
import { withFlags } from './features/flags/withFlags';
import { withLayout } from './features/layout/withLayout';
import { withLoad } from './features/load/withLoad';
import { withTrack } from './features/track/withTrack';
import { withPageContext } from './features/withPageContext';
import { withWorkflow } from './features/workflow/withWorkflow';
import { withZoom } from './features/zoom/withZoom';
import { Orientation, PageType, TranslateProps, UVEState } from './models';

import { DEFAULT_DEVICE, UVE_FEATURE_FLAGS } from '../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import { normalizeQueryParams } from '../utils';

// Some properties can be computed
// Ticket: https://github.com/dotCMS/core/issues/30760
const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    flags: {}, // Will be populated by withFlags feature
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
    view: {
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

    // ---- Core State Features (no dependencies) ----
    withFlags(UVE_FEATURE_FLAGS),    // Flags first (others may depend on it)
    withFeature(() => withClient()), // Client config (exposes timeMachine methods)
    withWorkflow(),                   // Workflow state (independent)
    withTrack(),                      // Tracking (independent)

    // ---- Shared Computeds ----
    withPageContext(),                // Common computed properties (depends on flags)

    // ---- Data Loading ----
    withFeature((store) => withLoad({
        resetClientConfiguration: () => store.resetClientConfiguration(),
        getWorkflowActions: (inode: string) => store.getWorkflowActions(inode),
        graphqlRequest: () => store.graphqlRequest(),
        $graphqlWithParams: store.$graphqlWithParams,
        setGraphqlResponse: (response) => store.setGraphqlResponse(response),
        addHistory: (state) => store.addHistory(state)
    })),  // Load methods (depends on client, workflow)

    // ---- Core Store Methods ----
    withMethods((store) => {
        return {
            setUveStatus(status: UVE_STATUS) {
                patchState(store, {
                    status
                });
            },
            updatePageResponse(pageAPIResponse: DotCMSPageAsset) {
                store.setGraphqlResponse({ pageAsset: pageAPIResponse });
                patchState(store, {
                    status: UVE_STATUS.LOADED,
                    page: pageAPIResponse?.page,
                    site: pageAPIResponse?.site,
                    viewAs: pageAPIResponse?.viewAs,
                    template: pageAPIResponse?.template,
                    urlContentMap: pageAPIResponse?.urlContentMap,
                    containers: pageAPIResponse?.containers,
                    vanityUrl: pageAPIResponse?.vanityUrl,
                    numberContents: pageAPIResponse?.numberContents
                });
            }
        };
    }),

    // ---- UI Features ----
    withLayout(),                     // Layout state
    withZoom(),                       // Zoom state
    withFeature((store) => withView({
        $isPageLocked: () => store.$isPageLocked()
    })),                              // View state - manages view modes (edit vs preview)
    withEditor(),                     // Editor state (uses shared PageContextComputed contract)

    // ---- Actions ----
    withFeature((store) => withSave({
        graphqlRequest: () => store.graphqlRequest(),
        $graphqlWithParams: store.$graphqlWithParams,
        setGraphqlResponse: (response) => store.setGraphqlResponse(response),
        rollbackGraphqlResponse: () => store.rollbackGraphqlResponse(),
        clearHistory: () => store.clearHistory(),
        addHistory: (response) => store.addHistory(response),
        graphqlResponse: () => store.graphqlResponse(),
        $customGraphqlResponse: store.$customGraphqlResponse
    })),  // Save methods (depends on client)
    withFeature((store) => withLock({
        reloadCurrentPage: () => store.reloadCurrentPage()
    })),  // Lock methods (depends on load)
    withComputed(
        ({
            page,
            viewAs,
            pageParams,
            view,
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
                        ...(view().viewParams ?? {})
                    };

                    return normalizeQueryParams(params);
                })
            };
        }
    )
);
