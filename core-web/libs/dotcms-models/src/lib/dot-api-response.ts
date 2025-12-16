import { DotPagination } from './dot-pagination.model';

/**
 * Generic API response structure for dotCMS endpoints.
 * @template T - Type of the entity being returned
 */
export interface DotCMSAPIResponse<T = unknown> {
    /** The main data payload */
    entity: T;
    /** Array of error messages, if any */
    errors: string[];
    /** Array of informational messages */
    messages: string[];
    /** User permissions for the entity */
    permissions: string[];
    /** Internationalization message map */
    i18nMessagesMap: { [key: string]: string };
    /** Pagination information, if applicable */
    pagination?: DotPagination;
}
