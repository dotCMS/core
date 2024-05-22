import { GetCollection } from './methods/get-collection';

export class Content {
    #requestOptions: Omit<RequestInit, 'body' | 'method'>;
    #serverUrl;

    constructor(requestOptions: Omit<RequestInit, 'body' | 'method'>, serverUrl: string) {
        this.#requestOptions = requestOptions;
        this.#serverUrl = serverUrl;
    }

    getCollection(contentType: string) {
        return new GetCollection(this.#requestOptions, this.#serverUrl, contentType);
    }
}
