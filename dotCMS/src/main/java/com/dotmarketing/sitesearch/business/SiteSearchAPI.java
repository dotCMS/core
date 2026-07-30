package com.dotmarketing.sitesearch.business;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.quartz.SchedulerException;

import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.DotSearchException;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchPublishStatus;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.ScheduledTask;


public interface SiteSearchAPI {
	public static final String ES_SITE_SEARCH_NAME = "sitesearch";
	public static final String ES_SITE_SEARCH_MAPPING = "_doc";
    public static final String ES_SITE_SEARCH_EXECUTE_JOB_NAME = "runningOnce";

	List<String> listIndices();

	/**
	 * Whether {@code indexName} exists on every engine that receives writes in the current migration
	 * phase (Phase&nbsp;0 → ES only; Phases&nbsp;1/2 → ES and OpenSearch; Phase&nbsp;3 → OpenSearch
	 * only).
	 *
	 * <h4>Why this exists — the incremental-crawl safety gate</h4>
	 * A Site Search index is <em>one logical index mirrored across both engines</em>. An
	 * <strong>incremental</strong> crawl writes documents <em>in place</em> into an existing index
	 * rather than rebuilding it, so it never issues a {@code createSiteSearchIndex}. If a write engine
	 * is missing its copy of that index (a phase rollout that never rebuilt an old index, a Phase-0
	 * index that has no OpenSearch twin yet, or a shadow-create that failed fire-and-forget), the
	 * in-place document write would let the engine <em>auto-create</em> the index with a dynamic
	 * mapping — {@code keyword} fields become {@code text}, breaking aggregations. The crawl planner
	 * gates on this method: when it returns {@code false} it must fall back to a <strong>full
	 * rebuild</strong>, which recreates the index (with the correct mapping) on every engine and
	 * re-points the alias — self-healing the missing mirror on the next crawl (issue #36360).
	 *
	 * <p>For a single-engine implementation this is simply whether that engine holds the index; the
	 * phase-aware router ({@code SiteSearchAPIImpl}) aggregates it across all current write providers.</p>
	 *
	 * @param indexName the logical site-search index name (no {@code .os} tag)
	 * @return {@code true} only if every current write engine already holds the index
	 */
	boolean existsOnAllWriteEngines(String indexName);

	/**
	 * Whether the index's copies on every current write engine are <strong>in sync</strong> — i.e.
	 * the index exists on all of them (see {@link #existsOnAllWriteEngines(String)}) <em>and</em> its
	 * document counts match across engines.
	 *
	 * <h4>Why counts, not just existence</h4>
	 * A missing twin is one kind of desync; the other is <em>content drift</em> — both engines hold
	 * the index but with different documents (e.g. an OpenSearch shadow write failed fire-and-forget
	 * during a previous incremental crawl, so ES has documents OpenSearch does not). Existence alone
	 * cannot see that. Because a Site Search index is written only by the crawl job (single writer,
	 * immediate refresh) and no crawl on the same index runs concurrently, at crawl-planning time the
	 * copies are quiescent, so equal document counts is a sound in-sync invariant and a mismatch is
	 * real drift.
	 *
	 * <p>The incremental-crawl gate uses this instead of bare existence: an incremental crawl writes
	 * only the new delta and would perpetuate any pre-existing drift, so when the mirrors are out of
	 * sync the crawl is demoted to a full rebuild that re-creates identical copies on every engine
	 * (issue #36360). For a single write engine there is nothing to compare, so this is trivially
	 * {@code true}; the phase-aware router aggregates it across all current write providers.</p>
	 *
	 * @param indexName the logical site-search index name (no {@code .os} tag)
	 * @return {@code true} only if the index exists on every write engine with matching document counts
	 */
	boolean writeMirrorsInSync(String indexName);

	/**
	 * Resolves site-search aliases to their backing index names — phase-aware and OpenSearch
	 * {@code .os}-aware. Keys (alias) and values (index) are both <strong>logical</strong> names.
	 *
	 * <h4>Design decision — why this lives on {@code SiteSearchAPI}, not the content-index router</h4>
	 * A Site Search index is <em>one logical index mirrored across both engines</em>, so this API's
	 * surface speaks in logical (untagged) names — a vendor-neutral handle — and each engine adapter
	 * translates that handle to its physical form at the boundary (ES uses it verbatim; OpenSearch
	 * appends {@code .os}). Alias resolution therefore MUST live here: the OpenSearch adapter knows to
	 * re-tag the lookup with {@code .os}, whereas the content-index router
	 * ({@code IndexAPI#getAliasToIndexMap}) builds the OS physical name <em>without</em> {@code .os}
	 * and, in Phases&nbsp;2/3 (OS reads), queries a name that does not exist — silently returning
	 * nothing ("Index Alias not found"). Routing site-search alias resolution through the content
	 * router was the root cause fixed in issue #36360; callers must use this method and never the
	 * content router with a logical Site Search name.
	 *
	 * <p>The {@code .os} tag never crosses this boundary: it is applied only inside the OpenSearch
	 * adapter for the lookup and stripped back off the resolved value, so both the alias keys and the
	 * index values returned here are logical and directly comparable against {@link #listIndices()}
	 * output.</p>
	 *
	 * <h4>Why dual-write phases cannot collide the map</h4>
	 * In Phases&nbsp;1/2 the ES twin ({@code xxx}) and the OpenSearch twin ({@code xxx.os}) carry the
	 * same alias, so a naive ES&cup;OS <em>merge</em> would map one alias key to two different index
	 * values and silently drop one. This method avoids that by resolving against a <strong>single
	 * engine — the current phase's read provider</strong> (ES in Phases&nbsp;0/1, OS in
	 * Phases&nbsp;2/3), never a union. The two twins never land in the same map: Phase&nbsp;1 returns
	 * {@code {lol=xxx}} from ES; Phase&nbsp;2 returns {@code {lol=xxx}} from OS ({@code xxx.os} with the
	 * tag stripped). Because both twins share the same logical base, a synchronized cluster resolves
	 * the alias to the same logical name in every phase.
	 *
	 * <p>Two residual edges, both benign here:</p>
	 * <ol>
	 *   <li><strong>Multi-index alias within one engine</strong> (one alias pointing at two indices on
	 *       the same provider) would lose one entry to the reverse-map — but that state is prevented
	 *       upstream by the {@code createAlias} existence check (issue #36360). A healthy cluster has
	 *       one index per alias per engine.</li>
	 *   <li><strong>Mirror desync</strong> (the ES and OS aliases point at <em>different</em> logical
	 *       indices) makes the result diverge by phase — which is correct, since you resolve against
	 *       the engine you read from; it is a mirror-reconciliation concern, not a collision.</li>
	 * </ol>
	 *
	 * @return map of logical alias name to logical index name; empty when nothing resolves
	 */
	Map<String, String> getAliasToIndexMap();

	/**
	 * This basically tells you if the index passed as parameter is the default site search index or not
	 * @param indexName
	 * @return
	 * @throws DotDataException
	 */
    boolean isDefaultIndex(String indexName) throws DotDataException;

	void activateIndex(String indexName) throws DotDataException;

	void deactivateIndex(String indexName) throws DotDataException, IOException;

	boolean createSiteSearchIndex(String indexName, String alias, int shards) throws DotSearchException, IOException;

	boolean setAlias(String indexName, final String alias);

	List<ScheduledTask> getTasks() throws SchedulerException;

	void deleteTask(String taskName) throws SchedulerException;

	void scheduleTask(SiteSearchConfig config) throws SchedulerException, ParseException, ClassNotFoundException;

	void putToIndex(String idx, SiteSearchResult res, String resultType);

	void putToIndex(String idx, List<SiteSearchResult> res, String resultType);

	void deleteFromIndex(String idx, String docId);

	SiteSearchResults search(String query, int start, int rows);
	
	SiteSearchResults search(String indexName, String query, int start, int rows);

	ScheduledTask getTask(String taskName) throws SchedulerException;
	
	void pauseTask(String taskName)  throws SchedulerException;

	SiteSearchPublishStatus getTaskProgress(String jobName) throws SchedulerException;

	boolean isTaskRunning(String jobName) throws SchedulerException;

	void executeTaskNow(SiteSearchConfig config) throws SchedulerException, ParseException, ClassNotFoundException;

	SiteSearchResult getFromIndex(String index, String id);

	Map<String, Aggregation> getAggregations(String indexName, String query) throws DotDataException;

	/***
	 * @deprecated use getAggregations instead
	 */
	@Deprecated
	Map<String, Aggregation> getFacets(String indexName, String query) throws DotDataException;

    List<String> listClosedIndices();

	public void deleteOldSiteSearchIndices();

	/**
	 * Deletes a single site-search index by name from every engine that holds it (ES and, during a
	 * migration, its OpenSearch counterpart), mirroring the operator's single-index view. The
	 * active (default) site-search index cannot be deleted — deactivate it first.
	 *
	 * @param indexName the site-search index name (must be a {@code sitesearch_*} name)
	 * @throws DotDataException if the name is not a site-search index or the delete fails
	 * @throws IOException      on an index-engine error
	 */
	void deleteIndex(String indexName) throws DotDataException, IOException;
}
