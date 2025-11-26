import { DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

/**
 * DotCMS {@link https://dotcms.com/docs/latest/widgets | widgets handler}
 */
export class DotApiWidget {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    /**
     * Get the widght HTML strong with it identifier
     *
     */
    getHtml(widgetId: string): Promise<string> {
        return this.dotCMSHttpClient
            .request({
                url: `/api/widget/id/${widgetId}`
            })
            .then(async (response: Response) => {
                if (response.status === 200) {
                    return response.text();
                }

                throw <DotCMSError>{
                    message: await response.text(),
                    statusCode: response.status
                };
            });
    }
}
