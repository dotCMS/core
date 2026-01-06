import { DotRequestOptions, DotHttpClient, DotCMSClientConfig } from '@dotcms/types';

import { CollectionBuilder } from './builders/collection/collection';
import { RawQueryBuilder } from './builders/rawQuery/raw-query-builder';

import { BaseApiClient } from '../base/api/base-api';

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
export class Content extends BaseApiClient {
    /**
     * Creates an instance of Content.
     * @param {DotCMSClientConfig} config - Configuration options for the DotCMS client
     * @param {DotRequestOptions} requestOptions - The options for the client request.
     * @param {DotHttpClient} httpClient - HTTP client for making requests.
     */

    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient
    ) {
        super(config, requestOptions, httpClient);
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
        return new CollectionBuilder<T>({
            requestOptions: this.requestOptions,
            config: this.config,
            contentType,
            httpClient: this.httpClient
        });
    }

    /**
     * Executes a raw Lucene query with optional pagination, sorting, and rendering options.
     *
     * This method provides direct access to Lucene query syntax, giving you full control
     * over query construction while still benefiting from common features like pagination,
     * sorting, and error handling.
     *
     * **Important Notes:**
     * - The raw query is used as-is (minimal sanitization only)
     * - NO automatic field prefixing (unlike `getCollection()`)
     * - NO implicit/system constraints are added (language, live/draft, site, variant, etc.)
     * - If you need constraints, include them in the raw query string (e.g. `+contentType:Blog +languageId:1 +live:true`)
     *
     * @template T - The type of the content items (defaults to unknown)
     * @param {string} rawQuery - Raw Lucene query string
     * @return {RawQueryBuilder<T>} A RawQueryBuilder instance for chaining options
     * @memberof Content
     *
     * @example Simple query with pagination
     * ```typescript
     * const response = await client.content
     *     .query('+contentType:Blog +title:"Hello World"')
     *     .limit(10)
     *     .page(1);
     *
     * console.log(response.contentlets);
     * ```
     *
     * @example Complex query with all available options
     * ```typescript
     * const response = await client.content
     *     .query('+(contentType:Blog OR contentType:News) +tags:"technology" +languageId:1 +(live:false AND working:true AND deleted:false) +variant:legends-forceSensitive')
     *     .limit(20)
     *     .page(2)
     *     .sortBy([{ field: 'modDate', order: 'desc' }])
     *     .render()
     *     .depth(2);
     *
     * console.log(`Found ${response.total} items`);
     * response.contentlets.forEach(item => console.log(item.title));
     * ```
     *
     * @example Using TypeScript generics for type safety
     * ```typescript
     * interface BlogPost {
     *     title: string;
     *     author: string;
     *     publishDate: string;
     * }
     *
     * const response = await client.content
     *     .query<BlogPost>('+contentType:Blog +author:"John Doe"')
     *     .limit(10);
     *
     * // TypeScript knows the type of contentlets
     * response.contentlets.forEach(post => {
     *     console.log(post.title, post.author);
     * });
     * ```
     *
     * @example Error handling with helpful messages
     * ```typescript
     * try {
     *     const response = await client.content
     *         .query('+contentType:Blog +publishDate:[2024-01-01 TO 2024-12-31]');
     * } catch (error) {
     *     if (error instanceof DotErrorContent) {
     *         console.error('Query failed:', error.message);
     *         console.error('Failed query:', error.query);
     *     }
     * }
     * ```
     *
     * @example Using Promise chain instead of async/await
     * ```typescript
     * client.content
     *     .query('+contentType:Blog')
     *     .limit(10)
     *     .then(response => console.log(response.contentlets))
     *     .catch(error => console.error(error));
     * ```
     */
    query<T = unknown>(rawQuery: string): RawQueryBuilder<T> {
        return new RawQueryBuilder<T>({
            requestOptions: this.requestOptions,
            config: this.config,
            rawQuery,
            httpClient: this.httpClient
        });
    }
}
