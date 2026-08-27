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
 * default ({@code appendExcludeArchivedQuery}), applied unless something explicitly lifts it —
 * {@link BrowserQuery#showArchived}, an archive-target workflow step, or {@link #ARCHIVED} below. The selected
 * statuses are OR'd into one group and that group is AND'd against the baseline; {@link #ARCHIVED}
 * is the only status that lifts it. Folding the baseline into the group would make
 * {@code [UNPUBLISHED, LOCKED]} read {@code (deleted = false or ...)}, which matches nearly every
 * row.
 * <p>
 * Distinct from {@link BrowserQuery#showArchived}, which is <i>inclusive</i> (archived content
 * <i>plus</i> everything else) and is relied on by the legacy Site Browser. That flag is unchanged;
 * {@link #ARCHIVED} is the exclusive variant added alongside it.
 * <p>
 * <b>Why not {@link com.dotmarketing.portlets.workflows.model.WorkflowState}?</b> It carries the
 * same three names ({@code LOCKED}, {@code UNPUBLISHED}, {@code ARCHIVED}) and the question comes
 * up on sight, but it is a different concept: it is the {@code show_on} vocabulary deciding whether
 * a workflow <i>action</i> renders, so it also carries {@code LISTING} and {@code EDITING}, which
 * are view contexts rather than states a contentlet can be in and have nothing to resolve against
 * in a query. Its {@code toSet} also swallows an unparseable value and returns an <i>empty</i> set,
 * dropping the whole filter and returning a <i>wider</i> result than asked for — the opposite of
 * the 400 this filter contracts for. Reusing it would additionally tie the public
 * {@code /v1/drive/search} input vocabulary to workflow internals, so a new {@code show_on} value
 * would silently widen the API. Same words, different job.
 *
 * @see BrowserQuery.Builder#withContentStatuses(java.util.Set)
 * @since 26.08
 */
public enum ContentStatus {

    /** Archived (soft-deleted but recoverable): {@code cvi.deleted = true}, indexed as {@code deleted:true}. */
    ARCHIVED,

    /**
     * No live version exists: {@code cvi.live_inode is null}.
     * <p>
     * <b>The index term {@code live:false} is not the same predicate.</b> The SQL condition is
     * identifier-scoped ("this content has no live version at all"), while {@code live} is indexed
     * per VERSION ({@code ESMappingAPIImpl}), so the working document of a published-but-edited
     * item carries {@code live:false} and would match. The two paths therefore differ for that
     * case; see {@code ContentDriveStatusFilterTest#publishedThenEditedItem}. This enum's
     * definition is the identifier-scoped one.
     */
    UNPUBLISHED,

    /** A lock is held, by anyone: {@code cvi.locked_by is not null}, indexed as {@code locked:true}. */
    LOCKED
}
