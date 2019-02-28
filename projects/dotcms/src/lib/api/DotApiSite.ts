import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSSite, DotCMSConfigurationParams } from '../models';

export class DotApiSite {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
    }

    getCurrentSite(): Promise<DotCMSSite> {
        return this.dotCMSHttpClient
            .request({
                url: `/api/v1/site/currentSite`
            })
            .then((response: Response) => response.json())
            .then((data) => {
                const { map, ...site } = data.entity;
                return <DotCMSSite>site;
            });
    }
}
