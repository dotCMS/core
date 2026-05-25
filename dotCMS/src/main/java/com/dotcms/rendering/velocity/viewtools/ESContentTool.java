package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.index.SearchAPI;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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

import com.dotcms.content.index.ESCoupled;
import com.liferay.portal.model.User;

@ESCoupled(
    reason = "Velocity ViewTool exposes SearchResponse and ESSearchResults in deprecated bridge methods. " +
             "Already delegates to neutral SearchAPI — remove bridge methods at R7 cutover.",
    remove = {"esSearch", "esSearchRaw"}
)
public class ESContentTool implements ViewTool {

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

	public ContentSearchResults<ContentMap> search(final String esQuery) throws DotSecurityException, DotDataException {
		final SearchAPI searchAPI = APILocator.getSearchAPI();
		final ContentSearchResults<Contentlet> cons = searchAPI.search(esQuery, mode.showLive, user, true);
		final List<ContentMap> maps = new ArrayList<>();

		for (final Contentlet con : cons) {
			maps.add(new ContentMap(con, user, !mode.showLive, currentHost, context));
		}

		return new ContentSearchResults<>(cons.getResponse(), maps);
	}

	public ContentSearchResponse raw(final String esQuery) throws DotSecurityException, DotDataException {
		return APILocator.getSearchAPI().searchRaw(esQuery, mode.showLive, user, true);
	}

	/**
	 * @deprecated Use {@link #search(String)} for vendor-neutral access.
	 *             This method returns Elasticsearch-specific types and will be removed in v26.08.04.
	 *             Velocity templates using {@code $results.hits}, {@code $results.aggregations},
	 *             or {@code $results.response} must migrate to the neutral equivalents exposed by
	 *             {@link ContentSearchResults}.
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("deprecation")
	public ESSearchResults esSearch(final String esQuery) throws DotSecurityException, DotDataException {
		return APILocator.getContentletAPI().esSearch(esQuery, mode.showLive, user, true);
	}

	/**
	 * @deprecated Use {@link #raw(String)} for vendor-neutral access.
	 *             This method returns an Elasticsearch-specific type and will be removed in v26.08.04.
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("deprecation")
	public SearchResponse esRaw(final String esQuery) throws DotSecurityException, DotDataException {
		return APILocator.getContentletAPI().esSearchRaw(esQuery, mode.showLive, user, true);
	}

}
