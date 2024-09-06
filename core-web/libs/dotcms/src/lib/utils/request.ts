import fetch from 'cross-fetch';

import { DotCMSConfigurationParams, DotAppHttpRequestParams } from '../models';

async function getUrl(
    { params, url }: DotAppHttpRequestParams,
    { host, hostId }: DotCMSConfigurationParams
): Promise<string> {
    if (!host) {
        throw new Error('Please pass the DotCMS instance in the initDotCMS initialization');
    }

    const newUrl = new URL(`${host}${url}`);
    const paramsKeys = params ? Object.keys(params) : [];

    if (paramsKeys.length) {
        paramsKeys.map((key: string) => {
            newUrl.searchParams.append(key, params[key]);
        });
    }

    if (hostId) {
        newUrl.searchParams.append('host_id', hostId);
    }

    return `${newUrl.toString()}`;
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

export async function request(options: DotAppHttpRequestParams, config: DotCMSConfigurationParams) {
    const url = await getUrl(options, config);
    const opts = getOpts(options, config);

    return fetch(url, opts);
}
