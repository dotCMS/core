export interface DotCMSContentTypeField {
    clazz: string;
    contentTypeId: string;
    dataType: string;
    defaultValue?: string;
    fieldContentTypeProperties?: any[];
    fieldType: string;
    fieldTypeLabel: string;
    fieldVariables: any[];
    fixed: boolean;
    hint?: string;
    iDate: number;
    id: string;
    indexed: boolean;
    listed: boolean;
    modDate: number;
    name: string;
    readOnly: boolean;
    regexCheck?: string;
    required: boolean;
    searchable: boolean;
    sortOrder: number;
    unique: boolean;
    values?: string;
    variable?: string;
}
