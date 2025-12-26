/**
 * Generic response structure for dotCMS API endpoints
 * @template T - The type of the entity data
 */
export interface DotCMSResponse<T = unknown> {
    entity: T;
    errors: string[];
    i18nMessagesMap: Record<string, unknown>;
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
