import { DotCMSClientConfig, RequestOptions } from '../client';

type NavigationApiOptions = {
    /**
     * The root path to begin traversing the folder tree.
     * @example
     * `/api/v1/nav/` starts from the root of the site
     * @example
     * `/about-us` starts from the "About Us" folder
     */
    path: string;

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
};

export class NavigationClient {
    private config: DotCMSClientConfig;
    private requestOptions: RequestOptions;

    constructor(config: DotCMSClientConfig, requestOptions: RequestOptions) {
        this.config = config;
        this.requestOptions = requestOptions;
    }

    /**
     * Retrieves information about the dotCMS file and folder tree.
     * @param {NavigationApiOptions} options - The options for the Navigation API call. Defaults to `{ depth: 0, path: '/', languageId: 1 }`.
     * @returns {Promise<unknown>} - A Promise that resolves to the response from the DotCMS API.
     * @throws {Error} - Throws an error if the options are not valid.
     */
    async getNavigation(
        options: NavigationApiOptions = { depth: 0, path: '/', languageId: 1 }
    ): Promise<unknown> {
        const { path, ...queryParamsOptions } = options;
        const queryParams: Record<string, string> = {};

        Object.entries(queryParamsOptions).forEach(([key, value]) => {
            if (value !== undefined) {
                queryParams[key] = String(value);
            }
        });

        const queryString = new URLSearchParams(queryParams).toString();
        const formattedPath = path === '/' ? '/' : `/${path}`;
        const url = `${this.config.dotcmsUrl}/api/v1/nav${formattedPath}${queryString ? `?${queryString}` : ''}`;

        const response = await fetch(url, this.requestOptions);

        if (!response.ok) {
            throw new Error(`Failed to fetch navigation data: ${response.statusText}`);
        }

        return response.json();
    }
}
