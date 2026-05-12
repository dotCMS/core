import { DotDeviceListItem, SeoMetaTags, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/types/internal';

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
        // currentTab is now managed as local state via signalState in DotUvePaletteComponent
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
    code: string | undefined;
    pageType: PageType;
    enableInlineEdit: boolean;
    /**
     * Reference to the page-asset response. Used as part of the equal
     * comparator so the effect fires when contentlets are edited /
     * removed / dragged (the asset reference changes via setPageAsset)
     * but not on unrelated signal cycles where every field is identical.
     */
    pageAssetRef: unknown;
}

export interface PersonaSelectorProps {
    pageId: string;
    value: DotCMSViewAsPersona;
}

export enum UVE_PALETTE_TABS {
    CONTENT_TYPES = 0,
    WIDGETS = 1,
    FAVORITES = 2,
    LAYERS = 3
}
