import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSConfigurationParams } from '../models';

export class DotApiWidget {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
    }

    getHtml(widgetId: string): Promise<string> {
        return this.dotCMSHttpClient
            .request({
                url: `/api/widget/id/${widgetId}`
            })
            .then((response) => response.text());
    }
}
