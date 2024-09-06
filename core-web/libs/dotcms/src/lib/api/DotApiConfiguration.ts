import { DotCMSConfigurationItem, DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

/**
 * Get the information of DotCMS configuration
 *
 */
export class DotApiConfiguration {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    // TODO: CATCH THIS ERROR
    get(): Promise<DotCMSConfigurationItem> {
        return this.dotCMSHttpClient
            .request({
                url: '/api/v1/configuration'
            })
            .then(async (response: Response) => {
                if (response.status === 200) {
                    const data = await response.json();

                    return data.entity;
                }

                throw <DotCMSError>{
                    message: await response.text(),
                    statusCode: response.status
                };
            });
    }
}
