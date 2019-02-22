import { request } from '../utils';

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

    request(params: { [key: string]: string }): Promise<any> {
        return request(params, this.config);
    }
}
