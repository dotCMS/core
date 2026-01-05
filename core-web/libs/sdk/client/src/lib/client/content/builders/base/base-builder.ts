import {
    DotHttpClient,
    DotRequestOptions,
    DotCMSClientConfig,
    DotErrorContent
} from '@dotcms/types';

import { BaseApiClient } from '../../../base/base-api';
import { CONTENT_API_URL } from '../../shared/const';
import {
    GetCollectionResponse,
    SortBy,
    GetCollectionRawResponse,
    OnFullfilled,
    OnRejected
} from '../../shared/types';

/**
 * Abstract base class for content builders that provides common functionality
 * for querying content from the DotCMS Content API.
 *
 * This class extracts shared behavior between different builder implementations,
 * including pagination, sorting, rendering, and response formatting.
 *
 * @export
 * @abstract
 * @class BaseBuilder
 * @template T - The type of the content items (defaults to unknown)
 */
export abstract class BaseBuilder<T = unknown> extends BaseApiClient {
    /**
     * Current page number for pagination.
     * @private
     */
    #page = 1;

    /**
     * Maximum number of items per page.
     * @private
     */
    #limit = 10;

    /**
     * Depth of related content to include in results.
     * Valid values: 0-3
     * @private
     */
    #depth = 0;

    /**
     * Whether to server-side render widgets in content.
     * @private
     */
    #render = false;

    /**
     * Sorting configuration for results.
     * @private
     */
    #sortBy?: SortBy[];

    /**
     * Creates an instance of BaseBuilder.
     *
     * @param {object} params - Constructor parameters
     * @param {DotRequestOptions} params.requestOptions - Options for the client request
     * @param {DotCMSClientConfig} params.config - The client configuration
     * @param {DotHttpClient} params.httpClient - HTTP client for making requests
     * @memberof BaseBuilder
     */
    constructor(params: {
        requestOptions: DotRequestOptions;
        config: DotCMSClientConfig;
        httpClient: DotHttpClient;
    }) {
        super(params.config, params.requestOptions, params.httpClient);
    }

    /**
     * Returns the offset for pagination based on current page and limit.
     *
     * @readonly
     * @protected
     * @memberof BaseBuilder
     */
    protected get offset(): number {
        return this.#limit * (this.#page - 1);
    }

    /**
     * Returns the sort query in the format: field order, field order, ...
     *
     * @readonly
     * @protected
     * @memberof BaseBuilder
     */
    protected get sort(): string | undefined {
        return this.#sortBy?.map((sort) => `${sort.field} ${sort.order}`).join(',');
    }

    /**
     * Returns the full URL for the content API.
     *
     * @readonly
     * @protected
     * @memberof BaseBuilder
     */
    protected get url(): string {
        return `${this.config.dotcmsUrl}${CONTENT_API_URL}`;
    }

    /**
     * Sets the maximum amount of content to fetch.
     *
     * @example
     * ```typescript
     * builder.limit(20);
     * ```
     *
     * @param {number} limit - The maximum amount of content to fetch
     * @return {this} The builder instance for method chaining
     * @memberof BaseBuilder
     */
    limit(limit: number): this {
        this.#limit = limit;

        return this;
    }

    /**
     * Sets the page number to fetch.
     *
     * @example
     * ```typescript
     * builder.page(2);
     * ```
     *
     * @param {number} page - The page number to fetch
     * @return {this} The builder instance for method chaining
     * @memberof BaseBuilder
     */
    page(page: number): this {
        this.#page = page;

        return this;
    }

    /**
     * Sorts the content by the specified fields and orders.
     *
     * @example
     * ```typescript
     * const sortBy = [
     *     { field: 'title', order: 'asc' },
     *     { field: 'modDate', order: 'desc' }
     * ];
     * builder.sortBy(sortBy);
     * ```
     *
     * @param {SortBy[]} sortBy - Array of constraints to sort the content by
     * @return {this} The builder instance for method chaining
     * @memberof BaseBuilder
     */
    sortBy(sortBy: SortBy[]): this {
        this.#sortBy = sortBy;

        return this;
    }

    /**
     * Setting this to true will server-side render (using velocity) any widgets
     * that are returned by the content query.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/content-api-retrieval-and-querying#ParamsOptional}
     *
     * @example
     * ```typescript
     * builder.render();
     * ```
     *
     * @return {this} The builder instance for method chaining
     * @memberof BaseBuilder
     */
    render(): this {
        this.#render = true;

        return this;
    }

    /**
     * Sets the depth of the relationships of the content.
     * Specifies the depth of related content to return in the results.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/content-api-retrieval-and-querying#ParamsOptional}
     *
     * @example
     * ```typescript
     * builder.depth(2);
     * ```
     *
     * @param {number} depth - The depth of the relationships of the content (0-3)
     * @return {this} The builder instance for method chaining
     * @throws {Error} When depth is not between 0 and 3
     * @memberof BaseBuilder
     */
    depth(depth: number): this {
        if (depth < 0 || depth > 3) {
            throw new Error('Depth value must be between 0 and 3');
        }

        this.#depth = depth;

        return this;
    }

    /**
     * Executes the fetch and returns a promise that resolves to the content or rejects with an error.
     *
     * @example
     * ```typescript
     * builder
     *     .limit(10)
     *     .then((response) => console.log(response))
     *     .catch((error) => console.error(error));
     * ```
     *
     * @example Using async/await
     * ```typescript
     * const response = await builder.limit(10);
     * ```
     *
     * @param {OnFullfilled} [onfulfilled] - A callback that is called when the fetch is successful
     * @param {OnRejected} [onrejected] - A callback that is called when the fetch fails
     * @return {Promise<GetCollectionResponse<T> | DotErrorContent>} A promise that resolves to the content or rejects with an error
     * @memberof BaseBuilder
     */
    then(
        onfulfilled?: OnFullfilled<T>,
        onrejected?: OnRejected
    ): Promise<GetCollectionResponse<T> | DotErrorContent> {
        return this.fetch().then(
            (data) => {
                const formattedResponse = this.formatResponse<T>(data);

                if (typeof onfulfilled === 'function') {
                    const result = onfulfilled(formattedResponse);
                    // Ensure we always return a value, fallback to formattedResponse if callback returns undefined
                    return result ?? formattedResponse;
                }

                return formattedResponse;
            },
            (error: unknown) => {
                // Wrap error in DotErrorContent
                const contentError = this.wrapError(error);

                if (typeof onrejected === 'function') {
                    const result = onrejected(contentError);
                    // Ensure we always return a value, fallback to original error if callback returns undefined
                    return result ?? contentError;
                }

                // Throw the wrapped error to trigger .catch()
                throw contentError;
            }
        );
    }

    /**
     * Formats the response to the desired format.
     *
     * @protected
     * @param {GetCollectionRawResponse<T>} data - The raw response data
     * @return {GetCollectionResponse<T>} The formatted response
     * @memberof BaseBuilder
     */
    protected formatResponse<T>(data: GetCollectionRawResponse<T>): GetCollectionResponse<T> {
        const contentlets = data.entity.jsonObjectView.contentlets;
        const total = data.entity.resultsSize;

        const mappedResponse: GetCollectionResponse<T> = {
            contentlets,
            total,
            page: this.#page,
            size: contentlets.length
        };

        return this.#sortBy
            ? {
                  ...mappedResponse,
                  sortedBy: this.#sortBy
              }
            : mappedResponse;
    }

    /**
     * Calls the content API to fetch the content.
     *
     * @protected
     * @return {Promise<GetCollectionRawResponse<T>>} The fetch response data
     * @throws {DotHttpError} When the HTTP request fails
     * @memberof BaseBuilder
     */
    protected fetch(): Promise<GetCollectionRawResponse<T>> {
        const finalQuery = this.buildFinalQuery();
        const languageId = this.getLanguageId();

        return this.httpClient.request<GetCollectionRawResponse<T>>(this.url, {
            ...this.requestOptions,
            method: 'POST',
            headers: {
                ...this.requestOptions.headers,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                query: finalQuery,
                render: this.#render,
                sort: this.sort,
                limit: this.#limit,
                offset: this.offset,
                depth: this.#depth,
                ...(languageId !== undefined ? { languageId } : {})
            })
        });
    }

    /**
     * Wraps an error in a DotErrorContent instance with helpful context.
     *
     * @protected
     * @param {unknown} error - The error to wrap
     * @return {DotErrorContent} The wrapped error
     * @memberof BaseBuilder
     */
    protected abstract wrapError(error: unknown): DotErrorContent;

    /**
     * Builds the final Lucene query string.
     * Subclasses must implement this method to define their query building strategy.
     *
     * @protected
     * @abstract
     * @return {string} The final Lucene query string
     * @memberof BaseBuilder
     */
    protected abstract buildFinalQuery(): string;

    /**
     * Gets the language ID for the query.
     * Subclasses must implement this method to provide the language ID.
     *
     * @protected
     * @abstract
     * @return {number | string} The language ID
     * @memberof BaseBuilder
     */
    protected abstract getLanguageId(): number | string | undefined;
}
