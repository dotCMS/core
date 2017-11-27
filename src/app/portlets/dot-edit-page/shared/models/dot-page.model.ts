import { ContentType } from './../../../content-types/shared/content-type.model';

export interface DotPage {
    archived?: boolean;
    categoryId?: string;
    content?: boolean;
    contentType: ContentType;
    contentTypeId?: string;
    folder?: string;
    friendlyName: string;
    host: string;
    htmlpage?: boolean;
    httpsRequired?: boolean;
    identifier: string;
    keyValue?: boolean;
    languageId?: number;
    locked?: boolean;
    modDate?: Date;
    name: string;
    owner?: string;
    pageUrl?: string;
    permissionId?: string;
    permissionType?: string;
    seoDescription?: string;
    seoKeywords?: string;
    showOnMenu?: boolean;
    sortOrder?: number;
    templateId?: string;
    title?: string;
    type: string;
    uri: string;
    vanityUrl?: boolean;
    versionId?: string;
    versionType: string;
}
