import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { DotAppHttpRequestParams } from '../utils';
import { DotAppPageAsset } from '../models';
import { DotAppLanguage } from './DotAppLanguage';

export class DotAppPage extends DotAppBase {
    private dotAppLanguage: DotAppLanguage;

    constructor(config: DotAppConfigParams) {
        super(config);
        this.dotAppLanguage = new DotAppLanguage(config);
    }

    async get(params: DotAppHttpRequestParams): Promise<DotAppPageAsset> {
        if (params.language) {
            params = {
                ...params,
                language: await this.dotAppLanguage.getId(params.language)
            };
        }

        params = {
            ...params,
            url: `/api/v1/page/json${params.url}`,
        };

        return this.request(params)
            .then((data: Response) => (data.ok ? data.json() : data))
            .then((data) => <DotAppPageAsset>data.entity);
    }
}
