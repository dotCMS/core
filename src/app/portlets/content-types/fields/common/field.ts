
export interface Field {
    dataType: string;
    fixed?: boolean;
    indexed?: boolean;
    listed?: boolean;
    name?: string;
    readOnly?: boolean;
    required?: boolean;
    sortOrder?: number;
    unique?: boolean;
    velocityVarName?: string;
}