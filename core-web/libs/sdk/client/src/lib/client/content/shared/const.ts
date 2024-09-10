/**
 * Default variant identifier used in the application.
 */
export const DEFAULT_VARIANT_ID = 'DEFAULT';

/**
 * Fields that should not be formatted when sanitizing the query.
 * These fields are essential for maintaining the integrity of the content type.
 */
export const CONTENT_TYPE_MAIN_FIELDS: string[] = ['live', 'variant', 'contentType', 'languageId'];

/**
 * URL endpoint for the content API search functionality.
 */
export const CONTENT_API_URL = '/api/content/_search';
