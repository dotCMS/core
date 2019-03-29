import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSContentType, DotCMSContentTypeField, DotCMSError } from '../models';

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
                status: response.status
            };
        }

        const data = await response.json();
        return data.entity;
    }

    getFields(contentTypeId): Promise<DotCMSContentTypeField[]> {
        return this.get(contentTypeId).then((contentType: DotCMSContentType) => {
            return contentType.fields;
        });
    }
}
