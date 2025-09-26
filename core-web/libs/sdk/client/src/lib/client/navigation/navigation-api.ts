import {
    DotCMSClientConfig,
    DotCMSNavigationRequestParams,
    DotRequestOptions,
    DotCMSNavigationItem,
    DotHttpClient,
    DotHttpError,
    DotErrorNavigation
} from '@dotcms/types';

export class NavigationClient {
    private requestOptions: DotRequestOptions;
    private BASE_URL: string;
    private httpClient: DotHttpClient;

    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient
    ) {
        this.requestOptions = requestOptions;
        this.BASE_URL = `${config?.dotcmsUrl}/api/v1/nav`;
        this.httpClient = httpClient;
    }

    /**
     * Retrieves information about the dotCMS file and folder tree.
     * @param {string} path - The path to retrieve navigation for.
     * @param {DotCMSNavigationRequestParams} params - The options for the Navigation API call.
     * @returns {Promise<DotCMSNavigationItem[]>} - A Promise that resolves to the response from the DotCMS API.
     * @throws {DotErrorNavigation} - Throws a navigation-specific error if the request fails.
     */
    async get(
        path: string,
        params?: DotCMSNavigationRequestParams
    ): Promise<DotCMSNavigationItem[]> {
        if (!path) {
            throw new DotErrorNavigation(
                "The 'path' parameter is required for the Navigation API",
                path
            );
        }

        const navParams = params ? this.mapToBackendParams(params) : {};
        const urlParams = new URLSearchParams(navParams).toString();

        const parsedPath = path.replace(/^\/+/, '/').replace(/\/+$/, '/');
        const url = `${this.BASE_URL}${parsedPath}${urlParams ? `?${urlParams}` : ''}`;

        try {
            const response = await this.httpClient.request<{ entity: DotCMSNavigationItem[] }>(
                url,
                this.requestOptions
            );

            return response.entity;
        } catch (error) {
            // Handle DotHttpError instances from httpClient.request
            if (error instanceof DotHttpError) {
                throw new DotErrorNavigation(
                    `Navigation API failed for path '${parsedPath}': ${error.message}`,
                    parsedPath,
                    error
                );
            }

            // Handle other errors (validation, network, etc.)
            throw new DotErrorNavigation(
                `Navigation API failed for path '${parsedPath}': ${error instanceof Error ? error.message : 'Unknown error'}`,
                parsedPath
            );
        }
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
