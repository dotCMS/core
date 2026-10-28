import { DotContentState } from './dot-content-state.model';

/**
 * What the chip next to a content title shows.
 *
 * - `null`: nothing to show (the content is still loading, a historical version is being viewed, or
 *   the page is not Edit Content).
 * - `{ state: null }`: content that has not been saved yet; the badge renders "New".
 * - `{ state }`: existing content; the badge derives Published, Draft, Archived or Revision from it.
 */
export type DotTitleStatus = { state: DotContentState | null } | null;
