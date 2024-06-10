import { DotCMSElasticSearchResult, DotCMSElasticSearchParams } from '../models';
import { DotCMSError } from '../models/DotCMSError.model';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { getEsQuery } from '../utils/getEsQuery';

/**
 * Request content from DotCMS using the {@link https://dotcms.com/docs/latest/elasticsearch-rest-api | Elastic Search API}
 *
 */
export class DotApiElasticSearch {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    /**
     * Provide the content type and the elastic search query and get the results contentlets and elastic search information
     *
     */
    search(params: DotCMSElasticSearchParams): Promise<DotCMSElasticSearchResult> {
        return this.dotCMSHttpClient
            .request({
                url: '/api/es/search',
                method: 'POST',
                body: getEsQuery(params)
            })
            .then(async (response: Response) => {
                if (response.status === 200) {
                    return response.json();
                }

                throw <DotCMSError>{
                    message: await response.text(),
                    statusCode: response.status
                };
            });
    }
}
