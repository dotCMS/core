import {
    DotDeviceListItem,
    DotExperiment,
    DotLanguage,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona } from '@dotcms/types';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE, PALETTE_CLASSES } from '../../../shared/enums';
import { ContentletPayload } from '../../../shared/models';
import { Orientation } from '../../models';

export interface EditorState {
    bounds: Container[];
    state: EDITOR_STATE;
    dragItem?: EmaDragItem;
    ogTags?: SeoMetaTags;
    styleSchemas: StyleSchema[];
    activeContentlet?: ContentletPayload;
    palette: {
        open: boolean;
        currentTab: UVE_PALETTE_TABS;
    };
}

export interface EditorToolbarState {
    device?: DotDeviceListItem;
    socialMedia?: string;
    isEditState: boolean;
    isPreviewModeActive?: boolean;
    orientation?: Orientation;
    ogTagsResults?: SeoMetaTagsResult[];
}

export interface PageDataContainer {
    identifier: string;
    uuid: string;
    contentletsId: string[];
}

export interface PageData {
    containers: PageDataContainer[];
    personalization: string;
    id: string;
    languageId: number;
    personaTag: string;
}

export interface ReloadEditorContent {
    isTraditionalPage: boolean;
}

export interface EditorProps {
    seoResults?: {
        ogTags: SeoMetaTags;
        socialMedia: string;
    };
    iframe: {
        wrapper?: {
            width: string;
            height: string;
        };
        pointerEvents: string;
        opacity: string;
    };
    contentletTools?: {
        contentletArea: ContentletArea;
        hide: boolean;
        isEnterprise: boolean;
        disableDeleteButton?: string;
    };
    dropzone?: {
        bounds: Container[];
        dragItem: EmaDragItem;
    };
    palette?: {
        languageId: number;
        paletteClass: PALETTE_CLASSES;
        pagePath: string;
        variantId?: string;
    };
    showDialogs: boolean;
    progressBar: boolean;
    showBlockEditorSidebar: boolean;
}

/**
 * This is used for model the props of
 * the New UVE Toolbar with Preview Mode and Future Time Machine
 *
 * @export
 * @interface UVEToolbarProps
 */
export interface UVEToolbarProps {
    editor: {
        bookmarksUrl: string;
        apiUrl: string;
    };
    preview?: {
        deviceSelector: {
            apiLink: string;
            hideSocialMedia: boolean;
        };
    };
    runningExperiment?: DotExperiment;
    currentLanguage: DotLanguage;
    workflowActionsInode?: string;
    unlockButton?: {
        inode: string;
        loading: boolean;
    };
    showInfoDisplay?: boolean;
}

export interface PersonaSelectorProps {
    pageId: string;
    value: DotCMSViewAsPersona;
}

export interface StyleSchema {
    contentType: string;
    [key: string]: unknown; // I need to Sync with Kevin's PR to add the specific type
}

export enum UVE_PALETTE_TABS {
    CONTENT_TYPES = 0,
    WIDGETS = 1,
    FAVORITES = 2,
    STYLE_EDITOR = 3
}
