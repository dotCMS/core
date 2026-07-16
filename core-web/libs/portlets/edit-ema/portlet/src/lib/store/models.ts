import { CurrentUser } from '@dotcms/dotcms-js';
import {
    DotCMSWorkflowAction,
    DotDeviceListItem,
    DotExperiment,
    DotLanguage,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotCMSPage } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/types/internal';

import { UVEFlags } from './features/flags/models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import { DotPageAssetParams, SelectedContentlet } from '../shared/models';

/**
 * Page type classification enum
 * Provides semantic clarity over boolean flag
 */
export enum PageType {
    /** Self-hosted by dotCMS - traditional server-side rendering */
    TRADITIONAL = 'traditional',
    /** Headless/client-hosted - external application using APIs */
    HEADLESS = 'headless'
}

export enum IframeAccessMode {
    LOCAL = 'local',
    CROSS_ORIGIN = 'cross-origin'
}

/**
 * View State (transient)
 * Manages editor view modes (edit vs preview) and preview configuration.
 * Controls how the user views the page: edit mode, device preview, or SEO preview.
 */
export interface ViewState {
    device: DotDeviceListItem | null;
    orientation: Orientation | null;
    socialMedia: string | null;

    /**
     * MOVED FROM TOP-LEVEL: viewParams
     * View parameters for device/SEO preview modes
     * Synchronized with device/orientation/socialMedia state
     */
    viewParams: DotUveViewParams | null;

    isEditState: boolean;
    isPreviewModeActive: boolean;
    ogTagsResults: SeoMetaTagsResult[] | null;
}

/**
 * UVE Store State Interface
 *
 * Completely flat state structure following NgRx Signal Store best practices.
 * Each root-level property receives its own Signal wrapper for fine-grained reactivity.
 *
 * State is organized by domain using prefixes:
 * - uve*: Global editor system state (withUve)
 * - flags: Feature flags (withFlags)
 * - page*: Page asset data and metadata (withPage)
 * - workflow*: Workflow actions and lock state (withWorkflow)
 * - editor*: Editor UI state (withEditor)
 * - view*: View modes and preview state (withView)
 */
export interface UVEState {
    // ============ UVE SYSTEM (withUve) ============
    uveStatus: UVE_STATUS;
    uveCurrentUser: CurrentUser | null;

    // ============ FLAGS (withFlags) ============
    // Note: flags added by withFlags feature - kept optional for backwards compatibility
    flags?: UVEFlags;

    // ============ PAGE DOMAIN (withPage) ============
    pageParams: DotPageAssetParams | null;
    pageLanguages: DotLanguage[];
    pageType: PageType;
    iframeAccessMode: IframeAccessMode;
    pageExperiment: DotExperiment | null;
    pageErrorCode: number | null;

    // ============ WORKFLOW (withWorkflow) ============
    workflowActions: DotCMSWorkflowAction[];
    workflowIsLoading: boolean;
    workflowLockIsLoading: boolean;

    // ============ EDITOR STATE (withEditor) ============
    editorDragItem: EmaDragItem | null;
    editorBounds: Container[];
    editorState: EDITOR_STATE;
    editorContentArea: ContentletArea | null;
    /**
     * The currently-selected contentlet: bounds + payload travel together.
     * Bounds drive the persistent overlay border; payload feeds the side
     * panel (quick-edit form, style editor) and the pencil dialog.
     *
     * Set by the SDK's CONTENTLET_CLICKED event (via SET_SELECTED_CONTENTLET)
     * and by the hover toolbar's bolt / palette buttons (via
     * `promoteHoverToSelected`). Re-anchored on every iframe reflow by
     * `withSelectionAnchor.applyBoundsForSelection`. Hides — but doesn't
     * clear — during `$iframeLayoutLocked` phases.
     *
     * Distinct from `editorContentArea` (hovered) — selection persists
     * while the user hovers other contentlets.
     */
    editorSelected: SelectedContentlet | null;
    editorPaletteOpen: boolean;
    editorEditPanelOpen: boolean;
    editorOgTags: SeoMetaTags | null;
    editorStyleSchemas: StyleEditorFormSchema[];

    // ============ VIEW STATE (withView) ============
    // Device and social media preview
    viewDevice: DotDeviceListItem | null;
    viewDeviceOrientation: Orientation | null;
    viewSocialMedia: string | null;
    viewParams: DotUveViewParams | null;
    viewOgTagsResults: SeoMetaTagsResult[] | null;

    // Zoom controls
    viewZoomLevel: number;

    // Iframe size (user-controlled; not derived from content)
    viewIframeWidth: number;
    viewIframeHeight: number;

    // Available canvas viewport size in CSS pixels (excluding padding/gutters).
    // Used to clamp the iframe so its *zoomed* size never exceeds the canvas
    // in responsive mode.
    viewCanvasAvailableWidth: number;
    viewCanvasAvailableHeight: number;
}

/**
 * Normalized Page Domain Data
 * Flattened from pageAPIResponse.page
 */
export interface NormalizedPageState {
    identifier: string;
    title: string;
    pageURI: string;
    inode: string;
    canEdit: boolean;
    canLock: boolean;
    canRead: boolean;
    locked: boolean;
    lockedBy: string | null;
    lockedByName: string | null;
    rendered: string;
    contentType: string;
}

/**
 * Normalized Site Data
 * Flattened from pageAPIResponse.site
 */
export interface NormalizedSiteState {
    identifier: string;
    hostname: string;
}

/**
 * Flattened ViewAs Context
 * Flattened from pageAPIResponse.viewAs
 */
export interface NormalizedViewAsState {
    languageId: number;
    personaId: string | null;
    personaKeyTag: string | null;
    variantId: string | null;
}

export interface TranslateProps {
    page: DotCMSPage;
    currentLanguage: DotLanguage;
}

/**
 * View params persisted in the URL — `null` means "no preset of that kind active".
 * E.g. exiting a device preset clears `device` + `orientation`; switching off SEO
 * preview clears `seo`. Nullable so the store actions can express those clears
 * type-safely.
 */
export interface DotUveViewParams {
    orientation: Orientation | null;
    device: string | null;
    seo: string | null;
}

export enum Orientation {
    LANDSCAPE = 'landscape',
    PORTRAIT = 'portrait'
}
