import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSNavigationItem, DotCMSConfigurationParams, DotCMSError } from '../models';

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
            .then(async (res: Response) => {
                if (res.status === 200) {
                    const data = await res.json();
                    return <DotCMSNavigationItem>data.entity;
                }

                throw <DotCMSError>{
                    message: await res.text(),
                    status: res.status
                };
            });
    }
}
