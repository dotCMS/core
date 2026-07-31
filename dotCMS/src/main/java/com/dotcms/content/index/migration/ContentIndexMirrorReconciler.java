package com.dotcms.content.index.migration;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Content-index half of the migration-readiness report (issue #36360): compares the active versioned
 * content indices (working and live) against their {@code .os} counterparts across both engines,
 * mirroring {@link SiteSearchMirrorReconciler} but for the content store. Never mutates anything.
 *
 * <h4>How the counts are read (phase-independently)</h4>
 * <p>{@code IndiciesInfo} always holds the cluster-prefixed, <em>un-tagged</em> Elasticsearch name for
 * working/live (its backing {@code indicies} table owns only the ES rows — {@code index_version IS
 * NULL}); the OpenSearch counterpart is that name with the {@code .os} tag. Exact per-engine document
 * counts come from each engine leaf's {@code getIndicesStats()} — the index {@code _stats}
 * {@code primaries.docs.count}, an exact total not subject to the 10,000 search hit-count cap. Those
 * stats maps are keyed by the <em>cluster-stripped</em> name (Elasticsearch un-tagged, OpenSearch
 * carrying {@code .os}), so each raw name is stripped of the cluster prefix and then, for the
 * OpenSearch lookup, tagged — the same strip-then-tag order the maintenance JSP uses.</p>
 *
 * <p>It queries the two engine leaves directly (never the phase-aware router) so the report shows both
 * sides regardless of which engine the current phase reads from. Scope is the active working/live
 * pair; reindex slots are out of scope for this report.</p>
 */
public class ContentIndexMirrorReconciler {

    private final IndexAPI esImpl;
    private final IndexAPI osImpl;
    private final Supplier<IndiciesInfo> indiciesSupplier;

    public ContentIndexMirrorReconciler() {
        this(new ESIndexAPI(), CDIUtils.getBeanThrows(OSIndexAPIImpl.class),
                ContentIndexMirrorReconciler::loadIndiciesQuietly);
    }

    @VisibleForTesting
    ContentIndexMirrorReconciler(final IndexAPI esImpl, final IndexAPI osImpl,
            final Supplier<IndiciesInfo> indiciesSupplier) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
        this.indiciesSupplier = indiciesSupplier;
    }

    /** Per-index mirror status for the active working and live content indices. */
    public List<MirrorStatus> statuses() {
        final IndiciesInfo info = indiciesSupplier.get();
        if (info == null) {
            return List.of();
        }
        final Map<String, IndexStats> esStats = esImpl.getIndicesStats();
        final Map<String, IndexStats> osStats = osImpl.getIndicesStats();
        final List<MirrorStatus> out = new ArrayList<>(2);
        addStatus(out, IndexKind.CONTENT_WORKING, info.getWorking(), esStats, osStats);
        addStatus(out, IndexKind.CONTENT_LIVE, info.getLive(), esStats, osStats);
        return out;
    }

    private void addStatus(final List<MirrorStatus> out, final IndexKind kind, final String rawName,
            final Map<String, IndexStats> esStats, final Map<String, IndexStats> osStats) {
        if (!UtilMethods.isSet(rawName)) {
            return;
        }
        // IndiciesInfo holds the cluster-prefixed, un-tagged ES name — which IS the full ES physical
        // name; the OS physical name is that + .os. The stats maps are keyed by the cluster-stripped
        // name (ES un-tagged, OS carrying .os), so strip for the count lookup, tag for the OS key.
        final String esPhysical = rawName;
        final String osPhysical = IndexTag.OS.tag(rawName);
        final String bare = esImpl.removeClusterIdFromName(rawName);
        final String osKey = IndexTag.OS.tag(bare);

        final boolean esExists = esStats.containsKey(bare);
        final long esCount = esExists ? esStats.get(bare).documentCount() : 0L;
        final boolean osExists = osStats.containsKey(osKey);
        final long osCount = osExists ? osStats.get(osKey).documentCount() : 0L;

        final Verdict verdict = MirrorStatus.verdictFor(esExists, osExists, esCount, osCount);
        out.add(new MirrorStatus(bare, kind,
                new MirrorStatus.EngineCopy(esExists, esCount, esPhysical),
                new MirrorStatus.EngineCopy(osExists, osCount, osPhysical),
                verdict, recommend(bare, verdict, osExists)));
    }

    private static String recommend(final String name, final Verdict verdict, final boolean osExists) {
        switch (verdict) {
            case IN_SYNC:
                return "In sync — no action needed.";
            case MISSING_COUNTERPART:
                final String missing = osExists ? "Elasticsearch" : "OpenSearch";
                return String.format("The %s copy of content index '%s' is missing. Run a full "
                        + "reindex to rebuild it before promoting to the OpenSearch-only phase.",
                        missing, name);
            case COUNT_DRIFT:
            default:
                return String.format("The two copies of content index '%s' hold a different number "
                        + "of documents. Run a full reindex to rebuild the OpenSearch copy before "
                        + "promoting the phase.", name);
        }
    }

    private static IndiciesInfo loadIndiciesQuietly() {
        return Try.of(() -> APILocator.getIndiciesAPI().loadIndicies())
                .onFailure(e -> Logger.warn(ContentIndexMirrorReconciler.class,
                        "Could not load content indices for migration readiness: " + e.getMessage()))
                .getOrNull();
    }
}
