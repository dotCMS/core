import { CONTENT_TYPE_MAIN_FIELDS } from './const';

/**
 * Sanitizes the query for the given content type.
 * It replaces the fields that are not contentType fields with the correct format.
 * Example: +field: -> +contentTypeVar.field:
 *
 *
 * @export
 * @param {string} query
 * @param {string} contentType
 * @return {*}  {string}
 */
export function sanitizeQueryForContentType(query: string, contentType: string): string {
    return query.replace(/\+([^+:]*?):/g, (match, field) => {
        return !CONTENT_TYPE_MAIN_FIELDS.includes(field) // Fields that are not contentType fields
            ? `+${contentType}.${field}:` // Should have this format: +contentTypeVar.field:
            : match;
    });
}
