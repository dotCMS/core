type ClientConfig = {
    dotcmsUrl: string;
    host_id: string;
    authToken: string;
};

type PageApiOptions = {
    path: string;
    host_id?: string;
    language_id?: number;
    personaId?: string;
    fireRules?: boolean;
    depth?: number;
};

type NavApiOptions = {
    path: string;
    depth?: number;
    languageId?: number;
};

function isValidUrl(url: string): boolean {
    try {
        new URL(url);

        return true;
    } catch (error) {
        return false;
    }
}

class DotCmsClient {
    private config: ClientConfig;

    constructor(config: ClientConfig) {
        if (!config.dotcmsUrl) {
            throw new Error("Invalid configuration - 'dotcmsUrl' is required");
        }

        if (!isValidUrl(config.dotcmsUrl)) {
            throw new Error("Invalid configuration - 'dotcmsUrl' must be a valid URL");
        }

        if (!config.host_id) {
            throw new Error("Invalid configuration - 'siteId' is required");
        }

        if (!config.authToken) {
            throw new Error("Invalid configuration - 'authToken' is required");
        }

        this.config = config;
    }

    /**
     * @description
     * The Page API enables you to retrieve all the elements of any Page in your dotCMS system.
     * The elements may be retrieved in JSON format.
     *
     * @link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas
     *
     *
     * @param {PageApiOptions} options
     * @return {*}  {Promise<any>}
     * @memberof DotCmsClient
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    async getPage(options: PageApiOptions): Promise<any> {
        this.validatePageOptions(options);

        const queryParamsObj: Record<string, string> = {};
        for (const [key, value] of Object.entries(options)) {
            if (value !== undefined && key !== 'path') {
                if (key === 'personaId') {
                    queryParamsObj['com.dotmarketing.persona.id'] = String(value);
                } else {
                    queryParamsObj[key] = String(value);
                }
            }
        }

        // Override or add the 'host_id' with the one from the config if it's not provided.
        queryParamsObj['host_id'] = options.host_id || this.config.host_id;

        const queryParams = new URLSearchParams(queryParamsObj).toString();

        const formattedPath = options.path.startsWith('/') ? options.path : `/${options.path}`;
        const response = await fetch(
            `${this.config.dotcmsUrl}/api/v1/page/json${formattedPath}?${queryParams}`,
            {
                headers: {
                    Authorization: `Bearer ${this.config.authToken}`
                }
            }
        );

        return response.json();
    }

    /**
     * @description
     *  Enables you to retrieve information about the dotCMS file and folder tree through REST API calls.
     *
     * @link https://www.dotcms.com/docs/latest/navigation-rest-api
     *
     * @param {NavApiOptions} options
     * @return {*}  {Promise<any>}
     * @memberof DotCmsClient
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    async getNav(options: NavApiOptions): Promise<any> {
        this.validateNavOptions(options);

        // Extract the 'path' from the options and prepare the rest as query parameters
        const { path, ...queryParamsOptions } = options;
        const queryParamsObj: Record<string, string> = {};
        Object.entries(queryParamsOptions).forEach(([key, value]) => {
            if (value !== undefined) {
                queryParamsObj[key] = String(value);
            }
        });

        const queryParams = new URLSearchParams(queryParamsObj).toString();

        // Format the URL correctly depending on the 'path' value
        const formattedPath = path === '/' ? '/' : `/${path}`;
        const url = `${this.config.dotcmsUrl}/api/v1/nav${formattedPath}${
            queryParams ? `?${queryParams}` : ''
        }`;

        const response = await fetch(url, {
            headers: {
                Authorization: `Bearer ${this.config.authToken}`
            }
        });

        return response.json();
    }

    private validatePageOptions(options: PageApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Page API");
        }
    }

    private validateNavOptions(options: NavApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Nav API");
        }
    }
}

// Usage
export const dotcmsClient = {
    init: (config: ClientConfig): DotCmsClient => {
        return new DotCmsClient(config);
    }
};
