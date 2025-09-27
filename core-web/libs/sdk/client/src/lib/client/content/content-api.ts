import { DotRequestOptions, DotHttpClient, DotCMSClientConfig } from '@dotcms/types';

import { CollectionBuilder } from './builders/collection/collection';

/**
 * Creates a builder to filter and fetch a collection of content items.
 * @param contentType - The content type to retrieve.
 * @returns A CollectionBuilder instance for chaining filters and executing the query.
 * @template T - The type of the content items (defaults to unknown).
 *
 * @example Fetch blog posts with async/await
 * ```typescript
 * const response = await client.content
 *     .getCollection<BlogPost>('Blog')
 *     .limit(10)
 *     .page(2)
 *     .sortBy([{ field: 'title', order: 'asc' }])
 *     .query(q => q.field('author').equals('John Doe'))
 *     .depth(1)
 *
 * console.log(response.contentlets);
 * ```
 *
 * @example Fetch blog posts with Promise chain
 * ```typescript
 * client.content
 *     .getCollection<BlogPost>('Blog')
 *     .limit(10)
 *     .page(2)
 *     .sortBy([{ field: 'title', order: 'asc' }])
 *     .query(q => q.field('author').equals('John Doe'))
 *     .depth(1)
 *     .then(response => console.log(response.contentlets))
 *     .catch(error => console.error(error));
 * ```
 *
 * @example Using a custom type
 * ```typescript
 * interface BlogPost {
 *     summary: string;
 *     author: string;
 *     title: string;
 * }
 *
 * const posts = await client.content
 *     .getCollection<BlogPost>('Blog')
 *     .limit(10)
 *
 * posts.contentlets.forEach(post => {
 *     console.log(post.title, post.author, post.summary);
 * });
 * ```
 */
export class Content {
    #requestOptions: DotRequestOptions;
    #httpClient: DotHttpClient;
    #config: DotCMSClientConfig;

    /**
     * Creates an instance of Content.
     * @param {DotRequestOptions} requestOptions - The options for the client request.
     * @param {string} serverUrl - The server URL.
     * @param {DotHttpClient} httpClient - HTTP client for making requests.
     */

    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient
    ) {
        this.#requestOptions = requestOptions;
        this.#config = config;
        this.#httpClient = httpClient;
    }

    /**
     * Takes a content type and returns a builder to filter and fetch the collection.
     * @param {string} contentType - The content type to get the collection.
     * @return {CollectionBuilder<T>} CollectionBuilder to filter and fetch the collection.
     * @template T - Represents the type of the content type to fetch. Defaults to unknown.
     * @memberof Content
     *
     * @example
     * ```javascript
     * // Using await and async
     * const collectionResponse = await client.content
     *     .getCollection('Blog')
     *     .limit(10)
     *     .page(2)
     *     .sortBy([{ field: 'title', order: 'asc' }])
     *     .query((queryBuilder) => queryBuilder.field('author').equals('John Doe'))
     *     .depth(1);
     * ```
     * @example
     * ```javascript
     * // Using then and catch
     * client.content
     *      .getCollection('Blog')
     *      .limit(10)
     *      .page(2)
     *      .sortBy([{ field: 'title', order: 'asc' }])
     *      .query((queryBuilder) => queryBuilder.field('author').equals('John Doe'))
     *      .depth(1)
     *      .then((response) => {
     *          console.log(response.contentlets);
     *      })
     *      .catch((error) => {
     *          console.error(error);
     *      });
     * ```
     * @example
     * ```typescript
     * // Using a specific type for your content
     *
     * type Blog = {
     *     summary: string;
     *     author: string;
     *     title: string;
     * };
     *
     * client.content
     *     .getCollection<Blog>('Blog')
     *     .limit(10)
     *     .page(2)
     *     .sortBy([{ field: 'title', order: 'asc' }])
     *     .query((queryBuilder) => queryBuilder.field('author').equals('John Doe'))
     *     .depth(1)
     *     .then((response) => {
     *         response.contentlets.forEach((blog) => {
     *             console.log(blog.title);
     *             console.log(blog.author);
     *             console.log(blog.summary);
     *         });
     *     })
     *     .catch((error) => {
     *         console.error(error);
     *     });
     * ```
     *
     */
    getCollection<T = unknown>(contentType: string): CollectionBuilder<T> {
        return new CollectionBuilder<T>(
            this.#requestOptions,
            this.#config,
            contentType,
            this.#httpClient
        );
    }
}
