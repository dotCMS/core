/**
 * Represents the selectable data types (Dropdown, Radio button) for a DotCMS content field.
 */
export enum DotEditContentFieldSingleSelectableDataType {
    BOOL = 'BOOL',
    INTEGER = 'INTEGER',
    FLOAT = 'FLOAT'
}

// Map to match the field type to component selector
export enum FIELD_TYPES {
    BINARY = 'Binary',
    FILE = 'File',
    IMAGE = 'Image',
    BLOCK_EDITOR = 'Story-Block',
    CATEGORY = 'Category',
    CHECKBOX = 'Checkbox',
    CONSTANT = 'Constant-Field',
    CUSTOM_FIELD = 'Custom-Field',
    DATE = 'Date',
    DATE_AND_TIME = 'Date-and-Time',
    HIDDEN = 'Hidden-Field',
    HOST_FOLDER = 'Host-Folder',
    JSON = 'JSON-Field',
    KEY_VALUE = 'Key-Value',
    MULTI_SELECT = 'Multi-Select',
    RADIO = 'Radio',
    SELECT = 'Select',
    TAG = 'Tag',
    TEXT = 'Text',
    TEXTAREA = 'Textarea',
    TIME = 'Time',
    WYSIWYG = 'WYSIWYG',
    RELATIONSHIP = 'Relationship',
    LINE_DIVIDER = 'Line_divider'
}

export const FIELD_TYPES_CONST = {
    BINARY: 'Binary',
    FILE: 'File',
    IMAGE: 'Image',
    BLOCK_EDITOR: 'Story-Block',
    CATEGORY: 'Category',
    CHECKBOX: 'Checkbox',
    CONSTANT: 'Constant-Field',
    CUSTOM_FIELD: 'Custom-Field',
    DATE: 'Date',
    DATE_AND_TIME: 'Date-and-Time',
    HIDDEN: 'Hidden-Field',
    HOST_FOLDER: 'Host-Folder',
    JSON: 'JSON-Field',
    KEY_VALUE: 'Key-Value',
    MULTI_SELECT: 'Multi-Select',
    RADIO: 'Radio',
    SELECT: 'Select',
    TAG: 'Tag',
    TEXT: 'Text',
    TEXTAREA: 'Textarea',
    TIME: 'Time',
    WYSIWYG: 'WYSIWYG',
    RELATIONSHIP: 'Relationship',
    LINE_DIVIDER: 'Line_divider'
} as const;
