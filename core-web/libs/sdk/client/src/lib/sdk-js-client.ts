type ClientConfig = {
    host: string;
    siteId: string;
    authToken: string;
};

type PageApiOptions = {
    path: string;
    host_id?: string;
    language_id?: number;
    'com.dotmarketing.persona.id'?: string;
    fireRules?: boolean;
    depth?: 0 | 1 | 2;
};

type NavApiOptions = {
    path: string;
    depth?: number;
    languageId?: number;
};

class DotCmsClient {
    private config: ClientConfig;

    constructor(config: ClientConfig) {
        if (!config.host || !config.siteId || !config.authToken) {
            throw new Error(
                'Invalid configuration - host, siteId, and authToken are required'
            );
        }

        this.config = config;
    }

    private validatePageOptions(options: PageApiOptions): void {
        if (!options.path) {
            throw new Error(
                "The 'path' parameter is required for the Page API"
            );
        }

        if (options.depth && ![0, 1, 2].includes(options.depth)) {
            throw new Error("Invalid 'depth' parameter. It must be 0, 1, or 2");
        }
    }

    private validateNavOptions(options: NavApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Nav API");
        }

        if (options.depth && options.depth < 1) {
            throw new Error(
                "Invalid 'depth' parameter. It must be 1 or greater"
            );
        }
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    async getPage(options: PageApiOptions): Promise<any> {
        this.validatePageOptions(options);

        const queryParamsObj: Record<string, string> = {};
        for (const [key, value] of Object.entries(options)) {
            if (value !== undefined) {
                queryParamsObj[key] = String(value);
            }
        }

        // Override or add the 'host_id' with the one from the config if it's not provided.
        queryParamsObj['host_id'] = options.host_id || this.config.siteId;

        const queryParams = new URLSearchParams(queryParamsObj).toString();

        const response = await fetch(
            `${this.config.host}/api/v1/page/json/${options.path}?${queryParams}`,
            {
                headers: {
                    Authorization: `Bearer ${this.config.authToken}`,
                },
            }
        );

        return response.json();
    }

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
        const url = `${this.config.host}/api/v1/nav${formattedPath}?${queryParams}`;

        const response = await fetch(url, {
            headers: {
                Authorization: `Bearer ${this.config.authToken}`,
            },
        });

        return response.json();
    }
}

// Usage
export const dotcmsClient = {
    init: (config: ClientConfig): DotCmsClient => {
        return new DotCmsClient(config);
    },
};
