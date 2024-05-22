import { GetCollection } from './methods/get-collection';

import { ClientConfig } from '../sdk-js-client';

export class Content {
    #clientConfig: ClientConfig;

    constructor(clientConfig: ClientConfig) {
        this.#clientConfig = clientConfig;
    }

    getCollection(contentType: string) {
        return new GetCollection(this.#clientConfig, contentType);
    }
}
