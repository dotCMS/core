import { CONTENT_TYPE_MAIN_FIELDS } from './const';

/**
 * @description
 * Sanitizes the query for the given content type.
 * It replaces the fields that are not content type fields with the correct format.
 * Example: +field: -> +contentTypeVar.field:
 *
 * @example
 *
 * ```ts
 * const query = '+field: value';
 * const contentType = 'contentTypeVar';
 * const sanitizedQuery = sanitizeQueryForContentType(query, contentType); // Output: '+contentTypeVar.field: value'
 * ```
 *
 * @export
 * @param {string} query - The query string to be sanitized.
 * @param {string} contentType - The content type to be used for formatting the fields.
 * @returns {string} The sanitized query string.
 */
export function sanitizeQueryForContentType(query: string, contentType: string): string {
    return query.replace(/\+([^+:]*?):/g, (original, field) => {
        return !CONTENT_TYPE_MAIN_FIELDS.includes(field) // Fields that are not content type fields
            ? `+${contentType}.${field}:` // Should have this format: +contentTypeVar.field:
            : original; // Return the field if it is a content type field
    });
}
