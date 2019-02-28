import { request, DotAppHttpRequestParams } from '.';
import { DotCMSConfigurationParams } from '../models';

export class DotCMSHttpClient {
    private _config: DotCMSConfigurationParams;

    constructor(config: DotCMSConfigurationParams) {
        this._config = config;
    }

    request(params: DotAppHttpRequestParams): Promise<any> {
        return request(params, this._config);
    }
}
