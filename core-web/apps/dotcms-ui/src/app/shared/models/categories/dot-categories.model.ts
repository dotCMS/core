export enum CATEGORY_SOURCE {
    FILE = 'FILE',
    DB = 'DB'
}

export interface DotCategory {
    categoryId?: string;
    categoryName: string;
    key: string;
    sortOrder: number;
    categoryVelocityVarName: string;
    deleted?: boolean;
    friendlyName?: string;
    path?: string;
    identifier: string;
    inode: string;
    name: string;
    type: string;
    source?: CATEGORY_SOURCE;
    live?: boolean;
    working?: boolean;
    parentPermissionable?: {
        hostname: string;
    };
}
