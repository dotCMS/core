import {
    DotCMSClientConfig,
    DotCMSNavigationRequestParams,
    RequestOptions,
    DotCMSNavigationItem
} from '@dotcms/types';

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
     * @returns {Promise<DotCMSNavigationItem[]>} - A Promise that resolves to the response from the DotCMS API.
     * @throws {Error} - Throws an error if the options are not valid.
     */
    async get(
        path: string,
        params?: DotCMSNavigationRequestParams
    ): Promise<DotCMSNavigationItem[]> {
        if (!path) {
            throw new Error("The 'path' parameter is required for the Navigation API");
        }

        const navParams = params ? this.mapToBackendParams(params) : {};
        const urlParams = new URLSearchParams(navParams).toString();

        const parsedPath = path.replace(/^\/+/, '/').replace(/\/+$/, '/');
        const url = `${this.BASE_URL}${parsedPath}${urlParams ? `?${urlParams}` : ''}`;

        const response = await fetch(url, this.requestOptions);

        if (!response.ok) {
            throw new Error(
                `Failed to fetch navigation data: ${response.statusText} - ${response.status}`
            );
        }

        return response.json().then((data) => data.entity);
    }

    private mapToBackendParams(params: DotCMSNavigationRequestParams): Record<string, string> {
        const backendParams: Record<string, string> = {};

        if (params.depth) {
            backendParams['depth'] = String(params.depth);
        }

        if (params.languageId) {
            backendParams['language_id'] = String(params.languageId);
        }

        return backendParams;
    }
}
