// You can add here the field types you want to filter
// Note: In the store when we get the fields for a fieldType this has priority
// so if you add a field type here and is also in the FieldTypes enum, it will be filtered and will not be shown
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

// Here goes all the fieldTypes
export enum FieldTypes {
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

// This is the function to create a getter for the fields. See getDefaultFields in dot-fields-service.ts as example
export type GetFieldsFunction = (variableContent: DotFieldContent) => DotFieldContent[];
