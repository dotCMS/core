import { ActionPayload, ClientData, DraggedPayload } from '../../../shared/models';

export interface ContentletArea {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: ActionPayload;
}

export interface ClientContentletArea {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: ClientData;
}

export interface Container {
    x: number;
    y: number;
    width: number;
    height: number;
    contentlets: ContentletArea[];
    payload: ClientData | string;
}

export interface Column {
    x: number;
    y: number;
    width: number;
    height: number;
    containers: Container[];
}

/**
 * Where a drop goes, as the dropzone resolves it from the pointer: next to a contentlet, into a
 * container that holds nothing, or nowhere. `none` covers the dragged contentlet's own place and
 * the two places next to it, which leave the order as it is, and a pointer that left the targets.
 */
export type EmaDropTarget =
    | { kind: 'contentlet'; payload: ClientData; position: 'before' | 'after' }
    | { kind: 'empty-container'; payload: ClientData }
    | { kind: 'none' };

export interface EmaDragItem {
    baseType: string;
    contentType: string;
    draggedPayload: DraggedPayload;
}

export interface Row {
    x: number;
    y: number;
    width: number;
    height: number;
    columns: Column[];
}

export type EmaPageDropzoneItem = Row | Column | Container | ContentletArea;

export interface CopyContentletPayload {
    fieldName: string;
    inode: string;
    language: string;
    mode: string;
}

export interface InlineEditingContentletDataset {
    language: string;
    mode: string;
    inode: string;
    fieldName: string;
}

export interface UpdatedContentlet {
    dataset: InlineEditingContentletDataset;
    content: string;
    eventType: string;
    isNotDirty: boolean;
}
