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
     * @param {string} contentType The content type to get the collection
     * @return {CollectionBuilder} CollectionBuilder to filter and fetch the collection
     * @memberof Content
     */
    getCollection(contentType: string): CollectionBuilder {
        return new CollectionBuilder(this.#requestOptions, this.#serverUrl, contentType);
    }
}
