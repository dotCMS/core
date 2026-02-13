import { patchState, signalStore, withFeature, withMethods, withState } from '@ngrx/signals';

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
import { withPageAsset } from './features/withPageAsset';
import { withPageContext } from './features/withPageContext';
import { withWorkflow } from './features/workflow/withWorkflow';
import { withViewZoom } from './features/zoom/withZoom';
import { Orientation, PageType, UVEState } from './models';

import { DEFAULT_DEVICE, UVE_FEATURE_FLAGS } from '../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';

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
    // Note: Page asset properties removed (page, site, template, containers, viewAs, vanityUrl, urlContentMap, numberContents)
    // Access via computed signals: store.pageData(), store.pageSite(), store.pageContainers(), etc.
    // Editor state (flattened with editor* prefix)
    editorDragItem: null,
    editorBounds: [],
    editorState: EDITOR_STATE.IDLE,
    editorActiveContentlet: null,
    editorContentArea: null,
    editorPaletteOpen: true,
    editorRightSidebarOpen: false,
    editorOgTags: null,
    editorStyleSchemas: [],
    // View state (flattened with view* prefix)
    viewDevice: DEFAULT_DEVICE,
    viewOrientation: Orientation.LANDSCAPE,
    viewSocialMedia: null,
    viewParams: null,
    viewIsEditState: true,
    viewIsPreviewModeActive: false,
    viewOgTagsResults: null
};

/**
 * Main UVE Store - Unified Visual Editor state management.
 *
 * This store manages all state for the Universal Visual Editor including:
 * - Page content and metadata
 * - Editor UI state (palette, sidebars, drag/drop)
 * - Workflow actions and permissions
 * - Client configuration and time machine (undo/redo)
 *
 * @example
 * ```typescript
 * export class MyComponent {
 *   readonly store = inject(UVEStore);
 *
 *   ngOnInit() {
 *     const page = this.store.pageData();
 *   }
 * }
 * ```
 */
export const UVEStore = signalStore(
    { protectedState: false }, // TODO: remove when the unit tests are fixed
    withState<UVEState>(initialState),

    // ---- Core State Features (no dependencies) ----
    withFlags(UVE_FEATURE_FLAGS),    // Flags first (others may depend on it)
    withClient(),                     // Client config (exposes timeMachine methods)
    withWorkflow(),                   // Workflow state (independent)
    withTrack(),                      // Tracking (independent)

    // ---- Shared Computeds ----
    withPageAsset(),                  // PageAsset computed properties (depends on client state)
    withPageContext(),                // Common computed properties (depends on pageAsset)

    // ---- Data Loading ----
    withFeature((store) => withLoad({
        resetClientConfiguration: () => store.resetClientConfiguration(),
        workflowFetch: (inode: string) => store.workflowFetch(inode),
        requestMetadata: () => store.requestMetadata(),
        $requestWithParams: store.$requestWithParams,
        setPageAssetResponse: (response) => store.setPageAssetResponse(response),
        addHistory: (state) => store.addHistory(state)
    })),  // Page load methods (depends on client, workflow)

    // ---- Core Store Methods ----
    withMethods((store) => {
        return {
            setUveStatus(status: UVE_STATUS) {
                patchState(store, {
                    status
                });
            },
            updatePageResponse(pageAPIResponse: DotCMSPageAsset) {
                // Single source of truth - pageAsset properties accessed via computed signals
                store.setPageAssetResponse({ pageAsset: pageAPIResponse });
                patchState(store, {
                    status: UVE_STATUS.LOADED
                });
            }
        };
    }),

    // ---- UI Features ----
    withLayout(),                     // Layout state
    withViewZoom(),                   // View Zoom state
    withFeature((store) => withView({
        workflowIsPageLocked: () => store.workflowIsPageLocked(),
        pageData: () => store.pageData(),
        pageUrlContentMap: () => store.pageUrlContentMap(),
        pageViewAs: () => store.pageViewAs()
    })),                              // View state - manages view modes (edit vs preview)
    withEditor(),                     // Editor state (uses shared PageContextComputed contract)

    // ---- Actions ----
    withFeature((store) => withSave({
        requestMetadata: () => store.requestMetadata(),
        $requestWithParams: store.$requestWithParams,
        setPageAssetResponse: (response) => store.setPageAssetResponse(response),
        rollbackPageAssetResponse: () => store.rollbackPageAssetResponse(),
        clearHistory: () => store.clearHistory(),
        addHistory: (response) => store.addHistory(response),
        pageAssetResponse: () => store.pageAssetResponse(),
        pageClientResponse: store.pageClientResponse,
        pageData: () => store.pageData(),
        pageTemplate: () => store.pageTemplate()
    })),  // Editor save methods (depends on client)
    withFeature((store) => withLock({
        pageReload: () => store.pageReload()
    }))  // Lock methods (depends on load)

    // ---- Store-level Computeds ----
    // withStoreComputed()  // TODO: Re-add after reducing feature count or merging features
);
