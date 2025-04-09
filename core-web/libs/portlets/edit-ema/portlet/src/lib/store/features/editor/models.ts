import {
    DotCMSContentlet,
    DotDeviceListItem,
    DotExperiment,
    DotLanguage,
    DotPageContainerStructure,
    DotPersona,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';
import { Orientation } from '../../models';

export interface EditorState {
    bounds: Container[];
    state: EDITOR_STATE;
    contentletArea?: ContentletArea;
    dragItem?: EmaDragItem;
    ogTags?: SeoMetaTags;
    paletteOpen: boolean;
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
    isClientReady: boolean;
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
        containers: DotPageContainerStructure;
        variantId: string;
        shouldShowPalette: boolean;
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
    value: DotPersona;
}
