export enum FilteredFieldTypes {
    Column = 'Column',
    Row = 'Row'
}

export interface DotVariableList {
    variables: DotVariableContent[];
}

export interface DotVariableContent {
    name: string;
    fieldTypeLabel?: string;
    variable: string;
    codeTemplate?: string;
}
