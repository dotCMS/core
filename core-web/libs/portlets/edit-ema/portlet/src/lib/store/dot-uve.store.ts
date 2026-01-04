import { patchState, signalStore, withComputed, withFeature, withMethods, withState } from '@ngrx/signals';

import { computed, untracked } from '@angular/core';

import { DotCMSPageAsset } from '@dotcms/types';

import { withClient } from './features/client/withClient';
import { withSave } from './features/editor/save/withSave';
import { withToolbar } from './features/editor/toolbar/withToolbar';
import { withEditor } from './features/editor/withEditor';
import { withLock } from './features/editor/withLock';
import { withFlags } from './features/flags/withFlags';
import { withLayout } from './features/layout/withLayout';
import { withLoad } from './features/load/withLoad';
import { withTrack } from './features/track/withTrack';
import { withPageContext } from './features/withPageContext';
import { withWorkflow } from './features/workflow/withWorkflow';
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

    // ---- Core State Features (no dependencies) ----
    withFlags(UVE_FEATURE_FLAGS),    // Flags first (others may depend on it)
    withClient(),                     // Client config (independent)
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
        setGraphqlResponse: (response) => store.setGraphqlResponse(response)
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

    // ---- UI Features ----
    withLayout(),                     // Layout state
    withFeature((store) => withToolbar({
        $isPageLocked: () => store.$isPageLocked()
    })),                              // Toolbar state (depends on flags, pageContext)
    withFeature((store) => withEditor({
        $isEditState: () => store.toolbar().isEditState,
        isEnterprise: () => store.isEnterprise()
    })),                              // Editor state (depends on pageContext, toolbar)

    // ---- Actions ----
    withFeature((store) => withSave({
        graphqlRequest: () => store.graphqlRequest(),
        $graphqlWithParams: store.$graphqlWithParams,
        setGraphqlResponse: (response) => store.setGraphqlResponse(response)
    })),  // Save methods (depends on client)
    withFeature((store) => withLock({
        reloadCurrentPage: () => store.reloadCurrentPage()
    })),  // Lock methods (depends on load)
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
