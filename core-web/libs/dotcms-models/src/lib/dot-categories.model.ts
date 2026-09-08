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
    /** Null for categories the API returns without one; several endpoints do. */
    identifier: string | null;
    inode: string;
    name?: string;
    type: string;
    source?: CATEGORY_SOURCE;
    live?: boolean;
    working?: boolean;
    childrenCount: number;
    /** Optional metadata; the API returns null when unset. */
    description: string | null;
    iDate: number;
    /** Optional metadata; the API returns null when unset — consumers already use `|| ''`. */
    keywords: string | null;
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
