export interface DotCMSContentTypeField {
    clazz?: string;
    contentTypeId?: string;
    defaultValue?: string;
    fieldType?: string;
    fixed?: boolean;
    id?: string;
    indexed?: boolean;
    listed?: boolean;
    name?: string;
    readOnly?: boolean;
    required?: boolean;
    searchable?: boolean;
    sortOrder?: number;
    dataType?: string;
    hint?: string;
    fieldTypeLabel?: string;
    values?: string;
    variable?: string;
}

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
