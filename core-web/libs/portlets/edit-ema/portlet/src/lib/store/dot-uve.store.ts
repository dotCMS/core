import { signalStore, withFeature, withMethods, withState } from '@ngrx/signals';

import { DotCMSPageAsset } from '@dotcms/types';

import { withView } from './features/editor/toolbar/withView';
import { withEditor } from './features/editor/withEditor';
import { withFlags } from './features/flags/withFlags';
import { withLayout } from './features/layout/withLayout';
import { withPage } from './features/page/withPage';
import { withPageApi } from './features/page-api/withPageApi';
import { withTrack } from './features/track/withTrack';
import { withUve } from './features/uve/withUve';
import { withWorkflow } from './features/workflow/withWorkflow';
import { Orientation, PageType, UVEState } from './models';

import { DEFAULT_DEVICE, UVE_FEATURE_FLAGS } from '../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';

// Some properties can be computed
// Ticket: https://github.com/dotCMS/core/issues/30760
const initialState: UVEState = {
    // UVE system state (managed by withUve)
    uveStatus: UVE_STATUS.LOADING,
    uveIsEnterprise: false,
    uveCurrentUser: null,
    // Flags (managed by withFlags)
    flags: {}, // Will be populated by withFlags feature
    // Page state (managed by withPage)
    pageParams: null,
    pageLanguages: [],
    pageType: PageType.TRADITIONAL,
    pageExperiment: null,
    pageErrorCode: null,
    // Workflow state (managed by withWorkflow)
    workflowActions: [],
    workflowIsLoading: false,
    workflowLockIsLoading: false,
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
    // View state (device, orientation, social media, zoom)
    viewDevice: DEFAULT_DEVICE,
    viewDeviceOrientation: Orientation.LANDSCAPE,
    viewSocialMedia: null,
    viewParams: null,
    viewOgTagsResults: null,
    viewZoomLevel: 1,
    viewZoomIsActive: false,
    viewZoomIframeDocHeight: 0,
    viewZoomGestureStartZoom: 1
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
 * ## Feature Composition Order (CRITICAL - Do not reorder without reviewing dependencies)
 *
 * 1. withState - Base state
 * 2. withUve - Global system state (status, enterprise, user)
 * 3. withFlags - Feature flags
 * 4. withPage - Page data + history (composes withHistory)
 * 5. withTrack - Analytics (standalone)
 * 6. withWorkflow - Workflow + lock (needs PageAssetComputed, uses pageReload via type assertion)
 * 7. withMethods - updatePageResponse helper
 * 8. withLayout - Layout operations (needs page data)
 * 9. withView - View modes + zoom (needs page params)
 * 10. withEditor - Editor UI (needs PageAssetComputed, WorkflowComputed, ViewComputed)
 * 11. withPageApi - Backend API (needs all above, provides pageReload)
 *
 * Note: Circular dependency exists between withWorkflow and withPageApi:
 * - withWorkflow needs pageReload() from withPageApi (accessed via type assertion)
 * - withPageApi needs workflowFetch() from withWorkflow (accessed via dependency injection)
 * Current order is optimal - do not change without addressing this circular dependency.
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
    // 1. Base state
    withState<UVEState>(initialState),
    // 2. Global system state
    withUve(),
    // 3. Feature flags
    withFlags(UVE_FEATURE_FLAGS),
    // 4. Page data + history
    withPage(),
    // 5. Analytics
    withTrack(),
    // 6. Workflow + lock
    withWorkflow(),
    // 7. Helper methods
    withMethods((store) => {
        return {
            updatePageResponse(pageAPIResponse: DotCMSPageAsset) {
                // Single source of truth - pageAsset properties accessed via computed signals
                store.setPageAssetResponse({ pageAsset: pageAPIResponse });
                store.setUveStatus(UVE_STATUS.LOADED);
            }
        };
    }),
    // 8. Layout operations
    withLayout(),
    // 9. View modes + zoom
    withView(),
    // 10. Editor UI
    withEditor(),
    // 11. Backend API (must be last - needs all dependencies above)
    withFeature((store) => withPageApi({
        // Client configuration
        resetClientConfiguration: () => store.resetClientConfiguration(),

        // Workflow
        workflowFetch: (inode: string) => store.workflowFetch(inode),

        // Request metadata
        requestMetadata: () => store.requestMetadata(),
        $requestWithParams: store.$requestWithParams,

        // Page asset management
        setPageAssetResponse: (response) => store.setPageAssetResponse(response),
        rollbackPageAssetResponse: () => store.rollbackPageAssetResponse(),

        // History management
        clearHistory: () => store.clearHistory(),
        addHistory: (response) => store.addToHistory(response),

        // Page accessors
        pageAssetResponse: () => store.pageAssetResponse(),
        pageClientResponse: () => store.pageClientResponse(),
        pageData: () => store.pageData(),
        pageTemplate: () => store.pageTemplate()
    }))
);
