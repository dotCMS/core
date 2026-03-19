/**
 * Generic response structure for dotCMS API endpoints
 * @template T - The type of the entity data
 */
export interface DotCMSResponse<T = unknown> {
    entity: T;
    contentlets?: T;
    tempFiles?: T;
    errors: string[];
    i18nMessagesMap: Record<string, string>;
    messages: string[];
    pagination: unknown;
    permissions: string[];
}

/**
 * Generic response structure for dotCMS API endpoints that return bodyJsonObject
 * @template T - The type of the bodyJsonObject data
 */
export interface DotCMSResponseJsonObject<T = unknown> {
    bodyJsonObject: T;
}

export interface DotRequestOptionsArgs {
    url: string;
    body?:
        | {
              [key: string]: unknown;
          }
        | string;
    method?: string;
    params?: {
        [key: string]: unknown;
    };
    headers?: {
        [key: string]: unknown;
    };
}
