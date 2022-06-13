export interface DotAppHttpRequestParams {
    url: string;
    method?: string;
    body?: { [key: string]: any } | string;
    params?: { [key: string]: string };
}
