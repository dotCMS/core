/**
 * Default variant identifier used in the application.
 */
export const DEFAULT_VARIANT_ID = 'DEFAULT';

/**
 * Identifier for the dotCMS System Host.
 * Used when building queries that should include content from the System Host.
 */
export const SYSTEM_HOST = 'SYSTEM_HOST';

/**
 * Fields that should not be formatted when sanitizing the query.
 * These fields are essential for maintaining the integrity of the content type.
 */
export const CONTENT_TYPE_MAIN_FIELDS: string[] = [
    'live',
    'variant',
    'contentType',
    'languageId',
    'conhost'
];

/**
 * URL endpoint for the content API search functionality.
 */
export const CONTENT_API_URL = '/api/content/_search';
