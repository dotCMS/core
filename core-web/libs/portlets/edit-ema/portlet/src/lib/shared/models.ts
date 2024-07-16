import { DotDevice, DotExperiment, DotLanguage } from '@dotcms/dotcms-models';

import { EDITOR_MODE, EDITOR_STATE } from './enums';

import {
    ClientContentletArea,
    Container,
    ContentletArea,
    EmaDragItem,
    UpdatedContentlet
} from '../edit-ema-editor/components/ema-page-dropzone/types';
import { DotPageApiParams, DotPageApiResponse } from '../services/dot-page-api.service';

export interface VTLFile {
    inode: string;
    name: string;
}

export interface ClientData {
    contentlet?: ContentletPayload;
    container: ContainerPayload;
    newContentlet?: ContentletPayload;
    vtlFiles?: VTLFile[];
}

export interface PositionPayload extends ClientData {
    position?: 'before' | 'after';
}

export interface ReorderPayload {
    reorderUrl: string;
}

export interface ActionPayload extends PositionPayload {
    language_id: string;
    pageContainers: PageContainer[];
    pageId: string;
    personaTag?: string;
    newContentletId?: string;
}

export interface PageContainer {
    personaTag?: string;
    identifier: string;
    uuid: string;
    contentletsId: string[];
}

export interface ContainerPayload {
    acceptTypes: string;
    identifier: string;
    contentletsId?: string[];
    maxContentlets: number;
    variantId: string;
    uuid: string;
}

export interface ContentletPayload {
    identifier: string;
    inode: string;
    title: string;
    contentType: string;
    baseType?: string;
    onNumberOfPages?: number;
}

export interface SetUrlPayload {
    url: string;
}

export interface SavePagePayload {
    pageContainers: PageContainer[];
    params?: DotPageApiParams;
    pageId: string;
    whenSaved?: () => void;
}

export interface ReloadPagePayload {
    params: DotPageApiParams;
    whenReloaded?: () => void;
}

export interface NavigationBarItem {
    icon?: string;
    iconURL?: string;
    label: string;
    href?: string;
    id: string;
    isDisabled?: boolean;
}

export interface EditorData {
    mode: EDITOR_MODE;
    device?: DotDevice & { icon?: string };
    socialMedia?: string;
    canEditVariant?: boolean;
    canEditPage?: boolean;
    variantId?: string;
    page?: {
        isLocked: boolean;
        canLock: boolean;
        lockedByUser: string;
    };
}

export interface EditEmaState {
    clientHost: string;
    error?: number;
    editor: DotPageApiResponse;
    isEnterpriseLicense: boolean;
    editorState: EDITOR_STATE;
    bounds: Container[];
    contentletArea: ContentletArea;
    editorData: EditorData;
    currentExperiment?: DotExperiment;
    dragItem?: EmaDragItem;
    shouldReload: boolean;
    languages: DotLanguage[];
}

export interface MessageInfo {
    summary: string;
    detail: string;
}

export interface WorkflowActionResult extends MessageInfo {
    workflowName: string;
    callback: string;
    args: unknown[];
}

export type PostMessagePayload =
    | ActionPayload
    | SetUrlPayload
    | Container[]
    | ClientContentletArea
    | ReorderPayload
    | UpdatedContentlet;

export interface DeletePayload {
    payload: ActionPayload;
    originContainer: ContainerPayload;
    contentletToMove: ContentletPayload;
}

export interface InsertPayloadFromDelete {
    payload: ActionPayload;
    pageContainers: PageContainer[];
    contentletsId: string[];
    destinationContainer: ContainerPayload;
    pivotContentlet: ContentletPayload;
    positionToInsert: 'before' | 'after';
}

export interface BasePayload {
    type: 'contentlet' | 'content-type' | 'temp';
}

export interface TempDragPayload extends BasePayload {
    type: 'temp';
}

export interface ContentletDragPayload extends BasePayload {
    type: 'contentlet';
    item: {
        container?: ContainerPayload;
        contentlet: ContentletPayload;
    };
    move: boolean;
}

export interface DragDataset extends BasePayload {
    item: string;
}

export interface DragDatasetItem {
    container?: ContainerPayload;
    contentlet?: ContentletPayload;
    contentType?: {
        variable: string;
        name: string;
        baseType: string;
    };
    move: boolean;
}

// Specific  interface when type is 'content-type'
export interface ContentTypeDragPayload extends BasePayload {
    type: 'content-type';
    item: {
        variable: string;
        name: string;
    };
}

export type DraggedPayload = ContentletDragPayload | ContentTypeDragPayload | TempDragPayload;

export interface SaveInlineEditing {
    contentlet: { [fieldName: string]: string; inode: string };
    params: DotPageApiParams;
}

export interface DotPage {
    title: string;
    identifier: string;
    inode: string;
    canEdit: boolean;
    canRead: boolean;
    canLock?: boolean;
    locked?: boolean;
    lockedBy?: string;
    lockedByName?: string;
    pageURI: string;
    rendered?: string;
    contentType: string;
    live: boolean;
    liveInode?: string;
    stInode?: string;
}
