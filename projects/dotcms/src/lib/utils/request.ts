import fetch from 'node-fetch';
import { DotAppConfigParams } from '../api/DotAppBase';

export interface DotAppHttpRequestParams {
    url: string;
    method?: string;
    body?: { [key: string]: any } | string;
    language?: string;
}

async function getLangQueryParam(language: string): Promise<string> {
    return language ? `?language_id=${language}` : '';
}

async function getUrl(
    params: DotAppHttpRequestParams,
    config: DotAppConfigParams
): Promise<string> {
    const host = config.environment !== 'development' ? config.host : '';
    return `${host}${params.url}${await getLangQueryParam(params.language)}`;
}

function shouldAppendBody(params: DotAppHttpRequestParams): boolean {
    return params.method === 'POST' && !!params.body;
}

function getOpts(
    params: DotAppHttpRequestParams,
    config: DotAppConfigParams
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

export async function request(params: DotAppHttpRequestParams, config: DotAppConfigParams) {
    const url = await getUrl(params, config);
    const opts = getOpts(params, config);

    return fetch(url, opts);
}
