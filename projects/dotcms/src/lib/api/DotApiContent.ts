import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSContent, DotCMSError } from '../models';

/**
 * Save/Publish the information of DotCMS Content
 *
 */
export class DotApiContent {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    save<Content extends DotCMSContent>(params: Content): Promise<Response> {
        return this.doRequest('/api/content/save/1', params);
    }

    publish<Content extends DotCMSContent>(params: Content): Promise<Response> {
        return this.doRequest('/api/content/publish/1', params);
    }

    private async doRequest<Content extends DotCMSContent>(
        url: string,
        params: Content
    ): Promise<Response> {
        const response = await this.dotCMSHttpClient.request({
            url,
            method: 'POST',
            body: JSON.stringify(params)
        });

        if (response.status !== 200) {
            throw <DotCMSError>{
                message: await response.text(),
                statusCode: response.status
            };
        }

        return response;
    }
}
