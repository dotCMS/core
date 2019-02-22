import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { DotAppNavChildren } from '../models/DotAppNav.model';

export class DotAppNav extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    get(deep = '2', location = '/'): Promise<DotAppNavChildren> {
        return this.request({
            url: `/api/v1/nav/${location}?depth=${deep}`
        })
            .then((response: Response) => response.json())
            .then((data) => data.entity);
    }
}
