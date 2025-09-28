import { DotHttpError } from '../client/public';

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

/**
 * Represents a navigation item in the DotCMS navigation structure
 *
 * @interface DotCMSNavigationItem
 * @property {string} [code] - Optional unique code identifier for the navigation item
 * @property {string} folder - The folder path where this navigation item is located
 * @property {DotCMSNavigationItem[]} [children] - Optional array of child navigation items
 * @property {string} host - The host/site this navigation item belongs to
 * @property {number} languageId - The language ID for this navigation item
 * @property {string} href - The URL/link that this navigation item points to
 * @property {string} title - The display title of the navigation item
 * @property {string} type - The type of navigation item
 * @property {number} hash - Hash value for the navigation item
 * @property {string} target - The target attribute for the link (e.g. "_blank", "_self")
 * @property {number} order - The sort order position of this item in the navigation
 */
export interface DotCMSNavigationItem {
    code?: string;
    folder: string;
    children?: DotCMSNavigationItem[];
    host: string;
    languageId: number;
    href: string;
    title: string;
    type: string;
    hash: number;
    target: string;
    order: number;
}
