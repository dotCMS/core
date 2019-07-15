export interface DotCMSContentType {
    multilingualable: boolean;
    modDate: number;
    versionable: boolean;
    workflows: Workflow[];
    defaultType: boolean;
    baseType: string;
    layout: DotCMSContentTypeLayoutRow[];
    system: boolean;
    folder: string;
    name: string;
    variable: string;
    host: string;
    fixed: boolean;
    id: string;
    fields: DotCMSContentTypeField[];
    clazz: string;
    iDate: number;
}
export interface DotCMSContentTypeField {
    categories?: DotCMSContentTypeFieldCategories;
    clazz: string;
    contentTypeId: string;
    dataType: string;
    defaultValue?: string;
    fieldType: string;
    fieldTypeLabel: string;
    fieldVariables: FieldVariable[];
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
    relationships?: Relationships;
    required: boolean;
    searchable: boolean;
    sortOrder: number;
    unique: boolean;
    values?: string;
    variable: string;
}

export interface DotCMSContentTypeLayoutRow {
    columns?: DotCMSContentTypeLayoutColumn[];
    divider: DotCMSContentTypeField;
}

export interface DotCMSContentTypeLayoutColumn {
    columnDivider: DotCMSContentTypeField;
    fields: DotCMSContentTypeField[];
}

export interface DotCMSContentTypeFieldCategories {
    categoryName: string;
    description?: string;
    inode: string;
    key: string;
    keywords?: string;
    sortOrder: number;
}

/** @private */
interface Relationships {
    cardinality: number;
    velocityVar: string;
}

/** @private */
interface FieldVariable {
    clazz: string;
    fieldId: string;
    id: string;
    key: string;
    value: string;
}

/** @private */
interface Workflow {
    id: string;
    creationDate: number;
    name: string;
    description: string;
    archived: boolean;
    mandatory: boolean;
    defaultScheme: boolean;
    modDate: number;
    entryActionId?: string;
    system: boolean;
}
