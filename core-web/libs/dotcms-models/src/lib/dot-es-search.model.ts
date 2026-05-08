export interface ESSearchParams {
    live?: boolean;
    userid?: string;
}

export interface ESHit {
    _index: string;
    /** @deprecated Removed in Elasticsearch 8; may be absent in responses from ES 8+ clusters. */
    _type: string;
    _id: string;
    _score: number;
    _source: Record<string, unknown>;
}

export interface ESHitsTotal {
    value: number;
    relation: string;
}

export interface ESHitsContainer {
    total: number | ESHitsTotal;
    hits: ESHit[];
}

export interface ESShards {
    total: number;
    successful: number;
    skipped: number;
    failed: number;
}

export interface ESSearchEsResponse {
    hits: ESHitsContainer;
    took: number;
    timed_out: boolean;
    aggregations?: Record<string, unknown>;
    suggest?: Record<string, unknown>;
    scroll_id?: string;
}

export interface ESSearchResponse {
    contentlets: Record<string, unknown>[];
    esresponse: [ESSearchEsResponse];
}

export interface RawESSearchResponse {
    hits: ESHitsContainer;
    took: number;
    timed_out: boolean;
    aggregations?: Record<string, unknown>;
    suggest?: Record<string, unknown>;
    _shards: ESShards;
}
