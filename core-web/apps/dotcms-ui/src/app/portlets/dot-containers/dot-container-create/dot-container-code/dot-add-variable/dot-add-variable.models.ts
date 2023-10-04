// You can add here the field types you want to filter
export enum FilteredFieldTypes {
    Column = 'Column',
    Row = 'Row',
    Category = 'Category',
    ConstantField = 'Constant-Field',
    HiddenField = 'Hidden-Field',
    JsonField = 'Json-Field',
    KeyValue = 'Key-Value',
    PermissionsField = 'Permissions-Field',
    Relationship = 'Relationship'
}

// This is the model we use in the HTML to render the field
export interface DotFieldContent {
    name: string;
    fieldTypeLabel?: string;
    variable: string;
    codeTemplate?: string;
}

// Here goes all the fieldTypes that have extra fields
export enum FieldTypeWithExtraFields {
    IMAGE = 'Image',
    HOST = 'Host-Folder',
    FILE = 'File',
    DEFAULT = 'default',
    BLOCK_EDITOR = 'Story-Block',
    BINARY = 'Binary',
    MULTISELECT = 'Multi-Select',
    SELECT = 'Select',
    CHECKBOX = 'Checkbox',
    RADIO = 'Radio',
    DATE = 'Date',
    DATE_AND_TIME = 'Date-and-Time',
    TIME = 'Time'
}
