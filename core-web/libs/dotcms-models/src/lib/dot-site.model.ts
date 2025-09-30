import { DotCMSContentType } from './dot-content-types.model';

/**
 * @deprecated Use SiteEntity instead
 */
export interface DotSite {
    archived?: string;
    categoryId?: string;
    contentType: DotCMSContentType;
    contentTypeId?: string;
    host?: string;
    hostname: string;
    identifier: string;
    inode?: string;
    keyValue?: boolean;
    locked?: boolean;
    modDate?: Date;
    name: string;
    owner?: string;
    permissionId: string;
    permissionType?: string;
    sortOrder?: number;
    tagStorage?: string;
    title?: string;
    type: string;
    vanityUrl?: boolean;
    versionId?: string;
    versionType?: string;
}

/**
 * Interface representing a complete site entity as returned by the DotCMS API.
 * This reflects the actual structure of site data from endpoints like /api/v1/site/currentSite.
 */
export interface SiteEntity {
    aliases: string;
    archived: boolean;
    categoryId: string;
    contentTypeId: string;
    default: boolean;
    dotAsset: boolean;
    fileAsset: boolean;
    folder: string;
    form: boolean;
    host: string;
    hostThumbnail: unknown;
    hostname: string;
    htmlpage: boolean;
    identifier: string;
    indexPolicyDependencies: string;
    inode: string;
    keyValue: boolean;
    languageId: number;
    languageVariable: boolean;
    live: boolean;
    locked: boolean;
    lowIndexPriority: boolean;
    modDate: number;
    modUser: string;
    name: string;
    new: boolean;
    owner: string;
    parent: boolean;
    permissionId: string;
    permissionType: string;
    persona: boolean;
    sortOrder: number;
    structureInode: string;
    systemHost: boolean;
    tagStorage: string;
    title: string;
    titleImage: unknown;
    type: string;
    vanityUrl: boolean;
    variantId: string;
    versionId: string;
    working: boolean;
    googleMap?: string;
}
