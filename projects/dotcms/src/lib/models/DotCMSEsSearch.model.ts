import { DotCMSContentlet } from './DotCMSPageAsset.model';

export interface DotCMSEsResult {
    contentlets: DotCMSContentlet[];
    esresponse: DotCMSEsresponse[];
}

interface DotCMSEsresponse {
    took: number;
    timed_out: boolean;
    _shards: DotCMSEsResponseShards;
    hits: DotCMSEsResponseHits;
}

interface DotCMSEsResponseHits {
    total: number;
    max_score?: any;
    hits: DotCMSEsResponseHit[];
}

interface DotCMSEsResponseHit {
    _index: string;
    _type: string;
    _id: string;
    _score?: any;
    _source: DotCMSEsResponseSource;
    sort: number[];
}

interface DotCMSEsResponseSource {
    inode: string;
    identifier: string;
}

interface DotCMSEsResponseShards {
    total: number;
    successful: number;
    skipped: number;
    failed: number;
}
