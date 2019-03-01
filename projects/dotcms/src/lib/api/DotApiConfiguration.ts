import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSConfigurationItem, DotCMSConfigurationParams, DotCMSError } from '../models';

export class DotApiConfiguration {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
    }

    get(): Promise<DotCMSConfigurationItem> {
        return this.dotCMSHttpClient.request({
            url: '/api/v1/configuration'
        })
        .then(async (response: Response) => {
            if (response.status === 200) {
                const data = await response.json();
                return data.entity;
            }

            throw <DotCMSError>{
                message: await response.text(),
                status: response.status
            };
        });
    }
}
