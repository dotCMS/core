import fetch from 'node-fetch';
import { DotAppConfigParams } from '../api/DotAppBase';


const getUrl = (pathname: string, config: DotAppConfigParams) => {
    const host = config.environment !== 'development' ? config.host : '';
    return `${host}/api/v1/page/json/${pathname.slice(1)}?language_id=1`;
};

export const request = (params: { [key: string]: string }, config: DotAppConfigParams) => {
    const url = getUrl(params.url, config);

    return fetch(url, {
        method: params.method || 'GET',
        headers: {
            Authorization: `Bearer ${config.token}`,
            'Content-type': 'application/json'
        },
        body: params.body
    });
};
