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

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../edit-ema-editor/components/ema-page-dropzone/types';
import { UVE_STATUS, EDITOR_STATE } from '../shared/enums';
import { ClientData, ContentletPayload, DotPageAssetParams } from '../shared/models';

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
 * Toolbar UI State (transient)
 * Manages toolbar-specific preview/device state
 */
export interface ToolbarUIState {
    device: DotDeviceListItem | null;
    orientation: Orientation | null;
    socialMedia: string | null;
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

    // ============ UI STATE (Transient) ============
    // Phase 3.2: Nested UI state for better organization
    editor: EditorUIState;
    toolbar: ToolbarUIState;

    // Other UI state
    viewParams?: DotUveViewParams;
    isTraditionalPage: boolean;
    isClientReady: boolean;
    selectedPayload?: Pick<ClientData, 'container' | 'contentlet'>;
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
