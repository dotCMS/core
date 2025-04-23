import { DotCMSSystemActionMappings } from './dot-workflow-action.model';
import { DotCMSWorkflow } from './dot-workflow.model';

export enum DotCMSDataTypes {
    SYSTEM = 'SYSTEM',
    TEXT = 'TEXT',
    LONG_TEXT = 'LONG_TEXT',
    DATE = 'DATE',
    BOOLEAN = 'BOOL',
    FLOAT = 'FLOAT',
    INTEGER = 'INTEGER',
}

export type DotCMSDataType = `${DotCMSDataTypes}`;

export enum DotCMSFieldTypes {
    ROW = 'Row',
    COLUMN = 'Column',
    BINARY = 'Binary',
    BLOCK_EDITOR = 'Story-Block',
    CATEGORY = 'Category',
    CHECKBOX = 'Checkbox',
    CONSTANT = 'Constant-Field',
    CUSTOM_FIELD = 'Custom-Field',
    DATE = 'Date',
    DATE_AND_TIME = 'Date-and-Time',
    FILE = 'File',
    HIDDEN = 'Hidden-Field',
    IMAGE = 'Image',
    JSON = 'JSON-Field',
    KEY_VALUE = 'Key-Value',
    MULTI_SELECT = 'Multi-Select',
    RADIO = 'Radio',
    RELATIONSHIP = 'Relationship',
    SELECT = 'Select',
    HOST_FOLDER = 'Host-Folder',
    TAG = 'Tag',
    TEXT = 'Text',
    TEXTAREA = 'Textarea',
    TIME = 'Time',
    WYSIWYG = 'WYSIWYG',
    LINE_DIVIDER = 'Line_divider'
}

export type DotCMSFieldType = `${DotCMSFieldTypes}`;

export type DotCMSContentTypeFieldMetadata = Record<string, string | number | boolean>;

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
    metadata?: DotCMSContentTypeFieldMetadata;
}

export interface DotCMSContentTypeBaseField {
    clazz: string;
    contentTypeId: string;
    dataType: DotCMSDataType;
    defaultValue?: string;
    fieldContentTypeProperties?: string[];
    fieldVariables: DotCMSContentTypeFieldVariable[];
    fieldType: DotCMSFieldType;
    fieldTypeLabel: string;
    fieldTypeOptions?: unknown[];
    fixed: boolean;
    forceIncludeInApi: boolean;
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
    variable: string;
    metadata?: DotCMSContentTypeFieldMetadata;
}

export interface ContentTypeRowField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.ROW;
}

export interface ContentTypeColumnField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.COLUMN;
}

export interface ContentTypeBinaryField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.BINARY;
}

export interface ContentTypeBlockEditorField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.BLOCK_EDITOR;
}

export interface ContentTypeCategoryField extends DotCMSContentTypeBaseField {
    categories: DotCMSContentTypeFieldCategories;
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.CATEGORY;
    values: string;
}

export interface ContentTypeCheckboxField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.TEXT;
    fieldType: DotCMSFieldTypes.CHECKBOX;
    values: string;
}

export interface ContentTypeConstantField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.CONSTANT;
    values: string;
}


export interface ContentTypeCustomField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.LONG_TEXT;
    fieldType: DotCMSFieldTypes.CUSTOM_FIELD;
    values: string;
}

export interface ContentTypeDateField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.DATE;
    fieldType: DotCMSFieldTypes.DATE;
}

export interface ContentTypeDateTimeField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.DATE;
    fieldType: DotCMSFieldTypes.DATE_AND_TIME;
}

export interface ContentTypeFileField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.TEXT;
    fieldType: DotCMSFieldTypes.FILE;
}

export interface ContentTypeHiddenField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.HIDDEN;
    values: string;
}

export interface ContentTypeImageField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.IMAGE;
}

export interface ContentTypeJSONField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.LONG_TEXT;
    fieldType: DotCMSFieldTypes.JSON;
}

export interface ContentTypeKeyValueField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.LONG_TEXT;
    fieldType: DotCMSFieldTypes.KEY_VALUE;
}

export interface ContentTypeMultiSelectField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.LONG_TEXT;
    fieldType: DotCMSFieldTypes.MULTI_SELECT;
    values: string;
}

export interface ContentTypeRadioField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.TEXT | DotCMSDataTypes.BOOLEAN | DotCMSDataTypes.FLOAT;
    fieldType: DotCMSFieldTypes.RADIO;
    values: string;
}

export interface ContentTypeRelationshipField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.RELATIONSHIP;
    relationships: {
        cardinality: number;
        isParentField: boolean;
        velocityVar: string;
    };
    skipRelationshipCreation: boolean;
}

export interface ContentTypeHostFolderField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.HOST_FOLDER;
}

export interface ContentTypeTagField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.TAG;
}

export interface ContentTypeTextField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.TEXT;
    fieldType: DotCMSFieldTypes.TEXT;
}

export interface ContentTypeTextAreaField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.LONG_TEXT;
    fieldType: DotCMSFieldTypes.TEXTAREA;
}

export interface ContentTypeTimeField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.DATE;
    fieldType: DotCMSFieldTypes.TIME;
}

export interface ContentTypeWYSIWYGField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.LONG_TEXT;
    fieldType: DotCMSFieldTypes.WYSIWYG;
}

export interface ContentTypeLineDividerField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.SYSTEM;
    fieldType: DotCMSFieldTypes.LINE_DIVIDER;
}

export interface ContentTypeSelectField extends DotCMSContentTypeBaseField {
    dataType: DotCMSDataTypes.TEXT | DotCMSDataTypes.BOOLEAN | DotCMSDataTypes.FLOAT | DotCMSDataTypes.INTEGER;
    fieldType: DotCMSFieldTypes.SELECT;
    values: string;
}

export type ContentTypeCalendarField = ContentTypeDateField | ContentTypeDateTimeField | ContentTypeTimeField;

export type CalendarFieldTypes = DotCMSFieldTypes.DATE_AND_TIME | DotCMSFieldTypes.DATE | DotCMSFieldTypes.TIME;

export type DotCMSContentTypeField =
    | ContentTypeRowField
    | ContentTypeColumnField
    | ContentTypeBinaryField
    | ContentTypeBlockEditorField
    | ContentTypeCategoryField
    | ContentTypeCheckboxField
    | ContentTypeConstantField
    | ContentTypeCustomField
    | ContentTypeDateField
    | ContentTypeDateTimeField
    | ContentTypeFileField
    | ContentTypeHiddenField
    | ContentTypeImageField
    | ContentTypeJSONField
    | ContentTypeKeyValueField
    | ContentTypeMultiSelectField
    | ContentTypeRadioField
    | ContentTypeRelationshipField
    | ContentTypeHostFolderField
    | ContentTypeTagField
    | ContentTypeTextField
    | ContentTypeTextAreaField
    | ContentTypeTimeField
    | ContentTypeWYSIWYGField
    | ContentTypeLineDividerField
    | ContentTypeSelectField;

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
