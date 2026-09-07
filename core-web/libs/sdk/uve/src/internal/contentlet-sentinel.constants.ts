/**
 * Sentinel values for the placeholder contentlet used when the UVE represents
 * an empty container (e.g. hover / selection without a real contentlet).
 *
 * @internal
 */
export const TEMP_EMPTY_CONTENTLET = 'TEMP_EMPTY_CONTENTLET' as const;

/**
 * Placeholder `contentType` for {@link TEMP_EMPTY_CONTENTLET}.
 *
 * @internal
 */
export const TEMP_EMPTY_CONTENTLET_TYPE = 'TEMP_EMPTY_CONTENTLET_TYPE' as const;
