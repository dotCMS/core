package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.SearchAPI;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.featureflag.FeatureFlagName;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;

import org.elasticsearch.action.search.SearchResponse;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.liferay.portal.model.User;

// ES-DECOMMISSION: Velocity ViewTool exposes SearchResponse and ESSearchResults in deprecated
// bridge methods. Already delegates to neutral SearchAPI — remove esSearch, esSearchRaw at R7 cutover.
public class ESContentTool implements ViewTool {

	/** How often, at most, the Phase 3 override warning is logged — a busy template calls it per render. */
	private static final int LEGACY_SEARCH_WARNING_INTERVAL_MILLIS = 5 * 60 * 1000;

	private static final String LEGACY_SEARCH_WARNING = "A template called the deprecated "
			+ "Elasticsearch-only $estool.esSearch()/$estool.esRaw() in Phase 3 of the OpenSearch "
			+ "migration. It returned null because "
			+ FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_LEGACY_ES_SEARCH_RETURNS_NULL + " is enabled, so "
			+ "the block using it renders without results. Migrate these calls to $estool.search()/"
			+ "$estool.raw(), then turn the flag off.";

	private HttpServletRequest req;
	private User user = null;
	private Context context;
	private Host currentHost;
    private PageMode mode;

	@Override
	public void init(Object initData) {
		this.context = ((ViewContext) initData).getVelocityContext();
		this.req = ((ViewContext) initData).getRequest();

		mode = PageMode.get(this.req);
		user = getUser(req);

		try{
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		}catch(Exception e){
			Logger.error(this, "Error finding current host", e);
		}
	}

	/**
	 * Velocity {@code $estool.search(...)}: runs the query and returns DB-loaded contentlets as
	 * {@link ContentMap}s, plus the aggregation tree via {@code $results.aggregations}.
	 *
	 * <p>This path <b>lowercases the whole query</b> before executing it, so a mixed-case field name
	 * such as {@code contentType} still resolves to the physical index field {@code contenttype}.
	 * {@link #raw(String)} applies the same normalization, so both behave consistently.</p>
	 */
	public ContentSearchResults<ContentMap> search(final String esQuery) throws DotSecurityException, DotDataException {
		final SearchAPI searchAPI = APILocator.getSearchAPI();
		final ContentSearchResults<Contentlet> cons = searchAPI.search(esQuery, mode.showLive, user, true);
		final List<ContentMap> maps = new ArrayList<>();

		for (final Contentlet con : cons) {
			maps.add(new ContentMap(con, user, !mode.showLive, currentHost, context));
		}

		return new ContentSearchResults<>(cons.getResponse(), maps);
	}

	/**
	 * Velocity {@code $estool.raw(...)}: runs the query and returns the index response directly
	 * (aggregation tree + index hits) <b>without</b> loading contentlets from the DB — the right
	 * choice for analytics/aggregation templates.
	 *
	 * <p><b>Query normalization:</b> like {@link #search(String)}, this path lowercases the whole
	 * query before executing it, so a mixed-case field such as {@code "field":"contentType"} resolves
	 * to the physical lower-case index field {@code contenttype}. The folding also lowercases query
	 * values, so neither {@code raw} nor {@code search} supports case-sensitive exact matches.</p>
	 */
	public ContentSearchResponse raw(final String esQuery) throws DotSecurityException, DotDataException {
		return APILocator.getSearchAPI().searchRaw(esQuery, mode.showLive, user, true);
	}

	/**
	 * @deprecated Use {@link #search(String)} for vendor-neutral access.
	 *             This method returns Elasticsearch-specific types and will be removed in a future release.
	 *             {@code $results.hits}, {@code $results.aggregations} and {@code $results.response}
	 *             continue to resolve on {@link ContentSearchResults}. What changes is
	 *             {@code $item.map.fieldName}, which no longer resolves: {@link #search(String)}
	 *             returns {@code ContentMap} objects rather than contentlets, so fields are read
	 *             directly as {@code $item.fieldName}. The old expression yields nothing and does
	 *             not error.
	 *             <p>In Phase 3 of the OpenSearch migration this fails the page render, or returns
	 *             {@code null} when {@code FEATURE_FLAG_OPEN_SEARCH_LEGACY_ES_SEARCH_RETURNS_NULL}
	 *             is enabled — see {@link #returnsNullInFinalPhase()}.</p>
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("deprecation")
	public ESSearchResults esSearch(final String esQuery) throws DotSecurityException, DotDataException {
		if (returnsNullInFinalPhase()) {
			return null;
		}
		return APILocator.getContentletAPI().esSearch(esQuery, mode.showLive, user, true);
	}

	/**
	 * @deprecated Use {@link #raw(String)} for vendor-neutral access.
	 *             This method returns an Elasticsearch-specific type and will be removed in a future release.
	 *             <p>Like {@link #raw(String)}, the query is lowercased before execution, so mixed-case
	 *             field names resolve to the physical index field name.</p>
	 *             <p>{@code $raw.toString()} no longer returns JSON. {@link #raw(String)} returns a
	 *             {@code ContentSearchResponse} record whose {@code toString()} is the record
	 *             default. Use {@code $json.generate($raw)}, which emits the neutral response shape
	 *             ({@code tookInMillis}, {@code hits.totalHits}, and
	 *             {@code index} / {@code id} / {@code score} / {@code sourceAsMap}) rather than
	 *             Elasticsearch's envelope. Nothing errors, so a consumer expecting JSON silently
	 *             receives something else.</p>
	 *             <p>In Phase 3 of the OpenSearch migration this fails the page render, or returns
	 *             {@code null} when {@code FEATURE_FLAG_OPEN_SEARCH_LEGACY_ES_SEARCH_RETURNS_NULL}
	 *             is enabled — see {@link #returnsNullInFinalPhase()}.</p>
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("deprecation")
	public SearchResponse esRaw(final String esQuery) throws DotSecurityException, DotDataException {
		if (returnsNullInFinalPhase()) {
			return null;
		}
		return APILocator.getContentletAPI().esSearchRaw(esQuery, mode.showLive, user, true);
	}

	/**
	 * Whether the deprecated {@link #esSearch(String)} / {@link #esRaw(String)} should return
	 * {@code null} instead of querying. Only in Phase 3, and only when support has enabled
	 * {@link FeatureFlagName#FEATURE_FLAG_OPEN_SEARCH_LEGACY_ES_SEARCH_RETURNS_NULL}: by default the
	 * API underneath throws there, which fails the page render so the frozen Elasticsearch copy is
	 * never served silently. With the flag on the page renders without those results instead, and a
	 * rate-limited warning names the migration to do.
	 */
	private static boolean returnsNullInFinalPhase() {
		if (!IndexConfigHelper.MigrationPhase.current().isMigrationComplete()
				|| !Config.getBooleanProperty(
						FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_LEGACY_ES_SEARCH_RETURNS_NULL, false)) {
			return false;
		}
		Logger.warnEvery(ESContentTool.class, "legacy-es-search-phase3-returns-null",
				LEGACY_SEARCH_WARNING, LEGACY_SEARCH_WARNING_INTERVAL_MILLIS);
		return true;
	}

}
