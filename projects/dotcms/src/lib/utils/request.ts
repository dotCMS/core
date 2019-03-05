import fetch from 'node-fetch';
import { DotCMSConfigurationParams, DotAppHttpRequestParams } from '../models';

async function getLangQueryParam(language: string): Promise<string> {
    return language ? `?language_id=${language}` : '';
}

async function getUrl(
    params: DotAppHttpRequestParams,
    config: DotCMSConfigurationParams
): Promise<string> {
    const host = config.host || '';
    return `${host}${params.url}${await getLangQueryParam(params.language)}`;
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
