import {
    DotCMSContentlet,
    DotDevice,
    DotDeviceListItem,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/types/internal';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { CONTAINER_INSERT_ERROR, EDITOR_STATE } from '../../../shared/enums';
import { ActionPayload, ContainerPayload } from '../../../shared/models';
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

/**
 * What the device selector shows: the active device, social-media preview and orientation.
 */
export interface DeviceSelectorState {
    device: DotDevice | null;
    socialMedia: string | null;
    orientation: Orientation | null;
}

/**
 * One change picked in the device selector: a device, a social-media preview or an orientation.
 */
export type DeviceSelectorChange =
    | { type: 'device'; device: DotDevice }
    | { type: 'socialMedia'; socialMedia: string }
    | { type: 'orientation'; orientation: Orientation };

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

/**
 * The tabs of the edit panel: quick edit and the style editor.
 */
export enum UVE_EDIT_PANEL_TABS {
    QUICK_EDIT = 0,
    STYLE_EDITOR = 1
}

/**
 * The selected contentlet as the edit panel shows it, with the container it sits in.
 */
export interface ContentletEditData {
    container: ContainerPayload | undefined;
    contentlet: DotCMSContentlet;
}

/**
 * Why a container refused a contentlet: it is full, holding `maxContentlets` already, or it
 * already holds that contentlet.
 */
export type EditorInsertRefusal =
    | {
          didInsert: false;
          errorCode: CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED;
          maxContentlets: number;
      }
    | { didInsert: false; errorCode: CONTAINER_INSERT_ERROR.DUPLICATE_CONTENT };

/**
 * What `editorInsertContentlet` reports: the contentlet went into the container, or why the
 * container refused it.
 */
export type EditorInsertResult = { didInsert: true } | EditorInsertRefusal;
