export enum CATEGORY_SOURCE {
    FILE = 'FILE',
    DB = 'DB'
}

export interface DotCategory {
    active: boolean;
    categoryId?: string;
    categoryName: string;
    key: string;
    sortOrder: number;
    deleted?: boolean;
    categoryVelocityVarName: string;
    friendlyName?: string;
    path?: string;
    identifier: string;
    inode: string;
    name?: string;
    type: string;
    source?: CATEGORY_SOURCE;
    live?: boolean;
    working?: boolean;
    childrenCount: number;
    description: string;
    iDate: number;
    keywords: string;
    owner: string;
    modDate?: number;
    parentPermissionable?: {
        hostname: string;
    };
    parentList?: DotCategoryParent[];
}

export type DotCategoryParent = {
    name: string;
    key: string;
    inode: string;
};
