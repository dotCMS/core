import { CurrentUser } from '@dotcms/dotcms-js';
import {
    DotCMSWorkflowAction,
    DotDeviceListItem,
    DotExperiment,
    DotLanguage,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import {
    DotCMSPage,
    DotCMSSite,
    DotCMSViewAs,
    DotCMSTemplate,
    DotCMSLayout,
    DotCMSURLContentMap,
    DotCMSPageAssetContainers,
    DotCMSVanityUrl
} from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { UVEFlags } from './features/flags/models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../edit-ema-editor/components/ema-page-dropzone/types';
import { UVE_STATUS, EDITOR_STATE } from '../shared/enums';
import { ClientData, ContentletPayload, DotPageAssetParams } from '../shared/models';

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

/**
 * Phase 3.1: UI State Interfaces
 * Clearly separate transient UI state from persistent domain state
 */

/**
 * Editor UI State (transient)
 * Manages editor-specific UI state like drag/drop, palette, sidebar
 */
export interface EditorUIState {
    // Drag and drop state
    dragItem: EmaDragItem | null;
    bounds: Container[];
    state: EDITOR_STATE;

    // Contentlet management
    activeContentlet: ContentletPayload | null;
    contentArea: ContentletArea | null;

    /**
     * Currently selected contentlet for quick-edit sidebar
     * MOVED FROM TOP-LEVEL: selectedPayload → selectedContentlet
     */
    selectedContentlet: Pick<ClientData, 'container' | 'contentlet'> | null;

    // UI panel preferences (user-configurable)
    panels: {
        palette: {
            open: boolean;
        };
        rightSidebar: {
            open: boolean;
        };
    };

    // Editor-specific data
    ogTags: any | null;
    styleSchemas: StyleEditorFormSchema[];
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
 * Main UVE Store State
 * Restructured to clearly separate domain state, UI state, and deprecated properties
 */
export interface UVEState {
    // ============ DOMAIN STATE (Source of Truth) ============
    // Core page data
    languages: DotLanguage[];
    isEnterprise: boolean;
    flags?: UVEFlags; // Feature flags (added by withFlags feature)
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    pageParams?: DotPageAssetParams;
    workflowActions?: DotCMSWorkflowAction[];

    // Normalized page response (Phase 1: Flattened structure)
    // Required properties (null during loading/error, populated when loaded)
    page: DotCMSPage | null;
    site: DotCMSSite | null;
    template: DotCMSTemplate | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'> | null;
    layout: DotCMSLayout | null;
    containers: DotCMSPageAssetContainers | null;

    // Optional properties (from API - may not be present even when loaded)
    viewAs?: DotCMSViewAs;
    vanityUrl?: DotCMSVanityUrl;
    urlContentMap?: DotCMSURLContentMap;
    numberContents?: number;

    // Status
    status: UVE_STATUS;
    errorCode?: number;

    /**
     * Page type classification - replaces isTraditionalPage boolean
     * - TRADITIONAL: Self-hosted by dotCMS (iframe src = '', traditional HTML)
     * - HEADLESS: External client-hosted (iframe src = clientHost, uses APIs)
     *
     * Set at page load based on presence of clientHost parameter
     * @readonly Conceptually immutable after initial load
     */
    pageType: PageType;

    // ============ UI STATE (Transient) ============
    // Phase 3.2: Nested UI state for better organization
    /**
     * Editor UI state - transient user interactions
     * Includes drag/drop state, selected contentlet, panel preferences
     */
    editor: EditorUIState;

    /**
     * View state - editor view modes (edit vs preview) and preview configuration
     * Includes device state, orientation, SEO preview, and view parameters
     */
    view: ViewState;

    // Note: isClientReady removed from UVEState (only in ClientConfigState via withClient)
    // Note: viewParams moved to view.viewParams
    // Note: selectedPayload renamed to editor.selectedContentlet
}

/**
 * Phase 3.1: Normalized State Interfaces
 * Flatten the pageAPIResponse structure for easier access
 */

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

export interface DotUveViewParams {
    orientation: Orientation;
    device: string;
    seo: string;
}

export enum Orientation {
    LANDSCAPE = 'landscape',
    PORTRAIT = 'portrait'
}
