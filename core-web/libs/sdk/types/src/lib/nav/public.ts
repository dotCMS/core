import { DotHttpError } from "../client/public";

/**
 * Navigation API specific error class
 * Wraps HTTP errors and adds navigation-specific context
 */
export class DotErrorNavigation extends Error {
    public readonly httpError?: DotHttpError;
    public readonly path: string;

    constructor(message: string, path: string, httpError?: DotHttpError) {
        super(message);
        this.name = 'DotNavigationError';
        this.path = path;
        this.httpError = httpError;

        // Ensure proper prototype chain for instanceof checks
        Object.setPrototypeOf(this, DotErrorNavigation.prototype);
    }

    /**
     * Serializes the error to a plain object for logging or transmission
     */
    toJSON() {
        return {
            name: this.name,
            message: this.message,
            path: this.path,
            httpError: this.httpError?.toJSON(),
            stack: this.stack
        };
    }
}

/**
 * The parameters for the Navigation API.
 * @public
 */
export interface DotCMSNavigationRequestParams {
    /**
     * The depth of the folder tree to return.
     * @example
     * `1` returns only the element specified in the path.
     * `2` returns the element specified in the path, and if that element is a folder, returns all direct children of that folder.
     * `3` returns all children and grandchildren of the element specified in the path.
     */
    depth?: number;

    /**
     * The language ID of content to return.
     * @example
     * `1` (or unspecified) returns content in the default language of the site.
     */
    languageId?: number;
}

