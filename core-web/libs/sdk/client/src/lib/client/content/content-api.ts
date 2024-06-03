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
     * const blogs = await this.content
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
     * this.content
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
     *
     *
     * @param {string} contentType The content type to get the collection
     * @return {CollectionBuilder} CollectionBuilder to filter and fetch the collection
     * @memberof Content
     */
    getCollection<T>(contentType: string): CollectionBuilder<T> {
        return new CollectionBuilder(this.#requestOptions, this.#serverUrl, contentType);
    }
}
