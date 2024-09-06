import { DotCMSSystemActionMappings } from './dot-workflow-action.model';
import { DotCMSWorkflow } from './dot-workflow.model';

export interface DotCMSContentType {
    baseType: string;
    icon?: string;
    clazz: string;
    defaultType: boolean;
    contentType?: string;
    description?: string;
    detailPage?: string;
    expireDateVar?: string;
    fields: DotCMSContentTypeField[];
    fixed: boolean;
    folder: string;
    host: string;
    iDate: number;
    id: string;
    layout: DotCMSContentTypeLayoutRow[];
    modDate: number;
    multilingualable: boolean;
    nEntries: number;
    name: string;
    owner?: string;
    publishDateVar?: string;
    system: boolean;
    urlMapPattern?: string;
    variable: string;
    versionable: boolean;
    workflows: DotCMSWorkflow[];
    workflow?: string[];
    systemActionMappings?: DotCMSSystemActionMappings;
    metadata?: { [key: string]: string | number | boolean };
}

export interface DotCMSContentTypeField {
    categories?: DotCMSContentTypeFieldCategories;
    clazz: string;
    contentTypeId: string;
    dataType: string;
    defaultValue?: string;
    fieldType: string;
    fieldTypeLabel: string;
    fieldVariables: DotCMSContentTypeFieldVariable[];
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
    forceIncludeInApi?: boolean;
    metadata?: { [key: string]: string | number | boolean };
}

export interface DotCMSContentTypeLayoutTab {
    title: string;
    layout: DotCMSContentTypeLayoutRow[];
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

export interface DotCMSContentTypeFieldVariable {
    clazz: string;
    fieldId: string;
    id: string;
    key: string;
    value: string;
}

export interface DotCMSAssetDialogFields {
    title: string;
    assetIdentifier: string;
    baseType: DotCMSBaseTypesContentTypes;
}

export const enum DotCMSBaseTypesContentTypes {
    WIDGET = 'WIDGET',
    CONTENT = 'CONTENT',
    PERSONA = 'PERSONA',
    FILEASSET = 'FILEASSET',
    HTMLPAGE = 'HTMLPAGE',
    VANITY_URL = 'VANITY_URL',
    DOTASSET = 'DOTASSET',
    FORM = 'FORM',
    KEY_VALUE = 'KEY_VALUE'
}

export type DotCopyContentTypeDialogFormFields = {
    name: string;
    variable: string;
    folder: string;
    host: string;
    icon: string;
};

/** @private */
interface Relationships {
    cardinality: number;
    isParentField: boolean;
    velocityVar: string;
}
