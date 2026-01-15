import {
    DotDeviceListItem,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/uve';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';
import { ActionPayload } from '../../../shared/models';
import { Orientation, PageType } from '../../models';

export interface EditorState {
    bounds: Container[];
    state: EDITOR_STATE;
    styleSchemas: StyleEditorFormSchema[];
    dragItem?: EmaDragItem;
    ogTags?: SeoMetaTags;
    activeContentlet?: ActionPayload;
    contentArea?: ContentletArea;
    palette: {
        open: boolean;
        // currentTab removed - now managed locally in DotUvePaletteComponent
    };
    rightSidebar: {
        open: boolean;
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
    pageType: PageType;
}

export interface PersonaSelectorProps {
    pageId: string;
    value: DotCMSViewAsPersona;
}

export enum UVE_PALETTE_TABS {
    CONTENT_TYPES = 0,
    WIDGETS = 1,
    FAVORITES = 2,
    STYLE_EDITOR = 4,
    LAYERS = 3
}
