import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotAppHttpRequestParams } from '../utils';
import { DotCMSConfigurationParams, DotCMSPageAsset } from '../models';
import { DotApiLanguage } from './DotApiLanguage';

export class DotApiPage {
    private dotAppLanguage: DotApiLanguage;
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
        this.dotAppLanguage = new DotApiLanguage(config);
    }

    async get(params: DotAppHttpRequestParams): Promise<DotCMSPageAsset> {
        if (params.language) {
            params = {
                ...params,
                language: await this.dotAppLanguage.getId(params.language)
            };
        }

        params = {
            ...params,
            url: `/api/v1/page/json${params.url}`
        };

        return this.dotCMSHttpClient
            .request(params)
            .then((data: Response) => (data.ok ? data.json() : data))
            .then((data) => <DotCMSPageAsset>data.entity);
    }
}
