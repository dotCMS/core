export interface DotHttpRequestOptions {
    method: string;
    headers: { [key: string]: string };
    body: XMLHttpRequestBodyInit | null;
}
