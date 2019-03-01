import { DotCMSPageParams } from './../models/DotCMSPage.model';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { DotCMSConfigurationParams, DotCMSPageAsset, DotCMSError } from '../models';
import { DotApiLanguage } from './DotApiLanguage';

/**
 * Allow easy interaction with {@link https://dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas | DotCMS Page Api
 *
 * @export
 * @class DotApiPage
 */
export class DotApiPage {
    private dotAppLanguage: DotApiLanguage;
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(config: DotCMSConfigurationParams) {
        this.dotCMSHttpClient = new DotCMSHttpClient(config);
        this.dotAppLanguage = new DotApiLanguage(config);
    }

    /**
     * Given the page url and the language return a {@link DotCMSPageAsset}
     *
     */
    async get(params: DotCMSPageParams): Promise<DotCMSPageAsset> {
        if (params.language) {
            params = {
                ...params,
                language: await this.dotAppLanguage.getId(params.language)
            };
        }

        params = {
            ...params,
            url: `/api/v1/page/json${params.url}`
        };

        return this.dotCMSHttpClient
            .request(params)
            .then(async (res: Response) => {
                if (res.status === 200) {
                    const data = await res.json();
                    return <DotCMSPageAsset>data.entity;
                }

                throw <DotCMSError>{
                    message: await res.text(),
                    status: res.status
                };
            });
    }
}
