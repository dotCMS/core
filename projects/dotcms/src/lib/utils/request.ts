import fetch from 'node-fetch';
import { DotAppConfigParams } from '../api/DotAppBase';

export interface DotAppHttpRequestParams {
    url: string;
    method?: string;
    body?: { [key: string]: any } | string;
}

function getUrl(pathname: string, config: DotAppConfigParams): string {
    const host = config.environment !== 'development' ? config.host : '';
    return `${host}${pathname}?language_id=1`;
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

export function request(params: DotAppHttpRequestParams, config: DotAppConfigParams) {
    const url = getUrl(params.url, config);
    const opts = getOpts(params, config);

    return fetch(url, opts);
}
