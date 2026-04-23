export interface ESSearchParams {
    live?: boolean;
    userid?: string;
}

export interface ESHit {
    _index: string;
    _type: string;
    _id: string;
    _score: number;
    _source: Record<string, unknown>;
}

export interface ESSearchEsResponse {
    hits: { total: number | { value: number; relation: string }; hits: ESHit[] };
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
    hits: {
        total: number | { value: number; relation: string };
        hits: ESHit[];
    };
    took: number;
    timed_out: boolean;
    aggregations?: Record<string, unknown>;
    suggest?: Record<string, unknown>;
    _shards: { total: number; successful: number; skipped: number; failed: number };
}
