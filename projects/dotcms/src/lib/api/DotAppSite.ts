import { DotAppBase, DotAppConfigParams } from './DotAppBase';

export class DotAppSite extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    getCurrentSite(): Promise<{ [key: string]: string }> {
        return this.request({
            url: `/api/v1/site/currentSite`,
            method: 'GET'
        })
            .then((response) => response.json())
            .then((data) => data.entity);
    }
}
