// Beware while using this type, since we have a [key: string]: any; it can be used to store any kind of data and you can write wrong properties and it will not fail

import { DotLanguage } from './dot-language.model';

// Maybe we need to refactor this to a generic type that extends from unknown when missing the generic type
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
    language?: string | DotLanguage;
    live: boolean;
    locked: boolean;
    lockedBy?: DotContentletLock;
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
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    [key: string]: any;
}

export interface DotContentletPermissions {
    READ?: string[];
    EDIT?: string[];
    PUBLISH?: string[];
    CAN_ADD_CHILDREN?: string[];
}

export interface DotContentletLock {
    firstName: string;
    lastName: string;
    userId: string;
}

/**
 * The depth of the contentlet.
 *
 * @enum {string}
 * @property {string} ZERO - Without relationships
 * @property {string} ONE - Retrieve the id of relationships
 * @property {string} TWO - Retrieve relationships
 * @property {string} THREE - Retrieve relationships with their relationships
 */
export enum DotContentletDepths {
    /**
     * Without relationships
     */
    ZERO = '0',
    /**
     * Retrieve the id of relationships
     */
    ONE = '1',
    /**
     * Retrieve relationships
     */
    TWO = '2',
    /**
     * Retrieve relationships with their relationships
     */
    THREE = '3'
}

export type DotContentletDepth = `${DotContentletDepths}`;

export interface DotContentletCanLock {
    canLock: boolean;
    id: string;
    inode: string;
    locked: boolean;
    lockedBy: string;
}
