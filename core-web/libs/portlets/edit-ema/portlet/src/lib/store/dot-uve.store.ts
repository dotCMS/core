import { signalStore, withFeature, withMethods, withState } from '@ngrx/signals';

import { DotCMSPageAsset } from '@dotcms/types';

import { withSave } from './features/editor/save/withSave';
import { withView } from './features/editor/toolbar/withView';
import { withEditor } from './features/editor/withEditor';
import { withFlags } from './features/flags/withFlags';
import { withLayout } from './features/layout/withLayout';
import { withLoad } from './features/load/withLoad';
import { withPage } from './features/page/withPage';
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
    withUve(),
    withFlags(UVE_FEATURE_FLAGS),
    withPage(),
    withTrack(),
    withWorkflow(),
    withFeature((store) => withLoad({
        resetClientConfiguration: () => store.resetClientConfiguration(),
        workflowFetch: (inode: string) => store.workflowFetch(inode),
        requestMetadata: () => store.requestMetadata(),
        $requestWithParams: store.$requestWithParams,
        setPageAssetResponse: (response) => store.setPageAssetResponse(response),
        addHistory: (state) => store.addToHistory(state)
    })),
    withMethods((store) => {
        return {
            updatePageResponse(pageAPIResponse: DotCMSPageAsset) {
                // Single source of truth - pageAsset properties accessed via computed signals
                store.setPageAssetResponse({ pageAsset: pageAPIResponse });
                store.setUveStatus(UVE_STATUS.LOADED);
            }
        };
    }),
    withLayout(),
    withView(),
    withEditor(),
    withFeature((store) => withSave({
        requestMetadata: () => store.requestMetadata(),
        $requestWithParams: () => store.$requestWithParams(),
        setPageAssetResponse: (response) => store.setPageAssetResponse(response),
        rollbackPageAssetResponse: () => store.rollbackPageAssetResponse(),
        clearHistory: () => store.clearHistory(),
        addHistory: (response) => store.addToHistory(response),
        pageAssetResponse: () => store.pageAssetResponse(),
        pageClientResponse: () => store.pageClientResponse(),
        pageData: () => store.pageData(),
        pageTemplate: () => store.pageTemplate()
    }))
);
