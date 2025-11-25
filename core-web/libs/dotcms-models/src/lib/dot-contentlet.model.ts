export interface DotCMSContentlet {
    archived: boolean;
    baseType: string;
    deleted?: boolean;
    binary?: string;
    binaryContentAsset?: string;
    binaryVersion?: string;
    contentType: string;
    file?: string;
    folder: string;
    hasLiveVersion?: boolean;
    hasTitleImage: boolean;
    host: string;
    hostName: string;
    identifier: string;
    inode: string;
    image?: string;
    languageId: number;
    language?: string;
    live: boolean;
    locked: boolean;
    mimeType?: string;
    modDate: string;
    modUser: string;
    modUserName: string;
    owner: string;
    sortOrder: number;
    stInode: string;
    title: string;
    titleImage: string;
    text?: string;
    url: string;
    working: boolean;
    body?: string;
    content?: string;
    contentTypeIcon?: string;
    variant?: string;
    __icon__?: string;
    [key: string]: any;
}

export interface DotContentletPermissions {
    READ?: string[];
    EDIT?: string[];
    PUBLISH?: string[];
    CAN_ADD_CHILDREN?: string[];
}
