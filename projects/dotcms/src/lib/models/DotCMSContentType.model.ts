import { DotCMSContentTypeField } from './DotCMSContentTypeField.model';

export interface DotCMSContentType {
    baseType?: string;
    clazz: string;
    defaultType: boolean;
    description?: string;
    detailPage?: string;
    expireDateVar?: string;
    fields?: Array<DotCMSContentTypeField>;
    fixed: boolean;
    folder: string;
    host: string;
    iDate?: Date;
    id?: string;
    modDate?: Date;
    name: string;
    owner: string;
    publishDateVar?: string;
    system: boolean;
    urlMapPattern?: string;
    variable?: string;
    workflow?: string;
}
