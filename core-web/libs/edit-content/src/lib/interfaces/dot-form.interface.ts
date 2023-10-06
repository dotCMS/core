export interface DotForm {
    row: DotRow;
}

export interface DotRow {
    id: string;
    columns: Column[];
}

interface Column {
    id: string;
    fields: DotField[];
}

export interface DotField {
    id: string;
    type: string;
    label: string;
    required: boolean;
    regexCheck?: string;
    hint?: string;
    url?: string;
    variable: string;
}
