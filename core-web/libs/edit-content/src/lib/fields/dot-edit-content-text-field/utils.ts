import { ContentTypeTextField, DotCMSDataTypes } from "@dotcms/dotcms-models";


// This is to hold the options for the input type
export interface InputTextOptions {
    type: string;
    inputMode: string;
    step?: number;
}

// This is to hold the options for the input type
export const INPUT_TEXT_OPTIONS: Record<
    ContentTypeTextField['dataType'],
    InputTextOptions
> = {
    [DotCMSDataTypes.TEXT]: {
        type: 'text',
        inputMode: 'text'
    },
    [DotCMSDataTypes.INTEGER]: {
        type: 'number',
        inputMode: 'numeric',
        step: 1
    },
    [DotCMSDataTypes.FLOAT]: {
        type: 'number',
        inputMode: 'decimal',
        step: 0.1
    }
};
