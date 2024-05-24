import { GetCollection } from './methods/get-collection/get-collection';

export class Content {
    private requestOptions: Omit<RequestInit, 'body' | 'method'>;
    private serverUrl;

    constructor(requestOptions: Omit<RequestInit, 'body' | 'method'>, serverUrl: string) {
        this.requestOptions = requestOptions;
        this.serverUrl = serverUrl;
    }

    getCollection(contentType: string) {
        return new GetCollection(this.requestOptions, this.serverUrl, contentType);
    }
}
