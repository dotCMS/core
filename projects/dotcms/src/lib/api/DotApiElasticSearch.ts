import { DotCMSError } from './../models/DotCMSError.model';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { getEsQuery } from '../utils/getEsQuery';
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
        }).then(async (response: Response) => {
            if (response.status === 200) {
                return response.json();
            }

            throw <DotCMSError>{
                message: await response.text(),
                status: response.status
            };
        });
    }
}
