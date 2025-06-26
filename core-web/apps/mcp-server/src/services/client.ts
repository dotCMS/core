/**
 * AgnosticClient provides a base HTTP client for dotCMS REST API services.
 *
 * This class automatically reads the dotCMS URL and authentication token from environment variables.
 * It provides a protected fetch method that child service classes can use to make authenticated requests.
 */
export class AgnosticClient {
    /**
     * The base URL for the dotCMS API, read from the DOTCMS_URL environment variable.
     */
    protected dotcmsUrl: string;

    /**
     * The authentication token for dotCMS API requests, read from the AUTH_TOKEN environment variable.
     * This value is required.
     */
    protected authToken: string;

    /**
     * Constructs an AgnosticClient instance.
     *
     * @throws {Error} If the DOTCMS_URL or AUTH_TOKEN environment variable is not set.
     */
    constructor() {
        this.dotcmsUrl = process.env.DOTCMS_URL || '';
        this.authToken = process.env.AUTH_TOKEN || '';

        if (!this.dotcmsUrl) {
            throw new Error('DOTCMS_URL environment variable is required');
        }

        if (!this.authToken) {
            throw new Error('AUTH_TOKEN environment variable is required');
        }
    }

    /**
     * Makes an HTTP request to the specified URL with the provided options, automatically adding the Authorization header.
     *
     * @param url - The full URL to request.
     * @param options - Optional fetch options (method, headers, body, etc.).
     * @returns A Promise resolving to the fetch Response object.
     */
    protected async fetch(url: string, options: RequestInit = {}): Promise<Response> {
        const headers: Record<string, string> = {
            ...(options.headers as Record<string, string> || {}),
        };
        headers['Authorization'] = `Bearer ${this.authToken}`;

        return fetch(url, { ...options, headers });
    }
}