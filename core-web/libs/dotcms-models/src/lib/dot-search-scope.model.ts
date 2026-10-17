/**
 * Which fields a search term is matched against.
 *
 * Not to be confused with a *browse* scope, which says where you are browsing. A search scope says
 * how a search reads what is there.
 *
 * `as const` rather than a TS `enum`, per TYPESCRIPT_STANDARDS.md. The values are the wire contract
 * and must match the server's `SearchScope`.
 */
export const DOT_SEARCH_SCOPE = {
    /** The term is matched against every indexed field. The server's default. */
    ALL_FIELDS: 'ALL_FIELDS',
    /** The term is matched against the contentlet title only. */
    TITLE: 'TITLE'
} as const;

export type DotSearchScope = (typeof DOT_SEARCH_SCOPE)[keyof typeof DOT_SEARCH_SCOPE];

/**
 * The key the search scope travels under in a surface's filter state (and, in Content Drive, in the
 * address). Stored only when it differs from Title, so a fresh opening and "Clear all" land on Title
 * without a rule of their own. Content Drive and the AssetPicker keep it under the same key.
 */
export const SEARCH_SCOPE_FILTER_KEY = 'searchScope';
