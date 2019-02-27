import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { DotCMSConfigItem } from '../models';

export class DotAppConfig extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    get(): Promise<DotCMSConfigItem> {
        return this.request({
            url: '/api/v1/configuration'
        })
            .then((response: Response) => response.json())
            .then((data) => data.entity);
    }
}
