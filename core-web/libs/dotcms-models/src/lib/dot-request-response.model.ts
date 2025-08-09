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
