// You can add here the field types you want to filter
export enum FilteredFieldTypes {
    Column = 'Column',
    Row = 'Row'
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
    DEFAULT = 'default'
}
