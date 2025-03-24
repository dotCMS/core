import { DotCMSUVEAction } from './public';

/**
 * @description Custom client parameters for fetching data.
 */
export type DotCMSCustomerParams = {
    depth: string;
};

/**
 * Configuration for reordering a menu.
 */
export interface DotCMSReorderMenuConfig {
    /**
     * The starting level of the menu to be reordered.
     */
    startLevel: number;

    /**
     * The depth of the menu levels to be reordered.
     */
    depth: number;
}

declare global {
    interface Window {
        dotCMSUVE: DotCMSUVE;
    }
}

/**
 * Post message props
 *
 * @export
 * @template T
 * @interface DotCMSUVEMessage
 */
export type DotCMSUVEMessage<T> = {
    action: DotCMSUVEAction;
    payload?: T;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type DotCMSUVEFunction = (...args: any[]) => void;

export interface DotCMSUVE {
    editContentlet: DotCMSUVEFunction;
    initInlineEditing: DotCMSUVEFunction;
    reorderMenu: DotCMSUVEFunction;
    lastScrollYPosition: number;
}

/**
 * Main fields of a Contentlet (Inherited from the Content Type).
 */
export interface ContentTypeMainFields {
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