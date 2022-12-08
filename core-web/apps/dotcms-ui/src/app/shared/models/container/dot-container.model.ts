export enum CONTAINER_SOURCE {
    FILE = 'FILE',
    DB = 'DB'
}

export interface DotContainerEntity {
    container: DotContainer;
    contentTypes: DotContainerStructure[];
}

export interface DotContainer {
    archived?: boolean;
    categoryId?: string;
    deleted?: boolean;
    live?: boolean;
    working?: boolean;
    locked?: boolean;
    friendlyName?: string;
    path?: string;
    identifier?: string;
    name?: string;
    type?: string;
    title?: string;
    source?: CONTAINER_SOURCE;
    maxContentlets?: number;
    preLoop?: string;
    postLoop?: string;
    code?: string;
    parentPermissionable?: {
        hostname: string;
    };
    disableInteraction?: boolean;
}

export interface DotContainerPayload {
    identifier: string;
    title: string;
    friendlyName: string;
    maxContentlets: number;
    notes: string;
    code: string;
    preLoop: string;
    postLoop: string;
    containerStructures: DotContainerStructure[];
}

export interface DotContainerStructure {
    structureId?: string;
    code?: string;
    containerId?: string;
    containerInode?: string;
}

export interface DotContainerStructure {
    contentTypeVar: string;
}

// The template endpoint returns DotContainer but the page endpoint returns {container}
export interface DotContainerMap {
    [key: string]: DotContainer;
}

export interface DotPageContainer {
    [key: string]: {
        container: DotContainer;
        containerStructures?: DotContainerStructure[];
    };
}
