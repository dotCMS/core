// Input type that you can select when creating the field
export enum INPUT_TYPE {
    TEXT = 'TEXT',
    INTEGER = 'INTEGER',
    FLOAT = 'FLOAT'
}

// This is to hold the options for the input type
export interface InputTextOptions {
    type: string;
    inputMode: string;
    step?: number;
}

// This is to hold the options for the input type
export const INPUT_TEXT_OPTIONS: Record<INPUT_TYPE, InputTextOptions> = {
    TEXT: {
        type: 'text',
        inputMode: 'text'
    },
    INTEGER: {
        type: 'number',
        inputMode: 'numeric',
        step: 1
    },
    FLOAT: {
        type: 'number',
        inputMode: 'decimal',
        step: 0.1
    }
};
