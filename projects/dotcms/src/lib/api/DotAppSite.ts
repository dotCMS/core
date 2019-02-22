import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { DotCMSSite } from '../models';

export class DotAppSite extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    getCurrentSite(): Promise<DotCMSSite> {
        return this.request({
            url: `/api/v1/site/currentSite`,
            method: 'GET'
        })
            .then((response: Response) => response.json())
            .then((data) => {
                const { map, ...site } = data.entity;
                return <DotCMSSite>site;
            });
    }
}
