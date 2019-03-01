import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSConfigurationParams, DotCMSError } from '../models';

export class DotApiWidget {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
    }

    getHtml(widgetId: string): Promise<string> {
        return this.dotCMSHttpClient
            .request({
                url: `/api/widget/id/${widgetId}`
            }).then(async (response: Response) => {
                if (response.status === 200) {

                    return response.text();
                }

                throw <DotCMSError>{
                    message: await response.text(),
                    status: response.status
                };
            });
    }
}
