import { CONTENT_TYPE_MAIN_FIELDS, SYSTEM_HOST } from './const';

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
    // Match field names that start with letter/underscore, followed by alphanumeric/underscore/dot
    // This excludes Lucene grouping operators like +(...)
    return query.replace(/\+([a-zA-Z_][a-zA-Z0-9_.]*):/g, (original, field) => {
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

/**
 * @description
 * Collects all positive `+conhost:` values from a fully assembled Lucene query,
 * removes them from their original positions, adds SYSTEM_HOST to the set,
 * and rebuilds a single grouped constraint at the end of the query.
 *
 * This function is designed to be called on the final assembled query string
 * (after raw query has been appended) so that conhosts from all sources —
 * auto-injected siteId, builder-path conhost, and raw-path conhost — are
 * all visible and handled uniformly.
 *
 * @example
 * ```ts
 * // Single siteId + SYSTEM_HOST
 * buildConhostWithSystemHost('+contentType:Blog +languageId:1 +live:true +conhost:site-123')
 * // → '+contentType:Blog +languageId:1 +live:true +(conhost:site-123 conhost:SYSTEM_HOST)'
 * ```
 *
 * @example
 * ```ts
 * // Multiple conhosts (multisite) + SYSTEM_HOST
 * buildConhostWithSystemHost('+contentType:Blog +live:true +conhost:site-a +conhost:site-b')
 * // → '+contentType:Blog +live:true +(conhost:site-a conhost:site-b conhost:SYSTEM_HOST)'
 * ```
 *
 * @example
 * ```ts
 * // No conhost in query (no siteId configured)
 * buildConhostWithSystemHost('+contentType:Blog +languageId:1 +live:true')
 * // → '+contentType:Blog +languageId:1 +live:true +conhost:SYSTEM_HOST'
 * ```
 *
 * @export
 * @param {string} query - The fully assembled Lucene query string to process.
 * @returns {string} The query with all positive conhost constraints replaced by a single grouped constraint.
 */
export function buildConhostWithSystemHost(query: string): string {
    // Collect all unique positive conhost values, excluding SYSTEM_HOST (we'll add it explicitly)
    const conhostRegex = /\+conhost:([^\s)]+)/gi;
    const values: string[] = [];
    let match: RegExpExecArray | null;

    while ((match = conhostRegex.exec(query)) !== null) {
        const value = match[1];

        if (value.toUpperCase() !== SYSTEM_HOST && !values.includes(value)) {
            values.push(value);
        }
    }

    // Always include SYSTEM_HOST at the end
    values.push(SYSTEM_HOST);

    // Remove all existing +conhost: constraints from the query
    const queryWithoutConhost = query
        .replace(/\s*\+conhost:[^\s)]+/gi, '')
        .replace(/\s+/g, ' ')
        .trim();

    // Build the final constraint:
    // - Single value (only SYSTEM_HOST, no other conhost was present): simple form
    // - Multiple values: grouped form so dotCMS treats them as OR
    const conhostConstraint =
        values.length === 1
            ? `+conhost:${values[0]}`
            : `+(${values.map((v) => `conhost:${v}`).join(' ')})`;

    return `${queryWithoutConhost} ${conhostConstraint}`.trim();
}
