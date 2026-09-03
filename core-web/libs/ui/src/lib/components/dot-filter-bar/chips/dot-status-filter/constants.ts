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
