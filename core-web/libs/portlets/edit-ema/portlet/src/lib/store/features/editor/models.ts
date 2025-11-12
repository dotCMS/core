import {
    DotCMSContentlet,
    DotDeviceListItem,
    DotExperiment,
    DotLanguage,
    DotPersona,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona } from '@dotcms/types';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';
import { Orientation } from '../../models';

export enum PALETTE_TABS {
    CONTENTTYPE = 0,
    WIDGETS = 1,
    FAVORITES = 2,
    STYLE_EDITOR = 3
}

export interface EditorState {
    activeContentletIdentifier: string | null;
    bounds: Container[];
    state: EDITOR_STATE;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    styleConfigurations: Record<string, any>;
    contentletArea?: ContentletArea;
    dragItem?: EmaDragItem;
    ogTags?: SeoMetaTags;
    palette?: {
        isOpen: boolean;
        currentTab: PALETTE_TABS;
        styleConfig: Record<string, unknown>;
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
    showDialogs: boolean;
    progressBar: boolean;
    showBlockEditorSidebar: boolean;
}

export interface ToolbarProps {
    urlContentMap?: DotCMSContentlet;
    bookmarksUrl: string;
    copyUrl: string;
    apiUrl: string;
    isDefaultVariant: boolean;
    showInfoDisplay: boolean;
    currentLanguage: DotLanguage;
    runningExperiment?: DotExperiment;
    workflowActionsInode?: string;
    personaSelector: {
        pageId: string;
        value: DotPersona;
    };
    unlockButton?: {
        inode: string;
        loading: boolean;
    };
    deviceSelector: {
        apiLink: string;
        hideSocialMedia: boolean;
    };
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
        copyUrl: string;
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
