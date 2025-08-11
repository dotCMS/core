import { DotCMSSystemActionMappings } from './dot-workflow-action.model';
import { DotCMSWorkflow } from './dot-workflow.model';

export const DotCMSClazzes = {
    // Types
    SIMPLE_CONTENT_TYPE: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    WIDGET_CONTENT_TYPE: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
    // Layout Fields
    ROW: 'com.dotcms.contenttype.model.field.ImmutableRowField',
    COLUMN: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
    TAB_DIVIDER: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
    LINE_DIVIDER: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
    COLUMN_BREAK: 'contenttype.column.break',
    // Content Type Fields
    BINARY: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
    BLOCK_EDITOR: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    CATEGORY: 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
    CHECKBOX: 'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
    CONSTANT: 'com.dotcms.contenttype.model.field.ImmutableConstantField',
    CUSTOM_FIELD: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
    DATE: 'com.dotcms.contenttype.model.field.ImmutableDateField',
    DATE_AND_TIME: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
    FILE: 'com.dotcms.contenttype.model.field.ImmutableFileField',
    HIDDEN: 'com.dotcms.contenttype.model.field.ImmutableHiddenField',
    IMAGE: 'com.dotcms.contenttype.model.field.ImmutableImageField',
    JSON: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
    KEY_VALUE: 'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
    MULTI_SELECT: 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
    RADIO: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    RELATIONSHIP: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
    SELECT: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    HOST_FOLDER: 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
    TAG: 'com.dotcms.contenttype.model.field.ImmutableTagField',
    TEXT: 'com.dotcms.contenttype.model.field.ImmutableTextField',
    TEXTAREA: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
    TIME: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
    WYSIWYG: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField'
} as const;

export type DotCMSClazz = (typeof DotCMSClazzes)[keyof typeof DotCMSClazzes];

export interface DotCMSContentType {
    baseType: string;
    icon?: string;
    clazz: DotCMSClazz;
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
    clazz: DotCMSClazz;
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
    fieldContentTypeProperties?: string[];
    skipRelationshipCreation?: boolean;
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
