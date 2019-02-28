import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSNavigationItem, DotCMSConfigurationParams } from '../models';

export class DotApiNavigation {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
    }

    get(deep = '2', location = '/'): Promise<DotCMSNavigationItem> {
        return this.dotCMSHttpClient
            .request({
                url: `/api/v1/nav/${location}?depth=${deep}`
            })
            .then((response: Response) => response.json())
            .then((data) => data.entity);
    }
}
