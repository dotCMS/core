import { consola } from 'consola';

import { DotCMSClientConfig, DotHttpClient, DotRequestOptions } from '@dotcms/types';

import { FetchHttpClient } from '../adapters/fetch-http-client';

/**
 * Parses a string into a URL object.
 *
 * @param url - The URL string to parse
 * @returns A URL object if parsing is successful, undefined otherwise
 */
function parseURL(url: string): URL | undefined {
    try {
        return new URL(url);
    } catch {
        consola.error('[DotCMS Client]: Invalid URL:', url);

        return undefined;
    }
}

/**
 * @internal
 *
 * The validated configuration and transport every API client is constructed with.
 */
export interface DotCMSClientContext {
    config: DotCMSClientConfig;
    requestOptions: DotRequestOptions;
    httpClient: DotHttpClient;
}

/**
 * @internal
 *
 * Validates the caller's configuration and builds the transport shared by every API client.
 *
 * This is the single place that normalizes `dotcmsUrl` to an origin, enforces the presence of
 * an auth token, and attaches the `Authorization` header — so `createDotCMSClient` and the
 * focused subpath factories (`@dotcms/client/page`, `/navigation`, `/content`, `/ai`) all
 * behave identically, including their error messages.
 *
 * @param clientConfig - Configuration options for the client
 * @returns the validated config plus the request options and HTTP client to construct APIs with
 * @throws {TypeError} when `dotcmsUrl` is not a valid URL or `authToken` is missing
 */
export function createClientContext(clientConfig: DotCMSClientConfig): DotCMSClientContext {
    const { dotcmsUrl, authToken } = clientConfig || {};
    const instanceUrl = parseURL(dotcmsUrl)?.origin;

    if (!instanceUrl) {
        throw new TypeError("Invalid configuration - 'dotcmsUrl' must be a valid URL");
    }

    if (!authToken) {
        throw new TypeError("Invalid configuration - 'authToken' is required");
    }

    const config: DotCMSClientConfig = {
        ...clientConfig,
        authToken,
        dotcmsUrl: instanceUrl
    };

    return {
        config,
        httpClient: config.httpClient || new FetchHttpClient(),
        requestOptions: {
            ...config.requestOptions,
            headers: {
                ...config.requestOptions?.headers,
                Authorization: `Bearer ${config.authToken}`
            }
        }
    };
}
