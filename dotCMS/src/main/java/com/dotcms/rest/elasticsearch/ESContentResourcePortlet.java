package com.dotcms.rest.elasticsearch;

import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.AggregationBucket;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.content.index.domain.Relation;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.index.domain.TotalHits;
import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResourceResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;
import org.apache.commons.io.IOUtils;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.dotmarketing.util.NumberUtil.toInt;

@Path("/es")
@Tag(name = "Elasticsearch Content Search", description = "Backend Elasticsearch search endpoints for portlet context")
public class ESContentResourcePortlet extends BaseRestPortlet {

	ContentletAPI esapi = APILocator.getContentletAPI();
    private final WebResource webResource = new WebResource();

	/**
	 *
	 * @param request
	 * @param response
	 * @param esQueryStr
	 * @param depthParam  When this param is set to:
	 *         0 --> The contentlet object will contain the identifiers of the related contentlets
	 *         1 --> The contentlet object will contain the related contentlets
	 *         2 --> The contentlet object will contain the related contentlets, which in turn will contain the identifiers of their related contentlets
	 *         3 --> The contentlet object will contain the related contentlets, which in turn will contain a list of their related contentlets
	 *         null --> Relationships will not be sent in the response
	 * @param liveParam
	 * @param allCategoriesInfo <code>true</code> to return all fields for
	 * the categories associated to the content (key, name, description), <code>false</code>
	 * to return only categories names.
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("search")
	@Hidden
	@Operation(hidden = true)
	public Response search(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response, final String esQueryStr,
			@QueryParam("depth") final String depthParam,
			@QueryParam("live") final boolean liveParam,
			@QueryParam("userid") final String useridParam,
			@QueryParam("allCategoriesInfo") final boolean allCategoriesInfo)
			throws DotDataException, DotSecurityException {

		final InitDataObject initData = webResource.init(null, true, request, false, null);
		final User user = initData.getUser();
		final ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

		final PageMode mode = PageMode.get(request);

		final int depth = toInt(depthParam, () -> -1);

		if ((depth < 0 || depth > 3) && depthParam != null){
			final String errorMsg =
					"Error executing search. Reason: Invalid depth: " + depthParam;
			Logger.error(this, errorMsg);
			return ExceptionMapperUtil.createResponse(null, errorMsg);
		}


		JSONObject esQuery;

		try {
			esQuery = new JSONObject(esQueryStr);
		} catch (Exception e1) {
			Logger.warn(this.getClass(), "Unable to create JSONObject", e1);
			return ExceptionMapperUtil.createResponse(e1, Response.Status.BAD_REQUEST);
		}

		final User userForSearch;
		try {
			userForSearch = resolveUserForSearch(user, useridParam);
		} catch (DotDataException e) {
			return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
		}

		try {

			final boolean isAnonymous = APILocator.getUserAPI().getAnonymousUser().equals(userForSearch);
			// Route through the phase-aware ContentletAPI.search() (delegates to SearchAPI) so the
			// endpoint observes the engine selected by the current OpenSearch migration phase (ES in
			// phases 0-1, OS in phases 2-3) instead of always hitting Elasticsearch.
			final ContentSearchResults<Contentlet> esresult = esapi
					.search(esQuery.toString(), isAnonymous ? mode.showLive : liveParam, userForSearch,
							mode.respectAnonPerms);
			
    			final JSONObject json = new JSONObject();
			final JSONArray jsonCons = new JSONArray();

			for(Object x : esresult){
				final Contentlet c = (Contentlet) x;
				try {
					final ContentHelper contentHelper = ContentHelper.getInstance();
					final JSONObject jsonObject = contentHelper
							.contentletToJSON(c, response,
									"false", user, allCategoriesInfo);
					jsonCons.put(jsonObject);

					//load relationships
					if (depth!= -1){
						contentHelper
								.addRelationshipsToJSON(request, response,
										"false", user, depth, true, c,
										jsonObject, null, -1, liveParam, allCategoriesInfo);
					}

				} catch (Exception e) {
					Logger.warn(this.getClass(), " unable JSON contentlet " + c.getIdentifier());
					Logger.debug(this.getClass(), "unable to find contentlet", e);
				}
			}

			try {
				json.put("contentlets", jsonCons);
			} catch (JSONException e) {
				Logger.warn(this.getClass(), "unable to create JSONObject");
				Logger.debug(this.getClass(), "unable to create JSONObject", e);
			}

			esresult.getContentlets().clear();
			// Emit the legacy Elasticsearch-wire shape for "esresponse" (took, hits.total,
			// hits.hits[]._id/._index/._score/._source, aggregations) rebuilt from the neutral
			// ContentSearchResponse. The backend still routes through the phase-aware SearchAPI (ES in
			// phases 0-1, OS in phases 2-3); this adapter preserves the existing wire contract that the
			// dot-es-search Angular portlet and external clients depend on. See toLegacyEsJson().
			json.append("esresponse", toLegacyEsJson(esresult.getResponse()));

			if ( request.getParameter("pretty") != null ) {
				return responseResource.response(json.toString(4));
			} else {
				return responseResource.response(json.toString());
			}
		} catch (Exception e) {
			Logger.warn(this.getClass(), "Error processing :" + e.getMessage(), e);
			return responseResource.responseError(e.getMessage());
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("search")
	@Operation(
			operationId = "searchContentByESPost",
			summary = "Search content using a search query (POST)",
			description = "Executes a JSON search query against dotCMS content in a portlet context. " +
					"The request body accepts an Elasticsearch/OpenSearch JSON query. The query is routed " +
					"through the phase-aware search API, so it targets whichever engine the active OpenSearch " +
					"migration phase selects (Elasticsearch in phases 0-1, OpenSearch in phases 2-3). Results " +
					"include the matching contentlets plus the search response metadata under 'esresponse', " +
					"which retains the legacy Elasticsearch-wire shape (took, hits.total, " +
					"hits.hits[]._id/._index/._score/._source, aggregations) for backward compatibility, " +
					"regardless of the engine that served the query."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200",
					description = "Search results returned successfully",
					content = @Content(mediaType = "application/json",
							schema = @Schema(type = "object",
									description = "Search results containing the matching contentlets and the Elasticsearch-wire response metadata under 'esresponse'"))),
			@ApiResponse(responseCode = "400",
					description = "Invalid Elasticsearch query syntax",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "401",
					description = "Authentication required",
					content = @Content(mediaType = "application/json"))
	})
	public Response searchPost(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response, final String esQuery,
			@Parameter(description = "Depth of related content to include: 0=identifiers only, " +
					"1=related contentlets, 2=related contentlets with their related identifiers, " +
					"3=related contentlets with their related contentlets. Omit to exclude relationships.")
			@QueryParam("depth") final String depthParam,
			@Parameter(description = "If true, search only live content; if false, search working content")
			@QueryParam("live") final boolean liveParam,
			@Parameter(description = "Admin-only: run the query under the permission context of this user ID or email address")
			@QueryParam("userid") final String useridParam,
			@Parameter(description = "If true, return full category details (key, name, description); " +
					"if false, return only category names")
			@QueryParam("allCategoriesInfo") final boolean allCategoriesInfo)
			throws DotDataException, DotSecurityException {
		return search(request, response, esQuery, depthParam, liveParam, useridParam, allCategoriesInfo);
	}
	
	@GET
	@Path("raw")
	@Produces(MediaType.APPLICATION_JSON)
	@Hidden
	@Operation(hidden = true)
	public Response searchRawGet(@Context HttpServletRequest request) {
		return searchRaw(request);

	}

	@POST
	@Path("raw")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(
			operationId = "rawSearchContentByESPost",
			summary = "Execute raw search query (POST)",
			description = "Executes a raw JSON search query and returns the unprocessed search response " +
					"in a portlet context. The request body accepts an Elasticsearch/OpenSearch JSON query. " +
					"Unlike the /search endpoint, results are returned directly from the search engine without " +
					"additional contentlet processing. The query is routed through the phase-aware search API, " +
					"so it targets whichever engine the active OpenSearch migration phase selects (Elasticsearch " +
					"in phases 0-1, OpenSearch in phases 2-3). Note: the response is the vendor-neutral " +
					"ContentSearchResponse serialized as JSON (fields: hits, scrollId, tookMillis, aggregationTree), " +
					"not the raw Elasticsearch SearchResponse wire format previously returned by this endpoint."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200",
					description = "Search response returned successfully",
					content = @Content(mediaType = "application/json",
							schema = @Schema(type = "object",
									description = "Vendor-neutral ContentSearchResponse including hits, scrollId, tookMillis, and the aggregation tree"))),
			@ApiResponse(responseCode = "400",
					description = "Invalid Elasticsearch query",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "401",
					description = "Authentication required",
					content = @Content(mediaType = "application/json"))
	})
	public Response searchRaw(@Context HttpServletRequest request) {

        InitDataObject initData = webResource.init(null, true, request, false, null);

		HttpSession session = request.getSession();

        PageMode mode = PageMode.get(request);

		ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

		User user = initData.getUser();
		try {
			String esQuery = IOUtils.toString(request.getInputStream());

			// Route through the phase-aware ContentletAPI.searchRaw() (delegates to SearchAPI) so the
			// endpoint observes the engine selected by the current OpenSearch migration phase (ES in
			// phases 0-1, OS in phases 2-3) instead of always hitting Elasticsearch. The neutral
			// ContentSearchResponse is serialized with Jackson rather than emitting the ES wire format
			// via SearchResponse.toString(), so the endpoint keeps working once ES is decommissioned.
			final ContentSearchResponse searchResponse =
					esapi.searchRaw(esQuery, mode.showLive, user, mode.showLive);
			return responseResource.response(
					DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
							.writeValueAsString(searchResponse));

		} catch (Exception e) {
			Logger.error(this.getClass(), "Error processing :" + e.getMessage(), e);
			return responseResource.responseError(e.getMessage());

		}
	}

	/**
	 * Resolves the user whose permission context should be used for the search.
	 * Only CMS Admins can impersonate another user; all others always search as themselves.
	 * Throws {@link DotDataException} if the provided ID/email cannot be resolved.
	 */
	private User resolveUserForSearch(final User requestingUser, final String useridParam)
			throws DotDataException, DotSecurityException {
		if (!UtilMethods.isSet(useridParam)) {
			return requestingUser;
		}
		if (!APILocator.getRoleAPI().doesUserHaveRole(requestingUser, APILocator.getRoleAPI().loadCMSAdminRole())) {
			return requestingUser;
		}
		try {
			return Validator.isEmailAddress(useridParam)
					? APILocator.getUserAPI().loadByUserByEmail(useridParam, APILocator.getUserAPI().getSystemUser(), true)
					: APILocator.getUserAPI().loadUserById(useridParam, APILocator.getUserAPI().getSystemUser(), true);
		} catch (Exception e) {
			Logger.warn(this, "Could not resolve userid '" + useridParam + "': " + e.getMessage());
			throw new DotDataException("Unknown user: " + useridParam);
		}
	}

	// -------------------------------------------------------------------------
	// Legacy Elasticsearch-wire adapter (backward compatibility)
	// -------------------------------------------------------------------------
	// The /api/es/search "esresponse" field historically carried the raw Elasticsearch SearchResponse
	// JSON. The backend now routes through the phase-aware, vendor-neutral SearchAPI, so these helpers
	// rebuild that ES-wire shape from the neutral ContentSearchResponse — preserving the contract the
	// dot-es-search Angular portlet (hits/total/_source, aggregations) and external clients depend on.
	// Isolated here so the shared neutral DTO stays clean; removable once clients migrate (R7).

	/** Rebuilds the legacy ES {@code SearchResponse}-style JSON from the neutral response. */
	private static JSONObject toLegacyEsJson(final ContentSearchResponse response) throws JSONException {
		final JSONObject root = new JSONObject();
		root.put("took", response.tookMillis());
		root.put("timed_out", false);
		root.put("hits", hitsToLegacyJson(response.hits()));
		if (response.aggregationTree() != null && !response.aggregationTree().isEmpty()) {
			root.put("aggregations", aggregationsToLegacyJson(response.aggregationTree()));
		}
		if (response.suggest() != null && !response.suggest().isEmpty()) {
			root.put("suggest", new JSONObject(response.suggest()));
		}
		return root;
	}

	/** Maps neutral {@link SearchHits} to the ES {@code hits} object (total + hits[] with _id/_index/_score/_source). */
	private static JSONObject hitsToLegacyJson(final SearchHits hits) throws JSONException {
		final JSONObject hitsObj = new JSONObject();
		final TotalHits total = hits.getTotalHits();
		hitsObj.put("total", new JSONObject()
				.put("value", total.value())
				.put("relation", total.relation() == Relation.EQUAL_TO ? "eq" : "gte"));
		final JSONArray arr = new JSONArray();
		for (final SearchHit hit : hits.getHits()) {
			arr.put(new JSONObject()
					.put("_id", hit.getId())
					.put("_index", hit.getIndex())
					.put("_score", hit.getScore())
					.put("_source", new JSONObject(hit.getSourceAsMap())));
		}
		hitsObj.put("hits", arr);
		return hitsObj;
	}

	/** Maps the neutral aggregation tree (keyed by aggregation name) to the ES-native {@code aggregations} JSON. */
	private static JSONObject aggregationsToLegacyJson(final Map<String, Aggregation> tree)
			throws JSONException {
		final JSONObject aggs = new JSONObject();
		for (final Map.Entry<String, Aggregation> entry : tree.entrySet()) {
			final Aggregation aggregation = entry.getValue();
			// Emit the ES-native "typed key" (e.g. "sterms#content_types") that the dot-es-search
			// portlet's splitAggKey() parses to label the aggregation type; fall back to the plain
			// name when the neutral type is absent/unknown (splitAggKey handles both).
			final String type = aggregation.getType();
			final String key = (type != null && !type.isEmpty() && !"unknown".equals(type))
					? type + "#" + entry.getKey()
					: entry.getKey();
			aggs.put(key, aggregationToLegacyJson(aggregation));
		}
		return aggs;
	}

	/**
	 * Maps a single neutral {@link Aggregation} to its ES-native shape: {@code top_hits} metric
	 * aggregations become a {@code hits} object; bucket aggregations (terms/histogram) become a
	 * {@code buckets} array carrying {@code key}/{@code key_as_string}/{@code doc_count} and any
	 * nested sub-aggregations.
	 */
	private static JSONObject aggregationToLegacyJson(final Aggregation aggregation) throws JSONException {
		final JSONObject obj = new JSONObject();
		if (aggregation.getHits() != null) {
			obj.put("hits", hitsToLegacyJson(aggregation.getHits()));
			return obj;
		}
		final JSONArray buckets = new JSONArray();
		for (final AggregationBucket bucket : aggregation.getBuckets()) {
			final JSONObject bucketJson = new JSONObject()
					.put("key", bucket.getKey())
					.put("doc_count", bucket.getDocCount());
			if (bucket.getKeyAsString() != null) {
				bucketJson.put("key_as_string", bucket.getKeyAsString());
			}
			for (final Map.Entry<String, Aggregation> sub : bucket.getAggregations().entrySet()) {
				bucketJson.put(sub.getKey(), aggregationToLegacyJson(sub.getValue()));
			}
			buckets.put(bucketJson);
		}
		obj.put("buckets", buckets);
		return obj;
	}

}
