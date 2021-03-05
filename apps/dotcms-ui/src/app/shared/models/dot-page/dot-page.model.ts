import { DotCMSContentType } from '@dotcms/dotcms-models';

// TODO: we need to see why the endpoints are returning different "Pages" objects.
export interface DotPage {
    archived?: boolean;
    cacheTTL?: number;
    categoryId?: string;
    content?: boolean;
    contentType: DotCMSContentType;
    contentTypeId?: string;
    disabledWysiwyg?: Array<boolean>;
    fileAsset: boolean;
    folder?: string;
    friendlyName: string;
    host: string;
    htmlpage?: boolean;
    httpsRequired?: boolean;
    identifier: string;
    inode: string;
    keyValue?: boolean;
    languageId?: number;
    lastReview?: Date;
    live?: boolean;
    locked?: boolean;
    lowIndexPriority?: boolean;
    menuOrder?: number;
    metadata?: string;
    modDate?: Date;
    modUser?: string;
    name: string;
    new?: boolean;
    nextReview?: string;
    owner?: string;
    pageUrl?: string;
    parentPermissionable?: any;
    permissionId?: string;
    permissionType?: string;
    redirect?: string;
    reviewInterval?: string;
    seoDescription?: string;
    seoKeywords?: string;
    showOnMenu?: boolean;
    sortOrder?: number;
    structure?: any;
    structureInode?: string;
    systemHost: false;
    templateId?: string;
    title?: string;
    type: string;
    uri: string;
    vanityUrl?: boolean;
    versionId?: string;
    versionType: string;
    working?: boolean;
}

// Extra properties from renderHTML
export interface DotPage {
    canEdit: boolean;
    canRead: boolean;
    canLock: boolean;
    identifier: string;
    liveInode?: string;
    lockMessage?: string;
    lockedBy?: string;
    lockedByName?: string;
    lockedOn?: Date;
    pageURI: string;
    remoteRendered?: boolean;
    shortyLive: string;
    shortyWorking: string;
    workingInode: string;
    rendered?: string;
}
