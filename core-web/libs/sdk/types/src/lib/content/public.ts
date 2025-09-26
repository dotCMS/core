import { DotHttpError } from '../client/public';

/**
 * Content API specific error class
 * Wraps HTTP errors and adds content-specific context including query information
 */
export class DotErrorContent extends Error {
    public readonly httpError?: DotHttpError;
    public readonly contentType: string;
    public readonly operation: string;
    public readonly query?: string;

    constructor(
        message: string,
        contentType: string,
        operation: string,
        httpError?: DotHttpError,
        query?: string
    ) {
        super(message);
        this.name = 'DotCMSContentError';
        this.contentType = contentType;
        this.operation = operation;
        this.httpError = httpError;
        this.query = query;

        // Ensure proper prototype chain for instanceof checks
        Object.setPrototypeOf(this, DotErrorContent.prototype);
    }

    /**
     * Serializes the error to a plain object for logging or transmission
     */
    toJSON() {
        return {
            name: this.name,
            message: this.message,
            contentType: this.contentType,
            operation: this.operation,
            httpError: this.httpError?.toJSON(),
            query: this.query,
            stack: this.stack
        };
    }
}
