package com.dotcms.content.index.migration;

import java.util.Collections;
import java.util.List;

/**
 * Content-index half of the migration-readiness report (issue #36360): compares the versioned
 * content indices (working/live) against their {@code .os} twins across both engines, mirroring
 * {@link SiteSearchMirrorReconciler} but for the content store.
 *
 * <p><strong>Work in progress (PR3, step b).</strong> The framework and the Site Search half are in
 * place; this half is stubbed to return no rows so {@link MigrationReadinessService} composes both
 * sections without special-casing. It will enumerate working/live from {@code IndiciesInfo} and read
 * <em>exact</em> per-engine document counts (index stats, not a capped search total) for each twin —
 * the same drift/missing-twin verdicts as the Site Search half, emitted as
 * {@link MirrorStatus.IndexKind#CONTENT_WORKING} / {@link MirrorStatus.IndexKind#CONTENT_LIVE}.</p>
 */
public class ContentIndexMirrorReconciler {

    /** Per-index mirror status for the working/live content indices. TODO(#36360 PR3 step b). */
    public List<MirrorStatus> statuses() {
        return Collections.emptyList();
    }
}
