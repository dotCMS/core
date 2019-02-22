import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { DotAppHttpRequestParams } from '../utils';
import { DotAppPageAsset } from '../models';

export class DotAppPage extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    get(params: DotAppHttpRequestParams): Promise<DotAppPageAsset> {
        params = {
            ...params,
            url: `/api/v1/page/json${params.url}`
        };

        return this.request(params)
            .then((data: Response) => (data.ok ? data.json() : data))
            .then((data) => <DotAppPageAsset>data.entity);
    }
}
