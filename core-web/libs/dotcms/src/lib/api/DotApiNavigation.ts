import { DotCMSNavigationItem, DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

/**
 * Retrieve information about the dotCMS file and folder tree with the  {@link https://dotcms.com/docs/latest/navigation-rest-api | Navigation REST API }
 *
 */
export class DotApiNavigation {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    get(depth = '2', location = '/'): Promise<DotCMSNavigationItem> {
        return this.dotCMSHttpClient
            .request({
                url: `/api/v1/nav/${location}`,
                params: {
                    depth
                }
            })
            .then(async (res: Response) => {
                if (res.status === 200) {
                    const data = await res.json();

                    return <DotCMSNavigationItem>data.entity;
                }

                throw <DotCMSError>{
                    message: await res.text(),
                    statusCode: res.status
                };
            });
    }
}
