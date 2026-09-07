import { createUnsupportedBlockMark } from '@dotcms/dotcms-models';

/**
 * Registered unconditionally so a mark this schema does not declare degrades to a neutral
 * placeholder instead of aborting `Node.fromJSON` for the whole document (#37175).
 */
export const UnsupportedMark = createUnsupportedBlockMark();
