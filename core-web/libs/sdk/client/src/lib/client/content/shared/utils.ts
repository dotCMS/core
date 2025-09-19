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

/**
 * @description
 * Determines whether a site ID constraint should be added to a query based on existing constraints.
 *
 * The site ID constraint is added only when:
 * - Query doesn't already contain a positive site constraint (+conhost)
 * - Query doesn't explicitly exclude the specified site ID (-conhost:siteId)
 * - Site ID is provided and configured
 *
 * @example
 * ```ts
 * const query = '+contentType:Blog +languageId:1';
 * const siteId = '123';
 * const shouldAdd = shouldAddSiteIdConstraint(query, siteId); // true
 * ```
 *
 * @example
 * ```ts
 * const query = '+contentType:Blog -conhost:123';
 * const siteId = '123';
 * const shouldAdd = shouldAddSiteIdConstraint(query, siteId); // false (explicitly excluded)
 * ```
 *
 * @example
 * ```ts
 * const query = '+contentType:Blog +conhost:456';
 * const siteId = '123';
 * const shouldAdd = shouldAddSiteIdConstraint(query, siteId); // false (already has constraint)
 * ```
 *
 * @export
 * @param {string} query - The Lucene query string to analyze
 * @param {string | number | null | undefined} siteId - The site ID to check for
 * @returns {boolean} True if site ID constraint should be added, false otherwise
 */
export function shouldAddSiteIdConstraint(
    query: string,
    siteId: string | number | null | undefined
): boolean {
    // No site ID configured
    if (!siteId) {
        return false;
    }

    // Query already contains a positive site constraint
    const hasExistingSiteConstraint = /\+conhost/gi.test(query);
    if (hasExistingSiteConstraint) {
        return false;
    }

    // Query explicitly excludes this specific site ID
    const hasThisSiteIdExclusion = new RegExp(`-conhost:${siteId}`, 'gi').test(query);
    if (hasThisSiteIdExclusion) {
        return false;
    }

    return true;
}
