import { DotCMSContentlet } from '@dotcms/dotcms-models';

export class DotContentLetService {
    static get(contentType = ''): Promise<DotCMSContentlet[]> {
        return fetch(
            `/api/content/render/false/query/+contentType:${contentType}%20+languageId:1%20+deleted:false%20+working:true/orderby/modDate%20desc`
        )
            .then((response) => response.json())
            .then((data) => data.contentlets);
    }
}
