import { buildQuery } from './utils';

import { ClientOptions } from '../sdk-js-client';

export class GQLClient {
    #requestOptions: ClientOptions;
    #serverUrl: string;
    #query: string;
    #variables: Record<string, string>;

    /**
     * Creates an instance of Content.
     * @param {ClientOptions} requestOptions - The options for the client request.
     * @param {string} serverUrl - The server URL.
     */
    constructor(
        requestOptions: ClientOptions,
        serverUrl: string,
        page: { url: string; mode: string; language: string; pageFragment: string },
        content: Record<string, string> = {}
    ) {
        this.#requestOptions = requestOptions;
        this.#serverUrl = serverUrl;

        this.#query = buildQuery({
            pageFragment: page.pageFragment,
            contentQueries: Object.entries(content).reduce((acc, [key, value]) => {
                return `${acc}
                ${key}: ${value}`;
            }, '')
        });
        this.#variables = {
            url: page.url,
            mode: page.mode,
            languageId: page.language
        };
    }

    private get url() {
        return `${this.#serverUrl}/api/v1/graphql`;
    }

    private fetch() {
        return fetch(this.url, {
            ...this.#requestOptions,
            method: 'POST',
            headers: {
                ...this.#requestOptions.headers,
                'Content-Type': 'application/json',
                dotcachettl: '0' // Bypasses GraphQL cache
            },
            body: JSON.stringify({
                query: this.#query,
                variables: this.#variables
            }),
            cache: 'no-cache' // Invalidate cache for Next.js
        });
    }

    then(
        onfulfilled?:
            | ((error: unknown) => unknown | PromiseLike<unknown> | void)
            | undefined
            | null,
        onrejected?: ((error: unknown) => unknown | PromiseLike<unknown> | void) | undefined | null
    ): Promise<unknown | unknown> {
        return this.fetch().then(async (response) => {
            const data = await response.json();
            if (response.ok) {
                const formattedResponse = {
                    ...data,
                    queryMetadata: {
                        query: this.#query,
                        variables: this.#variables
                    }
                };

                const finalResponse =
                    typeof onfulfilled === 'function' ? onfulfilled(formattedResponse) : data;

                return {
                    ...finalResponse
                };
            } else {
                return {
                    status: response.status,
                    ...data
                };
            }
        }, onrejected);
    }
}
