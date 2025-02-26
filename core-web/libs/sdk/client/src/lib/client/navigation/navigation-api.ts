import { DotCMSClientConfig, RequestOptions } from '../client';

interface NavRequestParams {
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

export class NavigationClient {
    private requestOptions: RequestOptions;

    private BASE_URL: string;

    constructor(config: DotCMSClientConfig, requestOptions: RequestOptions) {
        this.requestOptions = requestOptions;
        this.BASE_URL = `${config?.dotcmsUrl}/api/v1/nav`;
    }

    /**
     * Retrieves information about the dotCMS file and folder tree.
     * @param {NavigationApiOptions} options - The options for the Navigation API call. Defaults to `{ depth: 0, path: '/', languageId: 1 }`.
     * @returns {Promise<unknown>} - A Promise that resolves to the response from the DotCMS API.
     * @throws {Error} - Throws an error if the options are not valid.
     */
    async get(path: string, params?: NavRequestParams): Promise<unknown> {
        if (!path) {
            throw new Error("The 'path' parameter is required for the Navigation API");
        }

        const navParams = params ? this.mapToBackendParams(params) : {};
        const urlParams = new URLSearchParams(navParams).toString();

        const url = `${this.BASE_URL}/${path}${urlParams ? `?${urlParams}` : ''}`;

        const response = await fetch(url, this.requestOptions);

        if (!response.ok) {
            throw new Error(`Failed to fetch navigation data: ${response.statusText}`);
        }

        return response.json().then((data) => data.entity);
    }

    private mapToBackendParams(params: NavRequestParams): Record<string, string> {
        return {
            depth: params.depth ? String(params.depth) : '',
            language_id: params.languageId ? String(params.languageId) : ''
        };
    }
}
