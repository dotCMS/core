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
    TEXT = 'Text',
    TEXTAREA = 'Textarea',
    SELECT = 'Select',
    RADIO = 'Radio',
    DATE = 'Date',
    DATE_AND_TIME = 'Date-and-Time',
    TIME = 'Time',
    TAG = 'Tag',
    CHECKBOX = 'Checkbox',
    MULTI_SELECT = 'Multi-Select',
    BLOCK_EDITOR = 'Story-Block',
    BINARY = 'Binary',
    CUSTOM_FIELD = 'Custom-Field',
    JSON = 'JSON-Field',
    KEY_VALUE = 'Key-Value',
    WYSIWYG = 'WYSIWYG'
}
