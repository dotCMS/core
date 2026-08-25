package com.dotcms.browser;

/**
 * A content state the Content Drive Status filter can narrow by.
 * <p>
 * The three are independent boolean facts about the same {@code contentlet_version_info} row, so a
 * single contentlet may hold several at once. Selected statuses combine with <b>OR</b> — the filter
 * asks whether an item is in <i>any</i> selected state, not all of them — matching the Content Type
 * and Language filters beside it in the toolbar. Adding a status therefore never shrinks the result
 * set.
 * <p>
 * <b>Excluding archived content is not a member of this set.</b> It is the browse query's standing
 * default ({@code appendExcludeArchivedQuery}), applied on every request today. The selected
 * statuses are OR'd into one group and that group is AND'd against the baseline; {@link #ARCHIVED}
 * is the only status that lifts it. Folding the baseline into the group would make
 * {@code [UNPUBLISHED, LOCKED]} read {@code (deleted = false or ...)}, which matches nearly every
 * row.
 * <p>
 * Distinct from {@link BrowserQuery#showArchived}, which is <i>inclusive</i> (archived content
 * <i>plus</i> everything else) and is relied on by the legacy Site Browser. That flag is unchanged;
 * {@link #ARCHIVED} is the exclusive variant added alongside it.
 *
 * @see BrowserQuery.Builder#withContentStatuses(java.util.Set)
 * @since 26.08
 */
public enum ContentStatus {

    /** Archived (soft-deleted but recoverable): {@code cvi.deleted = true}, indexed as {@code deleted:true}. */
    ARCHIVED,

    /** No live version exists: {@code cvi.live_inode is null}, indexed as {@code live:false}. */
    UNPUBLISHED,

    /** A lock is held, by anyone: {@code cvi.locked_by is not null}, indexed as {@code locked:true}. */
    LOCKED
}
