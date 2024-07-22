import {
    DotCMSContentlet,
    DotExperiment,
    DotLanguage,
    DotPageContainerStructure,
    DotPersona
} from '@dotcms/dotcms-models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';

export interface EditorState {
    $bounds: Container[];
    $state: EDITOR_STATE;
    $contentletArea?: ContentletArea;
    $dragItem?: EmaDragItem;
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
    languageId: string | number;
    personaTag: string;
}

export interface ReloadEditorContent {
    code: string;
    isTraditionalPage: boolean;
    isEditState: boolean;
    isEnterprise: boolean;
}

export interface EditorProps {
    seoTools?: {
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
        languageId: string | number;
        containers: DotPageContainerStructure;
        variantId: string;
    };
    dialogs: boolean;
    progressBar: boolean;
    showEditorContent: boolean;
}

export interface ToolbarProps {
    deviceSelector: {
        apiLink: string;
        hideSocialMedia: boolean;
    };
    urlContentMap?: DotCMSContentlet;
    bookmarksUrl: string;
    copyUrlButton: {
        pureURL: string;
    };
    apiLinkButton: {
        apiURL: string;
    };
    experimentBadge?: {
        runningExperiment: DotExperiment;
    };
    languageSelector: {
        currentLanguage: DotLanguage;
    };
    personaSelector: {
        pageId: string;
        value: DotPersona;
    };
    workflowActions?: {
        inode: string;
    };
    unlockButton?: {
        inode: string;
        loading: boolean;
    };
    showInfoDisplay: boolean;
}
