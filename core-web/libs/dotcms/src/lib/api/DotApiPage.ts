import { DotApiLanguage } from './DotApiLanguage';

import { DotCMSPageAsset, DotCMSError, DotCMSPageFormat } from '../models';
import { DotCMSPageParams } from '../models/DotCMSPage.model';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

/**
 * Allow easy interaction with {@link https://dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas | DotCMS Page API}
 *
 */
export class DotApiPage {
    private dotAppLanguage: DotApiLanguage;
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient, appLanguage: DotApiLanguage) {
        this.dotCMSHttpClient = httpClient;
        this.dotAppLanguage = appLanguage;
    }

    /**
     * Given the page url and the language return a {@link DotCMSPageAsset}
     *
     */
    async get(
        params: DotCMSPageParams,
        format: DotCMSPageFormat = DotCMSPageFormat.JSON
    ): Promise<DotCMSPageAsset> {
        if (params.language) {
            params = {
                ...params,
                language: isNaN(params.language as any)
                    ? await this.dotAppLanguage.getId(params.language)
                    : params.language
            };
        }

        params = {
            ...params,
            url: `/api/v1/page/${format}${params.url}`
        };

        return this.dotCMSHttpClient.request(params).then(async (res: Response) => {
            if (res.status === 200) {
                const data = await res.json();

                return <DotCMSPageAsset>data.entity;
            }

            const response = await res.text();
            let error: DotCMSError;
            try {
                error = {
                    statusCode: res.status,
                    message: JSON.parse(response).message
                };
            } catch {
                error = {
                    statusCode: res.status,
                    message: response
                };
            }

            throw error;
        });
    }
}
