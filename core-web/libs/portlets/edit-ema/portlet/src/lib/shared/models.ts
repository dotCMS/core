import { DotDevice } from '@dotcms/dotcms-models';
import { InfoPage } from '@dotcms/ui';

import { CommonErrors, DialogStatus, FormStatus } from './enums';

import { DotPageApiParams } from '../services/dot-page-api.service';

export interface InfoOptions {
    icon: string;
    info: {
        message: string;
        args: string[];
    };
    id: string;
    actionIcon?: string;
}

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

export interface NavigationBarItem {
    icon?: string;
    iconURL?: string;
    label: string;
    href?: string;
    id: string;
    isDisabled?: boolean;
    tooltip?: string;
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
    working?: boolean;
    workingInode?: string;
}

export interface DotDeviceWithIcon extends DotDevice {
    icon?: string;
}

export type CommonErrorsInfo = Record<CommonErrors, InfoPage>;

export interface DialogForm {
    status: FormStatus;
    isTranslation: boolean;
}

export interface DialogAction {
    event: CustomEvent;
    payload: ActionPayload;
    form: DialogForm;
}

export type DialogType = 'content' | 'form' | 'widget' | null;

export interface EditEmaDialogState {
    header: string;
    status: DialogStatus;
    url: string;
    type: DialogType;
    payload?: ActionPayload;
    editContentForm: DialogForm;
}

// We can modify this if we add more events, for now I think is enough
export interface CreateFromPaletteAction {
    variable: string;
    name: string;
    payload: ActionPayload;
}

export interface EditContentletPayload {
    inode: string;
    title: string;
}

export interface CreateContentletAction {
    url: string;
    contentType: string;
    payload: ActionPayload;
}
