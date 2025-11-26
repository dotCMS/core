import { DotCMSSystemActionMappings } from './dot-workflow-action.model';
import { DotCMSWorkflow } from './dot-workflow.model';

/**
 * Constants defining the Java class names for different DotCMS content type classes
 * Used for identifying the specific implementation class of content types and fields
 */
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

/**
 * Union type representing all possible DotCMS class names
 * Derived from the DotCMSClazzes constant object
 */
export type DotCMSClazz = (typeof DotCMSClazzes)[keyof typeof DotCMSClazzes];

/**
 * Constants defining the data types supported by DotCMS content type fields
 * Maps to the underlying data storage types in the system
 */
export const DotCMSDataTypes = {
    SYSTEM: 'SYSTEM',
    TEXT: 'TEXT',
    LONG_TEXT: 'LONG_TEXT',
    DATE: 'DATE',
    BOOLEAN: 'BOOL',
    FLOAT: 'FLOAT',
    INTEGER: 'INTEGER'
} as const;

/**
 * Union type representing all possible DotCMS data types
 * Derived from the DotCMSDataTypes constant object
 */
export type DotCMSDataType = (typeof DotCMSDataTypes)[keyof typeof DotCMSDataTypes];

/**
 * Constants defining the field types available in DotCMS content types
 * Includes both layout fields and content type fields
 */
export const DotCMSFieldTypes = {
    // Layout Fields
    ROW: 'Row',
    COLUMN: 'Column',
    TAB_DIVIDER: 'Tab_divider',
    LINE_DIVIDER: 'Line_divider',
    COLUMN_BREAK: 'Column_break',
    // Content Type Fields
    BINARY: 'Binary',
    BLOCK_EDITOR: 'Story-Block',
    CATEGORY: 'Category',
    CHECKBOX: 'Checkbox',
    CONSTANT: 'Constant-Field',
    CUSTOM_FIELD: 'Custom-Field',
    DATE: 'Date',
    DATE_AND_TIME: 'Date-and-Time',
    FILE: 'File',
    HIDDEN: 'Hidden-Field',
    IMAGE: 'Image',
    JSON: 'JSON-Field',
    KEY_VALUE: 'Key-Value',
    MULTI_SELECT: 'Multi-Select',
    RADIO: 'Radio',
    RELATIONSHIP: 'Relationship',
    SELECT: 'Select',
    HOST_FOLDER: 'Host-Folder',
    TAG: 'Tag',
    TEXT: 'Text',
    TEXTAREA: 'Textarea',
    TIME: 'Time',
    WYSIWYG: 'WYSIWYG'
} as const;

/**
 * Union type representing all possible DotCMS field types
 * Derived from the DotCMSFieldTypes constant object
 */
export type DotCMSFieldType = (typeof DotCMSFieldTypes)[keyof typeof DotCMSFieldTypes];

/**
 * Additional metadata for content type fields
 * Flexible key-value structure for field-specific configuration
 */
export type DotCMSContentTypeFieldMetadata = Record<string, string | number | boolean>;

/**
 * Main interface representing a DotCMS content type
 * Defines the structure and properties of content types including fields, layout, and metadata
 */
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
    velocityVarName?: string;
    metadata?: DotCMSContentTypeFieldMetadata;
    values?: string;
}

// Layout Fields

/**
 * Row field for content type layout
 * Used to create a row in the layout grid
 */
export interface ContentTypeRowField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.ROW;
    clazz: typeof DotCMSClazzes.ROW;
}

/**
 * Column field for content type layout
 * Used to create a column within a row in the layout grid
 */
export interface ContentTypeColumnField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.COLUMN;
    clazz: typeof DotCMSClazzes.COLUMN;
}

/**
 * Line divider field for content type layout
 * Used to create a horizontal line separator in the layout
 */
export interface ContentTypeLineDividerField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.LINE_DIVIDER;
    clazz: typeof DotCMSClazzes.LINE_DIVIDER;
}

/**
 * Tab divider field for content type layout
 * Used to create a tab in the layout
 */
export interface ContentTypeTabDividerField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.TAB_DIVIDER;
    clazz: typeof DotCMSClazzes.TAB_DIVIDER;
}

/**
 * Column break field for content type layout
 * Used to break to a new column in the layout
 */
export interface ContentTypeColumnBreakField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.COLUMN_BREAK;
    clazz: typeof DotCMSClazzes.COLUMN_BREAK;
}

// Content Type Fields

/**
 * Binary field for content types
 * Used for binary data storage
 */
export interface ContentTypeBinaryField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.BINARY;
    clazz: typeof DotCMSClazzes.BINARY;
}

/**
 * Block editor field for content types
 * Used for structured content blocks/stories
 */
export interface ContentTypeBlockEditorField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.LONG_TEXT;
    fieldType: typeof DotCMSFieldTypes.BLOCK_EDITOR;
    clazz: typeof DotCMSClazzes.BLOCK_EDITOR;
}

/**
 * Category field for content types
 * Used to select categories from the category tree
 */
export interface ContentTypeCategoryField extends DotCMSContentTypeBaseField {
    categories: DotCMSContentTypeFieldCategories;
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.CATEGORY;
    clazz: typeof DotCMSClazzes.CATEGORY;
    values: string;
}

/**
 * Checkbox field for content types
 * Used for boolean values or multi-selection
 */
export interface ContentTypeCheckboxField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.TEXT;
    fieldType: typeof DotCMSFieldTypes.CHECKBOX;
    clazz: typeof DotCMSClazzes.CHECKBOX;
    values: string;
}

/**
 * Constant field for content types
 * Used for fixed values that don't change
 */
export interface ContentTypeConstantField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.CONSTANT;
    clazz: typeof DotCMSClazzes.CONSTANT;
    values: string;
}

/**
 * Custom field for content types
 * Used for custom-implemented fields
 */
export interface ContentTypeCustomField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.LONG_TEXT;
    fieldType: typeof DotCMSFieldTypes.CUSTOM_FIELD;
    clazz: typeof DotCMSClazzes.CUSTOM_FIELD;
    values: string;
    regexCheck?: string;
}

/**
 * Date field for content types
 * Used for date values without time
 */
export interface ContentTypeDateField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.DATE;
    fieldType: typeof DotCMSFieldTypes.DATE;
    clazz: typeof DotCMSClazzes.DATE;
}

/**
 * Date and time field for content types
 * Used for datetime values
 */
export interface ContentTypeDateTimeField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.DATE;
    fieldType: typeof DotCMSFieldTypes.DATE_AND_TIME;
    clazz: typeof DotCMSClazzes.DATE_AND_TIME;
}

/**
 * File field for content types
 * Used for file uploads
 */
export interface ContentTypeFileField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.TEXT;
    fieldType: typeof DotCMSFieldTypes.FILE;
    clazz: typeof DotCMSClazzes.FILE;
}

/**
 * Hidden field for content types
 * Used for values not shown in the UI
 */
export interface ContentTypeHiddenField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.HIDDEN;
    values: string;
    clazz: typeof DotCMSClazzes.HIDDEN;
}

/**
 * Image field for content types
 * Used for image uploads
 */
export interface ContentTypeImageField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.TEXT;
    fieldType: typeof DotCMSFieldTypes.IMAGE;
    clazz: typeof DotCMSClazzes.IMAGE;
}

/**
 * JSON field for content types
 * Used for storing JSON data
 */
export interface ContentTypeJSONField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.LONG_TEXT;
    fieldType: typeof DotCMSFieldTypes.JSON;
    clazz: typeof DotCMSClazzes.JSON;
}

/**
 * Key-value field for content types
 * Used for storing key-value pairs
 */
export interface ContentTypeKeyValueField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.LONG_TEXT;
    fieldType: typeof DotCMSFieldTypes.KEY_VALUE;
    clazz: typeof DotCMSClazzes.KEY_VALUE;
}

/**
 * Multi-select field for content types
 * Used for selecting multiple options from a list
 */
export interface ContentTypeMultiSelectField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.LONG_TEXT;
    fieldType: typeof DotCMSFieldTypes.MULTI_SELECT;
    values: string;
    clazz: typeof DotCMSClazzes.MULTI_SELECT;
}

/**
 * Radio field for content types
 * Used for selecting a single option from a list
 */
export interface ContentTypeRadioField extends DotCMSContentTypeBaseField {
    dataType:
        | typeof DotCMSDataTypes.TEXT
        | typeof DotCMSDataTypes.BOOLEAN
        | typeof DotCMSDataTypes.FLOAT
        | typeof DotCMSDataTypes.INTEGER;
    fieldType: typeof DotCMSFieldTypes.RADIO;
    values: string;
    clazz: typeof DotCMSClazzes.RADIO;
}

/**
 * Relationship field for content types
 * Used for relating content to other content
 */
export interface ContentTypeRelationshipField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.RELATIONSHIP;
    relationships: {
        cardinality: number;
        isParentField: boolean;
        velocityVar: string;
    };
    skipRelationshipCreation: boolean;
    clazz: typeof DotCMSClazzes.RELATIONSHIP;
}

/**
 * Select field for content types
 * Used for dropdown selection
 */
export interface ContentTypeSelectField extends DotCMSContentTypeBaseField {
    dataType:
        | typeof DotCMSDataTypes.TEXT
        | typeof DotCMSDataTypes.BOOLEAN
        | typeof DotCMSDataTypes.FLOAT
        | typeof DotCMSDataTypes.INTEGER;
    fieldType: typeof DotCMSFieldTypes.SELECT;
    values: string;
    clazz: typeof DotCMSClazzes.SELECT;
}

/**
 * Host/folder field for content types
 * Used for selecting hosts or folders
 */
export interface ContentTypeHostFolderField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.HOST_FOLDER;
    clazz: typeof DotCMSClazzes.HOST_FOLDER;
}

/**
 * Tag field for content types
 * Used for adding tags to content
 */
export interface ContentTypeTagField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.SYSTEM;
    fieldType: typeof DotCMSFieldTypes.TAG;
    clazz: typeof DotCMSClazzes.TAG;
}

/**
 * Text field for content types
 * Used for single-line text input
 */
export interface ContentTypeTextField extends DotCMSContentTypeBaseField {
    dataType:
        | typeof DotCMSDataTypes.TEXT
        | typeof DotCMSDataTypes.FLOAT
        | typeof DotCMSDataTypes.INTEGER;
    fieldType: typeof DotCMSFieldTypes.TEXT;
    regexCheck?: string;
    clazz: typeof DotCMSClazzes.TEXT;
}

/**
 * Text area field for content types
 * Used for multi-line text input
 */
export interface ContentTypeTextAreaField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.LONG_TEXT;
    fieldType: typeof DotCMSFieldTypes.TEXTAREA;
    regexCheck?: string;
    clazz: typeof DotCMSClazzes.TEXTAREA;
}

/**
 * Time field for content types
 * Used for time values without date
 */
export interface ContentTypeTimeField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.DATE;
    fieldType: typeof DotCMSFieldTypes.TIME;
    clazz: typeof DotCMSClazzes.TIME;
}

/**
 * WYSIWYG field for content types
 * Used for rich text editing
 */
export interface ContentTypeWYSIWYGField extends DotCMSContentTypeBaseField {
    dataType: typeof DotCMSDataTypes.LONG_TEXT;
    fieldType: typeof DotCMSFieldTypes.WYSIWYG;
    regexCheck?: string;
    clazz: typeof DotCMSClazzes.WYSIWYG;
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
    | typeof DotCMSFieldTypes.DATE_AND_TIME
    | typeof DotCMSFieldTypes.DATE
    | typeof DotCMSFieldTypes.TIME;

/* Legacy Fields */

/**
 * Legacy interface for DotCMS content type fields
 */
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

/**
 * @private
 * Internal interface for relationship configuration
 * Used internally by the relationship field implementation
 */
interface Relationships {
    cardinality: number;
    isParentField: boolean;
    velocityVar: string;
}

/**
 * Interface for pagination parameters when retrieving content types
 * Used for filtering and pagination of content types
 */
export interface DotContentTypePaginationOptions {
    filter?: string;
    page?: number;
    type?: string;
    ensure?: string;
}
