import { DotCMSContentTypeLayoutRow, DotCMSContentType } from 'dotcms-models';

import { DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

/**
 * Get the information of DotCMS contentTypes
 *
 */
export class DotApiContentType {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    async get(contentTypeId): Promise<DotCMSContentType> {
        const response = await this.dotCMSHttpClient.request({
            url: `/api/v1/contenttype/id/${contentTypeId}`
        });

        if (response.status !== 200) {
            throw <DotCMSError>{
                message: await response.text(),
                statusCode: response.status
            };
        }

        const data = await response.json();

        return data.entity;
    }

    getLayout(contentTypeId): Promise<DotCMSContentTypeLayoutRow[]> {
        return this.get(contentTypeId).then((contentType: DotCMSContentType) => {
            return contentType.layout;
        });
    }
}
