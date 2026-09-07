package com.dotcms.content.index.migration;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotcms.enterprise.publishing.sitesearch.ESSiteSearchAPI;
import com.dotcms.enterprise.publishing.sitesearch.OSSiteSearchAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
     * The per-index mirror status for every logical site-search index that exists on <em>either</em>
     * engine (so a counterpart missing on one side still appears). Purely factual and phase-independent.
     */
    public List<MirrorStatus> statuses() {
        final TreeSet<String> names = new TreeSet<>(esImpl.listIndices());
        names.addAll(osImpl.listIndices());
        // One alias lookup per engine for the whole set — not one per index. Operators identify a
        // site-search index by its alias, never by its sitesearch_<timestamp>_<uuid> name, so the
        // report is unusable without it (issue #36983).
        final Map<String, String> esAliases = indexToAlias(esImpl);
        final Map<String, String> osAliases = indexToAlias(osImpl);
        final List<MirrorStatus> statuses = new ArrayList<>(names.size());
        for (final String name : names) {
            statuses.add(statusFor(name, esAliases.get(name), osAliases.get(name)));
        }
        return statuses;
    }

    /**
     * Reverses one engine's {@code alias -> index} map into {@code index -> alias}. Both leaves return
     * logical (untagged) index names, so the keys line up with {@link SiteSearchAPI#listIndices()}.
     */
    private static Map<String, String> indexToAlias(final SiteSearchAPI engine) {
        final Map<String, String> reversed = new HashMap<>();
        engine.getAliasToIndexMap().forEach((alias, index) -> reversed.put(index, alias));
        return reversed;
    }

    private MirrorStatus statusFor(final String name, final String esAlias, final String osAlias) {
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
                new MirrorStatus.EngineCopy(esExists, esCount, esPhysical, esAlias),
                new MirrorStatus.EngineCopy(osExists, osCount, osPhysical, osAlias),
                verdict, recommend(name, verdict, esAlias, osAlias));
    }

    /**
     * A site-search index name: {@code sitesearch_<timestamp>[_<uuid>]}. Used to spot an alias that is
     * really an index name — see {@link #corruptedAlias(String, String)}.
     */
    private static final Pattern INDEX_NAME_SHAPED =
            Pattern.compile("^" + SiteSearchAPI.ES_SITE_SEARCH_NAME + "_\\d{8,}.*", Pattern.CASE_INSENSITIVE);

    /**
     * Whether {@code candidate} is shaped like a crawl-generated site-search index name
     * ({@code sitesearch_<timestamp>[_<uuid>]}) rather than a name a person would choose as an alias.
     *
     * <p>Shared with {@code SiteSearchJobImpl}, which refuses to adopt such a string as a new index's
     * alias: a job whose stored alias is a stale index name would otherwise resurrect the name of an
     * index the previous crawl deleted, which is the very defect this reconciler reports
     * (issue #36983). One definition, so the detector and the preventer cannot drift apart.</p>
     */
    public static boolean isIndexNameShaped(final String candidate) {
        return candidate != null && INDEX_NAME_SHAPED.matcher(candidate).matches();
    }

    /**
     * The alias of {@code name} on either engine when it is really an INDEX NAME rather than an alias —
     * the fingerprint of the defect fixed in issue #36983, where a crawl re-applied the name of the
     * index it had just deleted as the new index's alias. The fix stops it from happening again but
     * cannot restore an alias already overwritten, so the report surfaces it: this is the only way an
     * operator can tell which indices still need their alias restored.
     *
     * @return the offending alias, or {@code null} when neither engine's alias looks like an index name
     */
    private static String corruptedAlias(final String esAlias, final String osAlias) {
        if (isIndexNameShaped(esAlias)) {
            return esAlias;
        }
        if (isIndexNameShaped(osAlias)) {
            return osAlias;
        }
        return null;
    }

    private static String recommend(final String name, final Verdict verdict, final String esAlias,
            final String osAlias) {
        // Reported alongside the sync verdict, never as part of it: the verdict measures data
        // integrity (existence + counts), while a damaged alias is an identification problem. Folding
        // it into the verdict would block a phase change over something that costs no data.
        final String corrupted = corruptedAlias(esAlias, osAlias);
        final String aliasNote = corrupted == null ? "" : String.format(
                " NOTE: the alias '%s' is an index name, not a real alias — a crawl overwrote the "
                        + "original alias of '%s' (issue #36983). Re-crawl this index with the intended "
                        + "alias to restore it.", corrupted, name);
        switch (verdict) {
            case IN_SYNC:
                return "In sync — no action needed." + aliasNote;
            case MISSING_COUNTERPART:
                return String.format("A copy of site-search index '%s' is missing on one engine. "
                        + "Re-crawl it (Site Search → Run now) to rebuild the counterpart before "
                        + "promoting to the OpenSearch-only phase.", name) + aliasNote;
            case COUNT_DRIFT:
            default:
                return String.format("The two copies of site-search index '%s' hold a different "
                        + "number of documents. Re-crawl it (Site Search → Run now) to rebuild "
                        + "the counterpart before promoting the phase.", name) + aliasNote;
        }
    }
}
