export enum FilteredFieldTypes {
    Column = 'Column',
    Row = 'Row'
}

export interface DotFieldContent {
    name: string;
    fieldTypeLabel?: string;
    variable: string;
    codeTemplate?: string;
}
