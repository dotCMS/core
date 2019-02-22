import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { getEsQuery } from '../utils';

export interface DotEsSearchParams {
    contentType: string;
    queryParams: {
        languageId: string;
        sortResultsBy: string;
        sortOrder1: string;
        offset: string;
        pagination: string;
        itemsPerPage: string;
        numberOfResults: string;
        detailedSearchQuery: string;
    };
}

export class DotAppEs extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    search(params: DotEsSearchParams): Promise<any> {
        return this.request({
            url: '/api/es/search',
            method: 'POST',
            body: getEsQuery(params)
        });
    }
}
