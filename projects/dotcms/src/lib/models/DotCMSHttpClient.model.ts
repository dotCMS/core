export interface DotAppHttpRequestParams {
    url: string;
    method?: string;
    body?: { [key: string]: any } | string;
    language?: string;
}
