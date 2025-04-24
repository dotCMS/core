/**
 * Models for DotCMS Content Types system
 * This file contains interfaces and types used to define Content Types and Fields
 */

import { DotCMSSystemActionMappings } from './dot-workflow-action.model';
import { DotCMSWorkflow } from './dot-workflow.model';

/**
 * Full Java class names for DotCMS content type fields
 * Used to identify the specific implementation class for each field type
 */
export enum DotCMSClazzes {
    // Layout Fields
    ROW = 'com.dotcms.contenttype.model.field.ImmutableRowField',
    COLUMN = 'com.dotcms.contenttype.model.field.ImmutableColumnField',
    TAB_DIVIDER = 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
    LINE_DIVIDER = 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
    COLUMN_BREAK = 'contenttype.column.break',
    // Content Type Fields
    BINARY = 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
    BLOCK_EDITOR = 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    CATEGORY = 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
    CHECKBOX = 'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
    CONSTANT = 'com.dotcms.contenttype.model.field.ImmutableConstantField',
    CUSTOM_FIELD = 'com.dotcms.contenttype.model.field.ImmutableCustomField',
    DATE = 'com.dotcms.contenttype.model.field.ImmutableDateField',
    DATE_AND_TIME = 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
    FILE = 'com.dotcms.contenttype.model.field.ImmutableFileField',
    HIDDEN = 'com.dotcms.contenttype.model.field.ImmutableHiddenField',
    IMAGE = 'com.dotcms.contenttype.model.field.ImmutableImageField',
    JSON = 'com.dotcms.contenttype.model.field.ImmutableJSONField',
    KEY_VALUE = 'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
    MULTI_SELECT = 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
    RADIO = 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    RELATIONSHIP = 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
    SELECT = 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    HOST_FOLDER = 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
    TAG = 'com.dotcms.contenttype.model.field.ImmutableTagField',
    TEXT = 'com.dotcms.contenttype.model.field.ImmutableTextField',
    TEXTAREA = 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
    TIME = 'com.dotcms.contenttype.model.field.ImmutableTimeField',
    WYSIWYG = 'com.dotcms.contenttype.model.field.ImmutableWysiwygField'
}

/**
 * Type for DotCMS class strings
 * String literal type derived from DotCMSClazzes enum
 */
export type DotCMSClazz = `${DotCMSClazzes}`;

/**
 * Data types supported by DotCMS content type fields
 * Defines the underlying storage and validation type for field values
 */
export enum DotCMSDataTypes {
    SYSTEM = 'SYSTEM',
    TEXT = 'TEXT',
    LONG_TEXT = 'LONG_TEXT',
    DATE = 'DATE',
    BOOLEAN = 'BOOL',
    FLOAT = 'FLOAT',
    INTEGER = 'INTEGER'
}

/**
 * Type for DotCMS data type strings
 * String literal type derived from DotCMSDataTypes enum
 */
export type DotCMSDataType = `${DotCMSDataTypes}`;

/**
 * Field types available in DotCMS content types
 * These represent the specific type of field as displayed in the UI
 */
export enum DotCMSFieldTypes {
    // Layout Fields
    ROW = 'Row',
    COLUMN = 'Column',
    TAB_DIVIDER = 'Tab_divider',
    LINE_DIVIDER = 'Line_divider',
    COLUMN_BREAK = 'Column_break',
    // Content Type Fields
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
    WYSIWYG = 'WYSIWYG'
}

/**
 * Type for DotCMS field type strings
 * String literal type derived from DotCMSFieldTypes enum
 */
export type DotCMSFieldType = `${DotCMSFieldTypes}`;

/**
 * Additional metadata for content type fields
 * Flexible key-value structure for field-specific configuration
 */
export type DotCMSContentTypeFieldMetadata = Record<string, string | number | boolean>;

/**
 * Interface representing a DotCMS Content Type
 * Content Types define the structure and behavior of content in DotCMS
 */
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

/**
 * Base interface for all DotCMS content type fields
 * Defines common properties shared by all field types
 */
export interface DotCMSContentTypeBaseField {
    clazz: DotCMSClazz;
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
    required: boolean;
    searchable: boolean;
    sortOrder: number;
    unique: boolean;
    variable: string;
    metadata?: DotCMSContentTypeFieldMetadata;
}

// Layout Fields

/**
 * Row field for content type layout
 * Used to create a row in the layout grid
 */
export interface ContentTypeRowField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.ROW}`;
    clazz: `${DotCMSClazzes.ROW}`;
}

/**
 * Column field for content type layout
 * Used to create a column within a row in the layout grid
 */
export interface ContentTypeColumnField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.COLUMN}`;
    clazz: `${DotCMSClazzes.COLUMN}`;
}

/**
 * Line divider field for content type layout
 * Used to create a horizontal line separator in the layout
 */
export interface ContentTypeLineDividerField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.LINE_DIVIDER}`;
    clazz: `${DotCMSClazzes.LINE_DIVIDER}`;
}

/**
 * Tab divider field for content type layout
 * Used to create a tab in the layout
 */
export interface ContentTypeTabDividerField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.TAB_DIVIDER}`;
    clazz: `${DotCMSClazzes.TAB_DIVIDER}`;
}

/**
 * Column break field for content type layout
 * Used to break to a new column in the layout
 */
export interface ContentTypeColumnBreakField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.COLUMN_BREAK}`;
    clazz: `${DotCMSClazzes.COLUMN_BREAK}`;
}

// Content Type Fields

/**
 * Binary field for content types
 * Used for binary data storage
 */
export interface ContentTypeBinaryField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.BINARY}`;
    clazz: `${DotCMSClazzes.BINARY}`;
}

/**
 * Block editor field for content types
 * Used for structured content blocks/stories
 */
export interface ContentTypeBlockEditorField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.LONG_TEXT}`;
    fieldType: `${DotCMSFieldTypes.BLOCK_EDITOR}`;
    clazz: `${DotCMSClazzes.BLOCK_EDITOR}`;
}

/**
 * Category field for content types
 * Used to select categories from the category tree
 */
export interface ContentTypeCategoryField extends DotCMSContentTypeBaseField {
    categories: DotCMSContentTypeFieldCategories;
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.CATEGORY}`;
    clazz: `${DotCMSClazzes.CATEGORY}`;
    values: string;
}

/**
 * Checkbox field for content types
 * Used for boolean values or multi-selection
 */
export interface ContentTypeCheckboxField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.TEXT}`;
    fieldType: `${DotCMSFieldTypes.CHECKBOX}`;
    clazz: `${DotCMSClazzes.CHECKBOX}`;
    values: string;
}

/**
 * Constant field for content types
 * Used for fixed values that don't change
 */
export interface ContentTypeConstantField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.CONSTANT}`;
    clazz: `${DotCMSClazzes.CONSTANT}`;
    values: string;
}

/**
 * Custom field for content types
 * Used for custom-implemented fields
 */
export interface ContentTypeCustomField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.LONG_TEXT}`;
    fieldType: `${DotCMSFieldTypes.CUSTOM_FIELD}`;
    clazz: `${DotCMSClazzes.CUSTOM_FIELD}`;
    values: string;
    regexCheck?: string;
}

/**
 * Date field for content types
 * Used for date values without time
 */
export interface ContentTypeDateField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.DATE}`;
    fieldType: `${DotCMSFieldTypes.DATE}`;
    clazz: `${DotCMSClazzes.DATE}`;
}

/**
 * Date and time field for content types
 * Used for datetime values
 */
export interface ContentTypeDateTimeField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.DATE}`;
    fieldType: `${DotCMSFieldTypes.DATE_AND_TIME}`;
    clazz: `${DotCMSClazzes.DATE_AND_TIME}`;
}

/**
 * File field for content types
 * Used for file uploads
 */
export interface ContentTypeFileField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.TEXT}`;
    fieldType: `${DotCMSFieldTypes.FILE}`;
    clazz: `${DotCMSClazzes.FILE}`;
}

/**
 * Hidden field for content types
 * Used for values not shown in the UI
 */
export interface ContentTypeHiddenField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.HIDDEN}`;
    values: string;
    clazz: `${DotCMSClazzes.HIDDEN}`;
}

/**
 * Image field for content types
 * Used for image uploads
 */
export interface ContentTypeImageField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.TEXT}`;
    fieldType: `${DotCMSFieldTypes.IMAGE}`;
    clazz: `${DotCMSClazzes.IMAGE}`;
}

/**
 * JSON field for content types
 * Used for storing JSON data
 */
export interface ContentTypeJSONField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.LONG_TEXT}`;
    fieldType: `${DotCMSFieldTypes.JSON}`;
    clazz: `${DotCMSClazzes.JSON}`;
}

/**
 * Key-value field for content types
 * Used for storing key-value pairs
 */
export interface ContentTypeKeyValueField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.LONG_TEXT}`;
    fieldType: `${DotCMSFieldTypes.KEY_VALUE}`;
    clazz: `${DotCMSClazzes.KEY_VALUE}`;
}

/**
 * Multi-select field for content types
 * Used for selecting multiple options from a list
 */
export interface ContentTypeMultiSelectField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.LONG_TEXT}`;
    fieldType: `${DotCMSFieldTypes.MULTI_SELECT}`;
    values: string;
    clazz: `${DotCMSClazzes.MULTI_SELECT}`;
}

/**
 * Radio field for content types
 * Used for selecting a single option from a list
 */
export interface ContentTypeRadioField extends DotCMSContentTypeBaseField {
    dataType:
        | `${DotCMSDataTypes.TEXT}`
        | `${DotCMSDataTypes.BOOLEAN}`
        | `${DotCMSDataTypes.FLOAT}`
        | `${DotCMSDataTypes.INTEGER}`;
    fieldType: `${DotCMSFieldTypes.RADIO}`;
    values: string;
    clazz: `${DotCMSClazzes.RADIO}`;
}

/**
 * Relationship field for content types
 * Used for relating content to other content
 */
export interface ContentTypeRelationshipField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.RELATIONSHIP}`;
    relationships: {
        cardinality: number;
        isParentField: boolean;
        velocityVar: string;
    };
    skipRelationshipCreation: boolean;
    clazz: `${DotCMSClazzes.RELATIONSHIP}`;
}

/**
 * Select field for content types
 * Used for dropdown selection
 */
export interface ContentTypeSelectField extends DotCMSContentTypeBaseField {
    dataType:
        | `${DotCMSDataTypes.TEXT}`
        | `${DotCMSDataTypes.BOOLEAN}`
        | `${DotCMSDataTypes.FLOAT}`
        | `${DotCMSDataTypes.INTEGER}`;
    fieldType: `${DotCMSFieldTypes.SELECT}`;
    values: string;
    clazz: `${DotCMSClazzes.SELECT}`;
}

/**
 * Host/folder field for content types
 * Used for selecting hosts or folders
 */
export interface ContentTypeHostFolderField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.HOST_FOLDER}`;
    clazz: `${DotCMSClazzes.HOST_FOLDER}`;
}

/**
 * Tag field for content types
 * Used for adding tags to content
 */
export interface ContentTypeTagField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.SYSTEM}`;
    fieldType: `${DotCMSFieldTypes.TAG}`;
    clazz: `${DotCMSClazzes.TAG}`;
}

/**
 * Text field for content types
 * Used for single-line text input
 */
export interface ContentTypeTextField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.TEXT}` | `${DotCMSDataTypes.FLOAT}` | `${DotCMSDataTypes.INTEGER}`;
    fieldType: `${DotCMSFieldTypes.TEXT}`;
    regexCheck?: string;
    clazz: `${DotCMSClazzes.TEXT}`;
}

/**
 * Text area field for content types
 * Used for multi-line text input
 */
export interface ContentTypeTextAreaField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.LONG_TEXT}`;
    fieldType: `${DotCMSFieldTypes.TEXTAREA}`;
    regexCheck?: string;
    clazz: `${DotCMSClazzes.TEXTAREA}`;
}

/**
 * Time field for content types
 * Used for time values without date
 */
export interface ContentTypeTimeField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.DATE}`;
    fieldType: `${DotCMSFieldTypes.TIME}`;
    clazz: `${DotCMSClazzes.TIME}`;
}

/**
 * WYSIWYG field for content types
 * Used for rich text editing
 */
export interface ContentTypeWYSIWYGField extends DotCMSContentTypeBaseField {
    dataType: `${DotCMSDataTypes.LONG_TEXT}`;
    fieldType: `${DotCMSFieldTypes.WYSIWYG}`;
    regexCheck?: string;
    clazz: `${DotCMSClazzes.WYSIWYG}`;
}

/**
 * Union type for all calendar-related fields
 * Groups date, date/time, and time fields
 */
export type ContentTypeCalendarField =
    | ContentTypeDateField
    | ContentTypeDateTimeField
    | ContentTypeTimeField;

/**
 * Calendar field types available in DotCMS
 * Union type of all field types related to calendar data
 */
export type CalendarFieldTypes =
    | DotCMSFieldTypes.DATE_AND_TIME
    | DotCMSFieldTypes.DATE
    | DotCMSFieldTypes.TIME;

/**
 * Union type for all content type fields
 * Combines layout fields and content fields
 */
export type DotCMSContentTypeField =
    // Layout Fields
    | ContentTypeRowField
    | ContentTypeColumnField
    | ContentTypeTabDividerField
    | ContentTypeColumnBreakField
    | ContentTypeLineDividerField
    // Content Type Fields
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
    | ContentTypeSelectField;

/**
 * Interface representing a tab in the content type layout
 * Tabs group rows and columns into separate sections
 */
export interface DotCMSContentTypeLayoutTab {
    title: string;
    layout: DotCMSContentTypeLayoutRow[];
}

/**
 * Interface representing a row in the content type layout
 * Rows contain columns that hold fields
 */
export interface DotCMSContentTypeLayoutRow {
    columns?: DotCMSContentTypeLayoutColumn[];
    divider: DotCMSContentTypeField;
}

/**
 * Interface representing a column in the content type layout
 * Columns contain fields and have a divider
 */
export interface DotCMSContentTypeLayoutColumn {
    columnDivider: DotCMSContentTypeField;
    fields: DotCMSContentTypeField[];
}

/**
 * Interface representing categories for a category field
 * Defines the configuration for a category field
 */
export interface DotCMSContentTypeFieldCategories {
    categoryName: string;
    description?: string;
    inode: string;
    key: string;
    keywords?: string;
    sortOrder: number;
}

/**
 * Interface representing a variable for a content type field
 * Field variables allow for additional configuration per field
 */
export interface DotCMSContentTypeFieldVariable {
    clazz: string;
    fieldId: string;
    id: string;
    key: string;
    value: string;
}

/**
 * Interface representing asset dialog fields
 * Used for configuring asset selection dialogs
 */
export interface DotCMSAssetDialogFields {
    title: string;
    assetIdentifier: string;
    baseType: DotCMSBaseTypesContentTypes;
}

/**
 * Enum of base content type identifiers in DotCMS
 * Represents the fundamental categories of content
 */
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

/**
 * Type for content type copy dialog form fields
 * Used when copying a content type
 */
export type DotCopyContentTypeDialogFormFields = {
    name: string;
    variable: string;
    folder: string;
    host: string;
    icon: string;
};
