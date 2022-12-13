import { DotCMSContentType } from './dot-content-types.model';

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
