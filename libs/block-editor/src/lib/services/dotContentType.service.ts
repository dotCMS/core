import { DotCMSContentlet } from '@dotcms/dotcms-models';

export class DotContentTypeService {
    static get(filter = ''): Promise<DotCMSContentlet[]> {
        return fetch(
            `/api/v1/contenttype?filter=${filter}&orderby=modDate&direction=DESC&per_page=40`
        )
            .then((response) => response.json())
            .then((data) => data.entity);
    }
}
