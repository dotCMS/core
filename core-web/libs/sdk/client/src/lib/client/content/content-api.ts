import { GetCollection } from './methods/get-collection/get-collection';

/**
 * Content classs exposes the content api methods
 *
 * @export
 * @class Content
 */
export class Content {
    private requestOptions: Omit<RequestInit, 'body' | 'method'>;
    private serverUrl;

    constructor(requestOptions: Omit<RequestInit, 'body' | 'method'>, serverUrl: string) {
        this.requestOptions = requestOptions;
        this.serverUrl = serverUrl;
    }

    /**
     * Allows you to build a query to get a collection of an specified content type
     *
     * @param {string} contentType
     * @return {*}
     * @memberof Content
     */
    getCollection(contentType: string): GetCollection {
        return new GetCollection(this.requestOptions, this.serverUrl, contentType);
    }
}
