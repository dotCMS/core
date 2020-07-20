import fetch from 'node-fetch';
import { DotCMSConfigurationParams, DotAppHttpRequestParams } from '../models';

function getQueryParams(language: string, hostId: string): string {
    let result = '?';

    if (language) {
        result += `language_id=${language}`;

        if (hostId) {
            result += '&';
        }
    }

    if (hostId) {
        result += `host_id=${hostId}`;
    }

    return result;
}

async function getUrl(
    params: DotAppHttpRequestParams,
    config: DotCMSConfigurationParams
): Promise<string> {
    const host = config.host || '';
    return `${host}${params.url}${getQueryParams(params.language, config.hostId)}`;
}

function shouldAppendBody(params: DotAppHttpRequestParams): boolean {
    return params.method === 'POST' && !!params.body;
}

function getOpts(
    params: DotAppHttpRequestParams,
    config: DotCMSConfigurationParams
): { [key: string]: any } {
    let opts: { [key: string]: any } = {
        method: params.method || 'GET',
        headers: {
            Authorization: `Bearer ${config.token}`,
            'Content-type': 'application/json'
        }
    };

    if (shouldAppendBody(params)) {
        opts = {
            ...opts,
            body: params.body
        };
    }

    return opts;
}

export async function request(params: DotAppHttpRequestParams, config: DotCMSConfigurationParams) {
    const url = await getUrl(params, config);
    const opts = getOpts(params, config);
    return fetch(url, opts);
}
