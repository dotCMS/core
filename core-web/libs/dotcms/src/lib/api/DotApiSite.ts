import { DotCMSSite, DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

/**
 * Get information from {@link https://dotcms.com/docs/latest/multi-site-management | DotCMS Sites}
 */
export class DotApiSite {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    getCurrentSite(): Promise<DotCMSSite> {
        return this.dotCMSHttpClient
            .request({
                url: `/api/v1/site/currentSite`
            })
            .then(async (response: Response) => {
                if (response.status === 200) {
                    const data = await response.json();
                    const { map, ...site } = data.entity;

                    return <DotCMSSite>site;
                }

                throw <DotCMSError>{
                    message: await response.text(),
                    statusCode: response.status
                };
            });
    }
}
