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
}
