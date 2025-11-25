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
	public Response search(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response, final String esQueryStr,
			@QueryParam("depth") final String depthParam,
			@QueryParam("live") final boolean liveParam,
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

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("search")
	public Response searchPost(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response, final String esQuery,
			@QueryParam("depth") final String depthParam,
			@QueryParam("live") final boolean liveParam,
			@QueryParam("allCategoriesInfo") final boolean allCategoriesInfo)
			throws DotDataException, DotSecurityException {
		return search(request, response, esQuery, depthParam, liveParam, allCategoriesInfo);
	}
	
	@GET
	@Path("raw")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchRawGet(@Context HttpServletRequest request) {
		return searchRaw(request);

	}

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
