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
    uveIsEnterprise: boolean;
    uveCurrentUser: CurrentUser | null;

    // ============ FLAGS (withFlags) ============
    // Note: flags added by withFlags feature - kept optional for backwards compatibility
    flags?: UVEFlags;

    // ============ PAGE DOMAIN (withPage) ============
    pageParams: DotPageAssetParams | null;
    pageLanguages: DotLanguage[];
    pageType: PageType;
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
    editorActiveContentlet: ActionPayload | null;
    editorContentArea: ContentletArea | null;
    editorPaletteOpen: boolean;
    editorRightSidebarOpen: boolean;
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
    viewZoomIframeDocHeight: number;
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
