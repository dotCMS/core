import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { transformPage, DotAppHttpRequestParams } from '../utils';

export class DotAppPage extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    get(params: DotAppHttpRequestParams): Promise<{[key: string]: string} | string> {
        return this.request(params)
            .then((data: Response) => (data.ok ? data.json() : data))
            .then((data) => {
                if (data.entity) {
                    return data.entity.layout ? transformPage(data.entity) : {};
                }
                throw new Error(data.status);
            })
            .catch((err: Error) => {
                return {
                    error: err.message
                };
            });
    }
}
