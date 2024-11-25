import { ErrorHandler, signal } from '@angular/core';

export type DotErrorContext = {
    title?: string;
    contentType?: string;
    identifier?: string;
    inode?: string;
    uuid?: string;
    row?: number;
    column?: number;
};

export enum DotErrorCodes {
    ROW001 = 'ROW001',
    COL001 = 'COL001',
    CON001 = 'CON001',
    CON002 = 'CON002'
}

export const ERROR_MAP: Record<DotErrorCodes, (context: DotErrorContext) => string> = {
    [DotErrorCodes.ROW001]: ({ row }) =>
        `Error found on Row\nRow Index: ${row}\nCheck more on dotcms.docs.com/errors/row001`, // We can add more info
    [DotErrorCodes.COL001]: ({ row, column }) =>
        `Error found on Column\nRow Index: ${row}\nColumn Index: ${column}\nCheck more on dotcms.docs.com/errors/col001`,
    [DotErrorCodes.CON001]: ({ row, column, identifier, uuid }) =>
        `Error found on Container\nRow Index: ${row}\nColumn Index: ${column}\nContainer Identifier: ${identifier}\nContainer UUID: ${uuid}\nCheck more on dotcms.docs.com/errors/con001`,
    [DotErrorCodes.CON002]: ({ row, column, uuid, identifier, contentType, title }) =>
        `Error found on Contentlet\nRow Index: ${row}\nColumn Index: ${column}\nContainer UUID: ${uuid}\nContentlet Identifier: ${identifier}\nContentlet ContentType: ${contentType}\nContentlet Title: ${title}\nCheck more on dotcms.docs.com/errors/con002`
};

export class DotError extends Error {
    context: DotErrorContext;

    constructor(message: DotErrorCodes, context: DotErrorContext) {
        super(ERROR_MAP[message](context));
        this.name = 'DotError';

        this.context = context;
    }
}

export class DotErrorHandler implements ErrorHandler {
    errorSignal = signal<DotError | null>(null);

    handleError(error: unknown): void {
        if (error instanceof DotError) {
            this.errorSignal.set(error); // Set the error signal
            console.error('DotError:\n', error.message);
        } else {
            console.error('Unexpected error:', error);
        }
    }

    clearError(): void {
        this.errorSignal.set(null); // Reset the error signal
    }
}
