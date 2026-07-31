package com.dotcms.content.index.migration;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotcms.enterprise.publishing.sitesearch.ESSiteSearchAPI;
import com.dotcms.enterprise.publishing.sitesearch.OSSiteSearchAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Site Search half of the migration-readiness report (issue #36360): compares every logical
 * site-search index against its {@code .os} counterpart across both engines and produces a factual
 * {@link MirrorStatus} per index — does the ES copy exist, does the OpenSearch counterpart exist, do their
 * exact document counts match — plus a re-crawl recommendation. Never mutates anything.
 *
 * <p>It queries the two engine leaves directly (ES the plain name, OpenSearch the {@code .os} counterpart)
 * rather than the phase-aware router, so the report shows <em>both</em> sides regardless of which
 * engine the current phase reads from. Counts come from {@link SiteSearchAPI#documentCount(String)}
 * — an exact total, not a search hit-count capped at 10,000 — so content drift on large indices is
 * detected (issue #36360).</p>
 */
public class SiteSearchMirrorReconciler {

    private final SiteSearchAPI esImpl;
    private final SiteSearchAPI osImpl;
    private final Supplier<String> clusterPrefixSupplier;

    public SiteSearchMirrorReconciler() {
        this(new ESSiteSearchAPI(), CDIUtils.getBeanThrows(OSSiteSearchAPI.class),
                () -> APILocator.getESIndexAPI().getClusterPrefix());
    }

    @VisibleForTesting
    SiteSearchMirrorReconciler(final SiteSearchAPI esImpl, final SiteSearchAPI osImpl,
            final Supplier<String> clusterPrefixSupplier) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
        this.clusterPrefixSupplier = clusterPrefixSupplier;
    }

    /**
     * Whether a cross-engine mirror comparison is meaningful for a forward phase change in the
     * current phase. Only the dual-write phases (1 and 2) keep both engines populated as write
     * providers; Phase 0 (ES only) and Phase 3 (OS only) have a single write engine, so a "missing
     * counterpart" there is either expected (0) or unfixable in-phase (3).
     */
    public boolean canEvaluate() {
        return IndexConfigHelper.MigrationPhase.current().isDualWrite();
    }

    /**
     * The per-index mirror status for every logical site-search index that exists on <em>either</em>
     * engine (so a counterpart missing on one side still appears). Purely factual and phase-independent.
     */
    public List<MirrorStatus> statuses() {
        final TreeSet<String> names = new TreeSet<>(esImpl.listIndices());
        names.addAll(osImpl.listIndices());
        final List<MirrorStatus> statuses = new ArrayList<>(names.size());
        for (final String name : names) {
            statuses.add(statusFor(name));
        }
        return statuses;
    }

    private MirrorStatus statusFor(final String name) {
        final boolean esExists = esImpl.existsOnAllWriteEngines(name);
        final boolean osExists = osImpl.existsOnAllWriteEngines(name);
        final long esCount = esExists ? esImpl.documentCount(name) : 0L;
        final long osCount = osExists ? osImpl.documentCount(name) : 0L;
        // Physical names as stored: ES is the cluster-prefixed logical name; OS is that + the .os tag
        // (applied via IndexTag, the sole owner of the marker).
        final String esPhysical = clusterPrefixSupplier.get() + name;
        final String osPhysical = IndexTag.OS.tag(esPhysical);
        final Verdict verdict = MirrorStatus.verdictFor(esExists, osExists, esCount, osCount);
        return new MirrorStatus(name, IndexKind.SITE_SEARCH,
                new MirrorStatus.EngineCopy(esExists, esCount, esPhysical),
                new MirrorStatus.EngineCopy(osExists, osCount, osPhysical),
                verdict, recommend(name, verdict));
    }

    private static String recommend(final String name, final Verdict verdict) {
        switch (verdict) {
            case IN_SYNC:
                return "In sync — no action needed.";
            case MISSING_COUNTERPART:
                return String.format("A copy of site-search index '%s' is missing on one engine. "
                        + "Re-crawl it (Site Search → Run now) to rebuild the counterpart before "
                        + "promoting to the OpenSearch-only phase.", name);
            case COUNT_DRIFT:
            default:
                return String.format("The two copies of site-search index '%s' hold a different "
                        + "number of documents. Re-crawl it (Site Search → Run now) to rebuild "
                        + "the counterpart before promoting the phase.", name);
        }
    }
}
