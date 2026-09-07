import { createUnsupportedBlockMark } from '@dotcms/dotcms-models';

/**
 * Mark-side half of `UnsupportedBlockNode`: preserves a mark this schema does not declare
 * so it round-trips instead of aborting `Node.fromJSON` for the whole document (#37175).
 */
export const UnsupportedBlockMark = createUnsupportedBlockMark();
