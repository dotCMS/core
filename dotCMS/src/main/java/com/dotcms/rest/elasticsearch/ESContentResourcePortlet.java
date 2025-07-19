package com.dotcms.rest.elasticsearch;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.rest.ContentHelper;
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
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.SwaggerCompliant;

import static com.dotmarketing.util.NumberUtil.toInt;

@SwaggerCompliant(value = "Legacy and utility APIs", batch = 8)
@Path("/es")
@Tag(name = "Search")
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
	@Operation(
		summary = "Search content using Elasticsearch",
		description = "Executes a content search using Elasticsearch query syntax. Returns contentlets with optional relationship depth and category information. Supports both anonymous and authenticated access with appropriate permission filtering."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Search executed successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(type = "object", description = "Search results containing contentlets array and elasticsearch response metadata"))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid Elasticsearch query or depth parameter",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during search execution",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("search")
	public Response search(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response, 
			@Parameter(description = "Elasticsearch query string in JSON format", required = true) final String esQueryStr,
			@Parameter(description = "Relationship depth (0-3): 0=identifiers only, 1=related content, 2=nested identifiers, 3=nested content, null=no relationships", required = false) @QueryParam("depth") final String depthParam,
			@Parameter(description = "Whether to return only live content", required = false) @QueryParam("live") final boolean liveParam,
			@Parameter(description = "Whether to return all category fields (name, key, description) or just names", required = false) @QueryParam("allCategoriesInfo") final boolean allCategoriesInfo)
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
		summary = "Search content using Elasticsearch (POST)",
		description = "Executes a content search using Elasticsearch query syntax via POST method. Accepts JSON query in request body. Returns contentlets with optional relationship depth and category information."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Search executed successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(type = "object", description = "Search results containing contentlets array and elasticsearch response metadata"))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid Elasticsearch query or depth parameter",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during search execution",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("search")
	public Response searchPost(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response, 
			@RequestBody(description = "Elasticsearch query in JSON format", required = true) final String esQuery,
			@Parameter(description = "Relationship depth (0-3): 0=identifiers only, 1=related content, 2=nested identifiers, 3=nested content, null=no relationships", required = false) @QueryParam("depth") final String depthParam,
			@Parameter(description = "Whether to return only live content", required = false) @QueryParam("live") final boolean liveParam,
			@Parameter(description = "Whether to return all category fields (name, key, description) or just names", required = false) @QueryParam("allCategoriesInfo") final boolean allCategoriesInfo)
			throws DotDataException, DotSecurityException {
		return search(request, response, esQuery, depthParam, liveParam, allCategoriesInfo);
	}
	
	@Operation(
		summary = "Execute raw Elasticsearch query (GET)",
		description = "Executes a raw Elasticsearch query via GET method and returns the raw Elasticsearch response without dotCMS processing or transformation."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Raw search executed successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(type = "object", description = "Raw Elasticsearch query response as JSON object"))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during raw search execution",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("raw")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchRawGet(@Context HttpServletRequest request) {
		return searchRaw(request);

	}

	@Operation(
		summary = "Execute raw Elasticsearch query (POST)",
		description = "Executes a raw Elasticsearch query via POST method. Accepts the query from the request input stream and returns the raw Elasticsearch response without dotCMS processing or transformation."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Raw search executed successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(type = "object", description = "Raw Elasticsearch query response as JSON object"))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during raw search execution",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Path("raw")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response searchRaw(@RequestBody(description = "Raw Elasticsearch query in JSON format", required = true) @Context HttpServletRequest request) {

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

	@Operation(
		summary = "Load Elasticsearch layout component",
		description = "Renders the Elasticsearch portlet layout component for the dotCMS backend interface. Used for displaying the Elasticsearch administration UI within the admin interface."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Elasticsearch layout loaded successfully",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - user authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions to access Elasticsearch admin",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error loading layout",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/layout/{params:.*}")
	@Produces("text/html")
	public Response getLayout(@Context HttpServletRequest request, 
							 @Context HttpServletResponse response, 
							 @Parameter(description = "Layout parameters (portletId/jspName)", required = true)
							 @PathParam("params") String params)
			throws DotDataException, ServletException, IOException, PortalException, SystemException {
		return super.getLayout(request, response, params);
	}

}
