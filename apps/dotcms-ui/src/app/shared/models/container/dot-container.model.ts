export enum CONTAINER_SOURCE {
    FILE = 'FILE',
    DB = 'DB'
}

export interface DotContainer {
    categoryId?: string;
    deleted?: boolean;
    friendlyName?: string;
    path?: string;
    identifier: string;
    name: string;
    type: string;
    source: CONTAINER_SOURCE;
    parentPermissionable: {
        hostname: string;
    };
}

export interface DotContainerStructure {
    contentTypeVar: string;
}

// The template endpoint returns DotContainer but the page endpoint returns {container}
export interface DotContainerMap {
    [key: string]: DotContainer;
}
