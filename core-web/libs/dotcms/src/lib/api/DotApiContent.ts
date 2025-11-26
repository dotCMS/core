import { DotCMSContent, DotCMSContentQuery, DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

function populateQueryUrl(params: DotCMSContentQuery): string {
    let url = '';

    const attrs = {
        depth: `/depth/${params.options.depth}`,
        limit: `/limit/${params.options.limit}`,
        offset: `/offset/${params.options.offset}`,
        orderBy: `/orderby/${params.options.orderBy}`
    };

    if (params.queryParams) {
        for (const key of Object.keys(params.queryParams)) {
            url += `+${key}:${params.queryParams[key]}%20`;
        }
    }

    for (const key of Object.keys(params.options)) {
        url += attrs[key];
    }

    return url;
}

/**
 * Query/Save/Publish the information of DotCMS Content
 *
 */
export class DotApiContent {
    private dotCMSHttpClient: DotCMSHttpClient;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
    }

    query(params: DotCMSContentQuery): Promise<Response> {
        const url = `/api/content/query/+contentType:${params.contentType}%20${populateQueryUrl(
            params
        )}`;

        return this.doRequest(url, null, 'GET');
    }

    save<Content extends DotCMSContent>(params: Content): Promise<Response> {
        return this.doRequest('/api/content/save/1', params);
    }

    publish<Content extends DotCMSContent>(params: Content): Promise<Response> {
        return this.doRequest('/api/content/publish/1', params);
    }

    private async doRequest<Content extends DotCMSContent>(
        url: string,
        params?: Content,
        httpMethod = 'POST'
    ): Promise<Response> {
        const response = await this.dotCMSHttpClient.request({
            url,
            method: httpMethod,
            body: params ? JSON.stringify(params) : ''
        });

        if (response.status !== 200) {
            throw <DotCMSError>{
                message: await response.text(),
                statusCode: response.status
            };
        }

        return response;
    }
}
