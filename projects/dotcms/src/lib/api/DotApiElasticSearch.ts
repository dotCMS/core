import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { getEsQuery } from '../utils';
import { DotCMSElasticSearchResult, DotCMSConfigurationParams, DotCMSElasticSearchParams } from '../models';

export class DotApiElasticSearch {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
    }

    search(params: DotCMSElasticSearchParams): Promise<DotCMSElasticSearchResult> {
        return this.dotCMSHttpClient.request({
            url: '/api/es/search',
            method: 'POST',
            body: getEsQuery(params)
        });
    }
}
