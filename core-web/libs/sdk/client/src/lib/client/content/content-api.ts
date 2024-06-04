import { CollectionBuilder } from './builders/collection/collection';

import { ClientOptions } from '../sdk-js-client';

/**
 * Content classs exposes the content api methods
 *
 * @export
 * @class Content
 */
export class Content {
    #requestOptions: ClientOptions;
    #serverUrl: string;

    constructor(requestOptions: ClientOptions, serverUrl: string) {
        this.#requestOptions = requestOptions;
        this.#serverUrl = serverUrl;
    }

    /**
     * Takes a content type and returns a builder to filter and fetch the collection
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
     * // Using an specific type for your content
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
     * @param {string} contentType The content type to get the collection
     * @return {CollectionBuilder} CollectionBuilder to filter and fetch the collection
     * @template T Represents the type of the content type. defaults to unknown
     * @memberof Content
     */
    getCollection<T = unknown>(contentType: string): CollectionBuilder<T> {
        return new CollectionBuilder(this.#requestOptions, this.#serverUrl, contentType);
    }
}
