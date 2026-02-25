package com.dotcms.rest.elasticsearch;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.rest.ContentResource;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResourceResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;

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

import static com.dotmarketing.util.NumberUtil.toInt;

@Path("/es")
@Tag(name = "Elasticsearch Content Search", description = "Elasticsearch-based content search and raw query endpoints")
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
	@Hidden
	@Operation(
			operationId = "searchContentByESGet",
			summary = "Search content via ES (use POST instead)",
			description = "Performs an Elasticsearch content search using a GET request. " +
					"This endpoint is hidden from the API documentation because the POST " +
					"variant should be used instead to properly send the ES query in the request body."
	)
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("search")
	public Response search(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response, final String esQueryStr,
			@Parameter(description = "Relationship depth for related contentlets. " +
					"0=identifiers only, 1=related objects, 2=related with their identifiers, " +
					"3=related with their related objects. Null omits relationships.")
			@QueryParam("depth") final String depthParam,
			@Parameter(description = "If true, returns only live content; if false, returns working content")
			@QueryParam("live") final boolean liveParam,
			@Parameter(description = "If true, returns full category details (key, name, description) " +
					"instead of just category names")
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

		try {

			final boolean isAnonymous = APILocator.getUserAPI().getAnonymousUser().equals(user);
			final ESSearchResults esresult = esapi
					.esSearch(esQuery.toString(), isAnonymous ? mode.showLive : liveParam, user,
							mode.respectAnonPerms);
			
			final JSONObject json = new JSONObject();
			final JSONArray jsonCons = new JSONArray();

			for(Object x : esresult){
				final Contentlet c = (Contentlet) x;
				try {

					final JSONObject jsonObject = ContentResource
							.contentletToJSON(c, request, response,
									"false", user, allCategoriesInfo);
					jsonCons.put(jsonObject);

					//load relationships
					if (depth!= -1){
						ContentResource
								.addRelationshipsToJSON(request, response,
										"false", user, depth, true, c,
										jsonObject, null, -1, liveParam, allCategoriesInfo);
					}

				} catch (Exception e) {
					Logger.warn(this.getClass(), "unable JSON contentlet " + c.getIdentifier());
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
			json.append("esresponse", new JSONObject(esresult.getResponse().toString()));

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

	@Operation(
			operationId = "searchContentByES",
			summary = "Search content using an Elasticsearch query",
			description = "Performs a content search using a raw Elasticsearch query passed in the " +
					"request body as JSON. Returns matching contentlets along with the raw ES " +
					"response metadata. Supports relationship depth control and category info."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Search results returned successfully",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = Object.class))),
			@ApiResponse(responseCode = "400", description = "Invalid Elasticsearch query or invalid depth parameter",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "401", description = "Authentication required",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("search")
	public Response searchPost(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@RequestBody(
					description = "Elasticsearch query in JSON format",
					required = true,
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = Object.class))
			)
			final String esQuery,
			@Parameter(description = "Relationship depth for related contentlets. " +
					"0=identifiers only, 1=related objects, 2=related with their identifiers, " +
					"3=related with their related objects. Null omits relationships.")
			@QueryParam("depth") final String depthParam,
			@Parameter(description = "If true, returns only live content; if false, returns working content")
			@QueryParam("live") final boolean liveParam,
			@Parameter(description = "If true, returns full category details (key, name, description) " +
					"instead of just category names")
			@QueryParam("allCategoriesInfo") final boolean allCategoriesInfo)
			throws DotDataException, DotSecurityException {
		return search(request, response, esQuery, depthParam, liveParam, allCategoriesInfo);
	}
	
	@Hidden
	@Operation(
			operationId = "searchRawESGet",
			summary = "Raw ES search via GET (use POST instead)",
			description = "Performs a raw Elasticsearch search using a GET request. " +
					"This endpoint is hidden from the API documentation because the POST " +
					"variant should be used instead to properly send the ES query in the request body."
	)
	@GET
	@Path("raw")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchRawGet(@Context HttpServletRequest request) {
		return searchRaw(request);

	}

	@Operation(
			operationId = "searchRawES",
			summary = "Perform a raw Elasticsearch search",
			description = "Executes a raw Elasticsearch query and returns the unprocessed ES " +
					"response. The ES query JSON is read from the request body. Unlike the " +
					"/search endpoint, this returns the raw Elasticsearch response without " +
					"transforming contentlets into the standard dotCMS JSON format."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Raw Elasticsearch response returned successfully",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = Object.class))),
			@ApiResponse(responseCode = "400", description = "Invalid Elasticsearch query",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "401", description = "Authentication required",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Path("raw")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchRaw(@Context HttpServletRequest request) {

        InitDataObject initData = webResource.init(null, true, request, false, null);

		HttpSession session = request.getSession();

        PageMode mode = PageMode.get(request);

		ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

		User user = initData.getUser();
		try {
			String esQuery = IOUtils.toString(request.getInputStream());

			return responseResource.response(esapi.esSearchRaw(esQuery, mode.showLive, user, mode.showLive).toString());

		} catch (Exception e) {
			Logger.error(this.getClass(), "Error processing :" + e.getMessage(), e);
			return responseResource.responseError(e.getMessage());

		}
	}

}
