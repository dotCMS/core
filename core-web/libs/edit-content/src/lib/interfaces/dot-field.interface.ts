export interface DotField {
    type: string;
    hint: string;
    label: string;
    required: boolean;
    regexCheck: string;
}

// Not used yet
export enum DotFieldType {
    ROW,
    COLUMN,
    TEXT,
    DROPDOWN
}

export interface HostFieldData {
    identiter: string;
    name: string;
}
