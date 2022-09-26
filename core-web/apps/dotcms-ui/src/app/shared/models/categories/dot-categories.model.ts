export enum CATEGORY_SOURCE {
    FILE = 'FILE',
    DB = 'DB'
}

export interface DotCategory {
    categoryId?: string;
    deleted?: boolean;
    friendlyName?: string;
    path?: string;
    identifier: string;
    name: string;
    type: string;
    source: CATEGORY_SOURCE;
    live: boolean;
    working: boolean;
    parentPermissionable: {
        hostname: string;
    };
}
