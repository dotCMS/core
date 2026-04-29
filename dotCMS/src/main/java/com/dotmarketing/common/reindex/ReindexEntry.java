package com.dotmarketing.common.reindex;

import com.dotcms.annotations.Nullable;
import java.util.Date;
import org.immutables.value.Value;

/**
 * Immutable value object representing a single entry in the distributed reindex journal
 * ({@code dist_reindex_journal}).
 *
 * <h3>Equality</h3>
 * <p>Two entries are equal when they share the same {@code identToIndex}, {@code priority},
 * {@code delete} flag, and {@code serverId} — matching the original deduplication semantics
 * of the mutable POJO. {@code id}, {@code lastResult}, and {@code timeEntered} are
 * {@link Value.Auxiliary auxiliary} and therefore excluded from {@code equals}/{@code hashCode}.</p>
 *
 * <h3>Construction</h3>
 * <pre>{@code
 * ReindexEntry entry = ImmutableReindexEntry.builder()
 *     .id(42L)
 *     .identToIndex("abc123")
 *     .priority(0)
 *     .build();   // isDelete defaults to false
 * }</pre>
 */
@Value.Immutable
public abstract class ReindexEntry {

    // ── Identity / routing ────────────────────────────────────────────────────

    /** DB row id — excluded from equals/hashCode (auxiliary). */
    @Value.Auxiliary
    public abstract long getId();

    /** Contentlet identifier to reindex. */
    public abstract String getIdentToIndex();

    /** Priority level; also encodes error-retry count (see {@link #errorCount()}). */
    public abstract int getPriority();

    /**
     * {@code true} when the document should be deleted from the index.
     * Defaults to {@code false} — failed-reindex records are never deletes.
     */
    @Value.Default
    public boolean isDelete() { return false; }

    /** Server that owns this entry; {@code null} when not assigned to a specific node. */
    @Nullable
    public abstract String getServerId();

    // ── Metadata (auxiliary — excluded from equals/hashCode) ─────────────────

    /** Result of the last indexing attempt; {@code null} on first attempt. */
    @Nullable
    @Value.Auxiliary
    public abstract String getLastResult();

    /** When this entry was created; {@code null} when populated from the in-memory queue. */
    @Nullable
    @Value.Auxiliary
    public abstract Date getTimeEntered();

    // ── Derived ───────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the priority indicates a full reindex operation
     * rather than a single-content update.
     */
    public boolean isReindex() {
        return getPriority() >= ReindexQueueFactory.Priority.REINDEX.dbValue();
    }

    /**
     * Returns the number of previous failed attempts encoded in the priority value.
     */
    public int errorCount() {
        return getPriority() % 100;
    }

    @Override
    public String toString() {
        return "IndexJournal [id=" + getId() + ", identToIndex=" + getIdentToIndex()
                + ", priority=" + getPriority() + ", delete=" + isDelete()
                + ", serverId=" + getServerId() + "]";
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    /** Returns a new builder for constructing a {@link ReindexEntry}. */
    public static ImmutableReindexEntry.Builder builder() {
        return ImmutableReindexEntry.builder();
    }

}
