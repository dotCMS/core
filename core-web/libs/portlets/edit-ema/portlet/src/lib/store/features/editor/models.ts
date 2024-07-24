import {
    DotCMSContentlet,
    DotExperiment,
    DotLanguage,
    DotPageContainerStructure,
    DotPersona,
    SeoMetaTags
} from '@dotcms/dotcms-models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';
import { DotDeviceWithIcon } from '../../../shared/models';

export interface EditorState {
    bounds: Container[];
    state: EDITOR_STATE;
    contentletArea?: ContentletArea;
    dragItem?: EmaDragItem;
    ogTags?: SeoMetaTags;
}

export interface EditorToolbarState {
    device?: DotDeviceWithIcon;
    socialMedia?: string;
    isEditState: boolean;
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
    code: string;
    isTraditionalPage: boolean;
    isEditState: boolean;
    isEnterprise: boolean;
}

export interface EditorProps {
    seoResults?: {
        ogTags: SeoMetaTags;
        socialMedia: string;
    };
    iframe: {
        wrapper: {
            isDevice: boolean;
            width: string;
            height: string;
        };
        state: EDITOR_STATE;
        src: string;
        pointerEvents: string;
        opacity: string;
    };

    contentletTools?: {
        contentletArea: ContentletArea;
        hide: boolean;
        isEnterprise: boolean;
    };
    dropzone?: {
        bounds: Container[];
        dragItem: EmaDragItem;
    };
    palette?: {
        languageId: number;
        containers: DotPageContainerStructure;
        variantId: string;
    };
    dialogs: boolean;
    progressBar: boolean;
    showEditorContent: boolean;
}

export interface ToolbarProps {
    urlContentMap?: DotCMSContentlet;
    bookmarksUrl: string;
    copyUrl: string;
    apiUrl: string;
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
