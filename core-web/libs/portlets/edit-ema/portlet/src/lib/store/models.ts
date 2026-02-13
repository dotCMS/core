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
    DotCMSPage
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

export interface UVEState {
    // ============ DOMAIN STATE ============
    languages: DotLanguage[];
    isEnterprise: boolean;
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    pageParams?: DotPageAssetParams;
    workflowActions?: DotCMSWorkflowAction[];
    status: UVE_STATUS;
    errorCode?: number;
    pageType: PageType;

    // Note: flags added by withFlags feature - kept optional for backwards compatibility
    flags?: UVEFlags;

    // ============ EDITOR STATE ============
    editorDragItem: EmaDragItem | null;
    editorBounds: Container[];
    editorState: EDITOR_STATE;
    editorActiveContentlet: ActionPayload | null;
    editorContentArea: ContentletArea | null;
    editorPaletteOpen: boolean;
    editorRightSidebarOpen: boolean;
    editorOgTags: SeoMetaTags | null;
    editorStyleSchemas: StyleEditorFormSchema[];

    // ============ VIEW STATE ============
    viewDevice: DotDeviceListItem | null;
    viewOrientation: Orientation | null;
    viewSocialMedia: string | null;
    viewParams: DotUveViewParams | null;
    viewIsEditState: boolean;
    viewIsPreviewModeActive: boolean;
    viewOgTagsResults: SeoMetaTagsResult[] | null;
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

export interface DotUveViewParams {
    orientation: Orientation;
    device: string;
    seo: string;
}

export enum Orientation {
    LANDSCAPE = 'landscape',
    PORTRAIT = 'portrait'
}
