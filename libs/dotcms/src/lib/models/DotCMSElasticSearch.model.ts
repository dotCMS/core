import { DotCMSContentlet } from './DotCMSPage.model';

export interface DotCMSElasticSearchParams {
    contentType: string;
    queryParams: {
        languageId: string;
        sortResultsBy: string;
        sortOrder1: string;
        offset: string;
        pagination: string;
        itemsPerPage: string;
        numberOfResults: string;
        detailedSearchQuery: string;
    };
}

export interface DotCMSElasticSearchResult {
    contentlets: DotCMSContentlet[];
    esresponse: DotCMSElasticSearchresponse[];
}

interface DotCMSElasticSearchresponse {
    took: number;
    timed_out: boolean;
    _shards: DotCMSElasticSearchResponseShards;
    hits: DotCMSElasticSearchResponseHits;
}

interface DotCMSElasticSearchResponseHits {
    total: number;
    max_score?: any;
    hits: DotCMSElasticSearchResponseHit[];
}

interface DotCMSElasticSearchResponseHit {
    _index: string;
    _type: string;
    _id: string;
    _score?: any;
    _source: DotCMSElasticSearchResponseSource;
    sort: number[];
}

interface DotCMSElasticSearchResponseSource {
    inode: string;
    identifier: string;
}

interface DotCMSElasticSearchResponseShards {
    total: number;
    successful: number;
    skipped: number;
    failed: number;
}
