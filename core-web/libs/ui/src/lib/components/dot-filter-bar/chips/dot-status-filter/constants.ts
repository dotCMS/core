/**
 * Filter key the Status control writes.
 *
 * Shared verbatim with Content Drive, whose URL encode/decode keys off this exact string. Unlike
 * `languageId` and the shared-assets toggle it is **not** seeded by default: no selection means no
 * status filtering, and the request omits the key entirely so an unfiltered surface issues exactly
 * the request it always did.
 */
export const STATUS_FILTER_KEY = 'status';

/** The three conditions the browse endpoint accepts. Selections combine with OR. */
export const CONTENT_STATUS = {
    ARCHIVED: 'ARCHIVED',
    UNPUBLISHED: 'UNPUBLISHED',
    LOCKED: 'LOCKED'
} as const;

export type DotContentStatus = (typeof CONTENT_STATUS)[keyof typeof CONTENT_STATUS];

/** Offered in this order, which is also the order the chip lists a selection in. */
export const STATUS_FILTER_OPTIONS: { value: DotContentStatus; labelKey: string }[] = [
    { value: CONTENT_STATUS.ARCHIVED, labelKey: 'content-drive.status-filter.archived' },
    { value: CONTENT_STATUS.UNPUBLISHED, labelKey: 'content-drive.status-filter.unpublished' },
    { value: CONTENT_STATUS.LOCKED, labelKey: 'content-drive.status-filter.locked' }
];

/**
 * The conditions that can coexist with a published-only query.
 *
 * Only Locked: published content may be locked, while neither Archived nor Unpublished has a
 * published version at all. Asking for one alongside `live: true` would force the platform to
 * describe the content by its working version — a version the caller did not ask for — so a surface
 * pinned to published content must neither offer nor carry the other two (FR-014d, SC-009).
 *
 * Lives beside the options rather than in a toolbar because two callers need the same rule: the
 * control that decides what to *offer*, and the seeding that decides what a caller may *pre-select*.
 */
export const PUBLISHED_ONLY_STATUSES: DotContentStatus[] = [CONTENT_STATUS.LOCKED];
