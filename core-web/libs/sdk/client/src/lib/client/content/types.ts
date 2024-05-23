export type Order = 'asc' | 'desc';

export type SortBy = {
    field: string;
    order: Order;
};

export type SortByArray = Array<SortBy>;

export interface ContentletMainFields {
    hostName: string;
    modDate: string;
    publishDate: string;
    title: string;
    baseType: string;
    inode: string;
    archived: boolean;
    ownerName: string;
    host: string;
    working: boolean;
    locked: boolean;
    stInode: string;
    contentType: string;
    live: boolean;
    owner: string;
    identifier: string;
    publishUserName: string;
    publishUser: string;
    languageId: number;
    creationDate: string;
    url: string;
    titleImage: string;
    modUserName: string;
    hasLiveVersion: boolean;
    folder: string;
    hasTitleImage: boolean;
    sortOrder: number;
    modUser: string;
    __icon__: string;
    contentTypeIcon: string;
    variant: string;
}

export interface Contentlet extends ContentletMainFields {
    [key: string]: unknown;
}

export interface GetCollectionResponse {
    contentlets: Contentlet[];
    page: number;
    size: number;
    total: number;
    sortedBy: SortByArray;
}
