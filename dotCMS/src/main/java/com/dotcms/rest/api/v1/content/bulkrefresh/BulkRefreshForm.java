package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * JSON body of {@code POST /api/v1/content/_bulkrefresh}.
 * <p>
 * {@code contentletIds} carries contentlet <b>inodes</b> — the name matches {@code
 * FireBulkActionsForm} and what the Action Center already sends. No Lucene query is accepted: a
 * query resolves to an unbounded set, and combined with synchronous indexing that is a self-inflicted
 * full reindex.
 * <p>
 * Duplicates are allowed and collapsed by identifier server-side. Inodes that no longer resolve are
 * <b>not</b> a request error — they count toward {@code failedCount} rather than rejecting the batch,
 * because a selection can go stale between the click and the submit.
 *
 * @author dotCMS
 */
public class BulkRefreshForm extends Validated {

    @NotNull(message = "A non-empty list of contentlet inodes is required")
    @Size(min = 1, message = "A non-empty list of contentlet inodes is required")
    private final List<String> contentletIds;

    private final boolean includeDependencies;

    private final boolean includeItemResults;

    @JsonCreator
    public BulkRefreshForm(
            @JsonProperty("contentletIds") final List<String> contentletIds,
            @JsonProperty("includeDependencies") final Boolean includeDependencies,
            @JsonProperty("includeItemResults") final Boolean includeItemResults) {
        super();
        this.contentletIds = contentletIds;
        this.includeDependencies = Boolean.TRUE.equals(includeDependencies);
        this.includeItemResults = Boolean.TRUE.equals(includeItemResults);
        this.checkValid();
    }

    /** Contentlet inodes to reindex. Capped by {@code CONTENT_BULK_REFRESH_MAX_ITEMS}. */
    public List<String> getContentletIds() {
        return contentletIds;
    }

    /**
     * Whether related content is reindexed alongside each item. Defaults to {@code false}, which
     * <b>diverges from the single-item</b> {@code _refresh}: that one always includes dependencies,
     * but at batch size a {@code loadDeps()} fan-out per item is a different cost profile.
     */
    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    /**
     * Whether per-item records are recorded and reported. Counters are returned either way; only a
     * drill-down needs the breakdown. Set once at submit and irreversible for that job, since the
     * processor either keeps the records or does not.
     */
    public boolean isIncludeItemResults() {
        return includeItemResults;
    }

    @Override
    public String toString() {
        return "BulkRefreshForm{" +
                "contentletIds=" + (null == contentletIds ? 0 : contentletIds.size()) + " item(s)" +
                ", includeDependencies=" + includeDependencies +
                ", includeItemResults=" + includeItemResults +
                '}';
    }
}
