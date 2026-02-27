import { EDITOR_STATE, UVE_STATUS } from '../../shared/enums';
import { Orientation, PageType, UVEState } from '../models';

/**
 * Base UVEState for store tests.
 * Use createInitialUVEState(overrides) to extend/customize per test.
 */
export const BASE_UVE_STATE: UVEState = {
    uveStatus: UVE_STATUS.LOADED,
    uveIsEnterprise: true,
    uveCurrentUser: null,
    flags: {},
    pageParams: null,
    pageLanguages: [],
    pageType: PageType.HEADLESS,
    pageExperiment: null,
    pageErrorCode: null,
    workflowActions: [],
    workflowIsLoading: false,
    workflowLockIsLoading: false,
    editorDragItem: null,
    editorBounds: [],
    editorState: EDITOR_STATE.IDLE,
    editorActiveContentlet: null,
    editorContentArea: null,
    editorPaletteOpen: true,
    editorRightSidebarOpen: false,
    editorOgTags: null,
    editorStyleSchemas: [],
    viewDevice: null,
    viewDeviceOrientation: Orientation.LANDSCAPE,
    viewSocialMedia: null,
    viewParams: null,
    viewOgTagsResults: null,
    viewZoomLevel: 1,
    viewZoomIsActive: false,
    viewZoomIframeDocHeight: 0
};

/**
 * Creates initial UVEState for tests with optional overrides.
 * @param overrides - Partial state to merge over the base
 */
export function createInitialUVEState(overrides?: Partial<UVEState>): UVEState {
    return { ...BASE_UVE_STATE, ...overrides };
}
