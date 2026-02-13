import { CurrentUser } from '@dotcms/dotcms-js';
import {
    DotCMSWorkflowAction,
    DotDeviceListItem,
    DotExperiment,
    DotLanguage,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import {
    DotCMSLayout,
    DotCMSPage,
    DotCMSPageAssetContainers,
    DotCMSSite,
    DotCMSTemplate,
    DotCMSURLContentMap,
    DotCMSVanityUrl,
    DotCMSViewAs
} from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { UVEFlags } from './features/flags/models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import { ActionPayload, DotPageAssetParams } from '../shared/models';

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
    activeContentlet: ActionPayload | null;
    contentArea: ContentletArea | null;

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
    ogTags: SeoMetaTags | null;
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

    // Note: Page asset computed signals (pageData, pageSite, pageContainers, pageTemplate, etc.)
    // are added by withPageAsset feature and not declared here. TypeScript will infer them
    // from the feature composition.

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
    // Phase 6: Flattened editor state with domain-prefixed properties (editor*)
    /**
     * Editor drag and drop state
     */
    editorDragItem: EmaDragItem | null;
    editorBounds: Container[];
    editorState: EDITOR_STATE;

    /**
     * Editor contentlet management
     */
    editorActiveContentlet: ActionPayload | null;
    editorContentArea: ContentletArea | null;

    /**
     * Editor UI panel preferences (user-configurable)
     */
    editorPaletteOpen: boolean;
    editorRightSidebarOpen: boolean;

    /**
     * Editor-specific data
     */
    editorOgTags: SeoMetaTags | null;
    editorStyleSchemas: StyleEditorFormSchema[];

    // ============ VIEW STATE (Flattened from ViewState with view* prefix) ============
    /**
     * View device - device preview mode
     */
    viewDevice: DotDeviceListItem | null;

    /**
     * View orientation - device orientation for preview
     */
    viewOrientation: Orientation | null;

    /**
     * View social media - SEO/social media preview mode
     */
    viewSocialMedia: string | null;

    /**
     * View parameters - device/SEO preview mode parameters
     * Synchronized with viewDevice/viewOrientation/viewSocialMedia state
     */
    viewParams: DotUveViewParams | null;

    /**
     * Is edit state - whether editor is in edit mode (vs preview mode)
     */
    viewIsEditState: boolean;

    /**
     * Is preview mode active - whether preview mode is currently active
     */
    viewIsPreviewModeActive: boolean;

    /**
     * OG tags results - SEO/social media preview tag results
     */
    viewOgTagsResults: SeoMetaTagsResult[] | null;

    // Note: isClientReady removed from UVEState (only in ClientConfigState via withClient)
    // Note: editor nested object removed - flattened to editor* prefixed properties
    // Note: view nested object removed - flattened to view* prefixed properties
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
