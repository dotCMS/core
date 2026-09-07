import { DotCMSContentlet } from '@dotcms/dotcms-models';

export interface QueryToolSearchForm {
    query: string;
    sort: string;
    limit: number;
    offset: number;
    userId?: string;
}

export interface QueryToolSearchResponse {
    resultsSize: number;
    queryTook: number;
    contentTook: number;
    jsonObjectView: {
        contentlets: DotCMSContentlet[];
    };
}

export interface QueryToolHelpExample {
    title: string;
    query: string;
    description?: string;
}

export type QueryToolActiveTab = 'results' | 'raw';
