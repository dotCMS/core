import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSConfigurationItem, DotCMSConfigurationParams } from '../models';

export class DotApiConfiguration extends DotCMSHttpClient {
    constructor(config: DotCMSConfigurationParams) {
        super(config);
    }

    get(): Promise<DotCMSConfigurationItem> {
        return this.request({
            url: '/api/v1/configuration'
        })
            .then((response: Response) => response.json())
            .then((data) => data.entity);
    }
}
