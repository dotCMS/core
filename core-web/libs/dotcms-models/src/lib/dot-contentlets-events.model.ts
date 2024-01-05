import { DotCMSContentType, DotPageContainer, DotPageContent } from '@dotcms/dotcms-models';

export interface DotContentletEvent<T = Record<string, unknown>> {
    name: string;
    data?: T;
}

export type DotContentletEventDragAndDropDotAsset = DotContentletEvent<DotAssetPayload>;

export type DotContentletEventAddContentType = DotContentletEvent<DotAddContentTypePayload>;

export type DotContentletEventRelocate = DotContentletEvent<DotRelocatePayload>;

export type DotContentletEventReorder = DotContentletEvent<DotPageContainer>;

export type DotContentletEventSelect = DotContentletEvent<DotPageContent>;

export type DotContentletEventSave = DotContentletEvent<DotPageContent>;

export type DotContentletEventContent = DotContentletEvent<DotContentletPayload>;

export interface DotAssetPayload {
    contentlet: DotPageContent;
    placeholderId: string;
}

export interface DotRelocatePayload {
    container: DotPageContainer;
    contentlet: DotPageContent;
}

export interface DotInlineDataset {
    mode: string;
    inode: string;
    fieldName: string;
    language: string;
}

export interface DotInlineEditContent {
    innerHTML: string;
    dataset: DotInlineDataset;
    element: HTMLElement;
    isNotDirty: boolean;
    eventType: string;
}

export interface DotShowCopyModal {
    container: HTMLElement;
    contentlet: HTMLElement;
    selector?: string;
    initEdit: (element: HTMLElement) => void;
}

export interface DotContentletContainer {
    dotObject: string;
    dotInode: string;
    dotIdentifier: string;
    dotUuid: string;
    maxContentlets: string;
    dotAcceptTypes: string;
    dotCanAdd: string;
}

export interface DotContentletDataset {
    dotObject: string;
    dotAction?: string;
    dotAdd?: string;
    dotIdentifier: string;
    dotUuid?: string;
    dotInode?: string;
}

export interface DotAddContentTypePayload {
    container: DotContentletContainer | null;
    contentType: DotCMSContentType;
}

export interface DotContentletPayload {
    container: DotContentletContainer;
    dataset: DotContentletDataset;
}
