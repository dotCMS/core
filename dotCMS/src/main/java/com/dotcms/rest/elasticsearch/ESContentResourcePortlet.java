package com.dotcms.rest.elasticsearch;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.*;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

import com.liferay.portal.model.User;

import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

@Path("/es")
public class ESContentResourcePortlet extends BaseRestPortlet {

	ContentletAPI esapi = APILocator.getContentletAPI();
    private final WebResource webResource = new WebResource();

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("search")
	public Response search(
			@Context HttpServletRequest request,
			@Context HttpServletResponse response,
			String esQueryStr,
			@QueryParam("distinctLang") boolean distinctLang,
			@QueryParam("live") Boolean liveQueryParam,
			@QueryParam("workingSite") boolean workingSite) throws DotDataException, DotSecurityException{

		InitDataObject initData = webResource.init(null, true, request, false, null);
		User user = initData.getUser();
		ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

		final boolean live = (liveQueryParam == null) ? PageMode.get(request).showLive : liveQueryParam;
		JSONObject esQuery;

		try {
			esQuery = new JSONObject(esQueryStr);
		} catch (Exception e1) {
			Logger.warn(this.getClass(), "Unable to create JSONObject", e1);
			return ExceptionMapperUtil.createResponse(e1, Response.Status.BAD_REQUEST);
		}

		try {
			final ESSearchResults esresult = esapi.esSearch(esQuery.toString(), live, user, live);

			final JSONArray jsonCons = applyFilters(distinctLang, workingSite, esresult)
					.stream()
					.map(contentlet -> {
						try {
							return ContentResource
									.contentletToJSON(contentlet, request, response, "false", user);
						} catch (Exception e) {
							Logger.warn(this.getClass(),
									"unable JSON contentlet " + contentlet.getIdentifier());
							Logger.debug(this.getClass(), "unable to find contentlet", e);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(JSONArray::new, JSONArray::put, JSONArray::put);

			final JSONObject json = new JSONObject();
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

		}catch(DotStateException dse) {
	        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level){
	            final String noLicenseMessage = "Unable to execute ES API Requests. Please apply an Enterprise License";
	            Logger.warn(this.getClass(), noLicenseMessage);
	            return Response.status(Status.FORBIDDEN)
	                    .entity(map("message", noLicenseMessage))
	                    .header("error-message", noLicenseMessage)
	                    .build();
	        }
            Logger.warn(this.getClass(), "Error processing :" + dse.getMessage(), dse);
            return responseResource.responseError(dse.getMessage());
	        
		} catch (Exception e) {
			Logger.warn(this.getClass(), "Error processing :" + e.getMessage(), e);
			return responseResource.responseError(e.getMessage());
		}
	}

	private Collection<Contentlet> applyFilters(
			final boolean distinctLang,
			final boolean workingSite,
			final ESSearchResults esresult) throws DotDataException {

		final Collection<Contentlet> contentlets = distinctLang ?
				this.filterDistinctLangContentlet(esresult) :
				esresult;

		return workingSite ? filterByWorkingSite(contentlets) : contentlets;
	}

	private Collection<Contentlet> filterByWorkingSite(final Collection<Contentlet> contentlets)
			throws DotDataException {

		final User systemUser = APILocator.getUserAPI().getSystemUser();

		final Map<String, Host> hosts = contentlets.stream()
				.map(Contentlet::getHost)
				.distinct()
				.map(hostId -> {
					try {
						return APILocator.getHostAPI().find(hostId, systemUser, false);
					} catch (DotDataException | DotSecurityException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Host::getIdentifier, host -> host));

		return contentlets.stream()
			.filter(contentlet -> {
				try {
					return hosts.get(contentlet.getHost()).isLive();
				} catch (DotDataException | DotSecurityException e) {
					return false;
				}
			})
			.collect(Collectors.toSet());
	}

	private Collection<Contentlet> filterDistinctLangContentlet(final Collection<Contentlet> contentlets) {
		final Map<String, Contentlet> result = new HashMap<>();

		for (final Contentlet contentlet : contentlets) {
			final String contenletId = contentlet.getIdentifier();

			if (!result.containsKey(contenletId)) {
				result.put(contenletId, contentlet);
			}
		}

		return result.values();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("search")
	public Response searchPost(
			@Context HttpServletRequest request,
			@Context HttpServletResponse response,
			String esQuery,
			@QueryParam("distinctLang") boolean distinctLang,
			@QueryParam("live") Boolean liveQueryParam,
			@QueryParam("workingSite") boolean workingSite) throws DotDataException, DotSecurityException{

		return search(request, response, esQuery, distinctLang, liveQueryParam, workingSite);
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