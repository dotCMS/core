import { request, DotAppHttpRequestParams } from '../utils';

export interface DotAppConfigParams {
    token: string;
    host: string;
    environment: string;
}

export class DotAppBase {
    private _config: DotAppConfigParams;

    constructor(config: DotAppConfigParams) {
        this._config = config;
    }

    get config() {
        return this._config;
    }

    /**
     * Global HTTP request to DotCMS instance
     *
     * @param {DotAppHttpRequestParams} params
     * @returns {Promise<any>}
     * @memberof DotAppBase
     */
    request(params: DotAppHttpRequestParams): Promise<any> {
        return request(params, this.config);
    }
}
