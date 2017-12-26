package com.dotcms.rest.api.v1.contenttype;

import java.io.Serializable;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContentTypesPaginator;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import com.dotcms.repackage.javax.ws.rs.*;
import static com.dotcms.util.CollectionsUtils.map;

@Path("/v1/contenttype")
public class ContentTypeResource implements Serializable {
	private final WebResource webResource;
	private final ContentTypeHelper contentTypeHelper;
	private final PaginationUtil paginationUtil;
	private final WorkflowHelper workflowHelper;

	public ContentTypeResource() {
		this(ContentTypeHelper.getInstance(), new WebResource(),
				new PaginationUtil(new ContentTypesPaginator()), WorkflowHelper.getInstance());
	}

	@VisibleForTesting
	public ContentTypeResource(final ContentTypeHelper contentletHelper, final WebResource webresource,
							   final PaginationUtil paginationUtil, final WorkflowHelper workflowHelper) {

		this.webResource       = webresource;
		this.contentTypeHelper = contentletHelper;
		this.paginationUtil    = paginationUtil;
		this.workflowHelper    = workflowHelper;
	}

	private static final long serialVersionUID = 1L;


	@POST
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response createType(@Context final HttpServletRequest req, final ContentTypeForm form)
			throws DotDataException, DotSecurityException {
		final InitDataObject initData = this.webResource.init(null, true, req, true, null);
		final User user = initData.getUser();

		Response response = null;

		try {
			Logger.debug(this, String.format("Saving new content type", form.getRequestJson()));

			final Iterable<ContentTypeForm.ContentTypeFormEntry> typesToSave = form.getIterable();
			final List<ContentType> retTypes = new ArrayList<>();

			// Validate input
			for (final ContentTypeForm.ContentTypeFormEntry entry : typesToSave) {
				final ContentType type = entry.contentType;
				final List<String> workflowsIds = entry.workflowsIds;

				if (UtilMethods.isSet(type.id()) && !UUIDUtil.isUUID(type.id())) {
					return ExceptionMapperUtil.createResponse(null, "ContentType 'id' if set, should be a uuid");
				}
				final ContentType contentTypeSaved = APILocator.getContentTypeAPI(user, true).save(type);
				retTypes.add(contentTypeSaved);
				this.workflowHelper.saveSchemesByContentType(contentTypeSaved.inode(), user, workflowsIds);
			}


			response = Response.ok(new ResponseEntityView(new JsonContentTypeTransformer(retTypes).mapList())).build();

		} catch (DotStateException | DotDataException e) {

			response = ExceptionMapperUtil.createResponse(null, "Content-type is not valid ("+ e.getMessage() +")");

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@PUT
	@Path("/id/{id}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response updateType(@PathParam("id") final String id, final ContentTypeForm form,
							   @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final ContentTypeAPI capi = APILocator.getContentTypeAPI(user, true);

		Response response = null;

		try {
			ContentType contentType = form.getContentType();

			Logger.debug(this, String.format("Updating content type", form.getRequestJson()));

			if (!UtilMethods.isSet(contentType.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else {

				ContentType currentContentType = capi.find(id);

				if (!currentContentType.id().equals(contentType.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field id '"+ id +"' does not match a content-type with id '"+ contentType.id() +"'");

				} else {

					contentType = capi.save(contentType);

					final List<String> workflowsIds = form.getWorkflowsIds();
					workflowHelper.saveSchemesByContentType(id, user, workflowsIds);
					response = Response.ok(new ResponseEntityView(new JsonContentTypeTransformer(contentType).mapObject())).build();
				}
			}
		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch ( DotStateException | DotDataException e) {

			response = ExceptionMapperUtil.createResponse(null, "Content-type is not valid ("+ e.getMessage() +")");

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@DELETE
	@Path("/id/{id}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response deleteType(@PathParam("id") final String id, @Context final HttpServletRequest req)
			throws DotDataException, DotSecurityException, JSONException {

		final InitDataObject initData = this.webResource.init(null, true, req, true, null);
		final User user = initData.getUser();

		ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);


		ContentType type = null;
		try {
			type = tapi.find(id);
		} catch (NotFoundInDbException nfdb) {
			try {
				type = tapi.find(id);
			} catch (NotFoundInDbException nfdb2) {
				return Response.status(404).build();
			}
		}

		tapi.delete(type);


		JSONObject joe = new JSONObject();
		joe.put("deleted", type.id());


		Response response = Response.ok(new ResponseEntityView(joe.toString())).build();
		return response;
	}


	@GET
	@Path("/id/{idOrVar}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response getType(@PathParam("idOrVar") final String idOrVar, @Context final HttpServletRequest req)
			throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
		Response response = Response.status(404).build();
		final Map<String, Object> resultMap = new HashMap<>();
		try {

			final ContentType type = tapi.find(idOrVar);
			resultMap.putAll(new JsonContentTypeTransformer(type).mapObject());
			resultMap.put("workflows", this.workflowHelper.findSchemesByContentType(type.id(), initData.getUser()));

			response = Response.ok(new ResponseEntityView(resultMap)).build();
		} catch (NotFoundInDbException nfdb2) {
			// nothing to do here, will throw a 404
		}

		return response;
	}


	@GET
	@Path("/basetypes")
	@JSONP
	@InitRequestRequired
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getRecentBaseTypes(@Context final HttpServletRequest request) {

		Response response = null;

		try {
			List<BaseContentTypesView> types = contentTypeHelper.getTypes(request);
			response = Response.ok(new ResponseEntityView(types)).build();
		} catch (Exception e) { // this is an unknown error, so we report as a 500.

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	} // getTypes.

	/**
	 * Return a list of {@link ContentType}, entity response syntax:.
	 *
	 * <code>
	 *  {
	 *      contentTypes: array of ContentType
	 *      total: total number of content types
	 *  }
	 * <code/>
	 *
	 * Url sintax: contenttype?query=query-string&limit=n-limit&offset=n-offset&orderby=fieldname-order_direction
	 *
	 * where:
	 *
	 * <ul>
	 *     <li>query-string: just return ContentTypes who content this pattern</li>
	 *     <li>n-limit: limit of items to return</li>
	 *     <li>n-offset: offset</li>
	 *     <li>fieldname: field to order by</li>
	 *     <li>order_direction: asc for upward order and desc for downward order</li>
	 * </ul>
	 *
	 * Url example: v1/contenttype?query=New%20L&limit=4&offset=5&orderby=name-asc
	 *
	 * @param request
	 * @return
	 */
	@GET
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getContentTypes(@Context final HttpServletRequest request,
										  @QueryParam(PaginationUtil.FILTER)   final String filter,
										  @QueryParam(PaginationUtil.PAGE) final int page,
										  @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
										  @DefaultValue("upper(name)") @QueryParam(PaginationUtil.ORDER_BY) String orderbyParam,
										  @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) String direction) {

		final InitDataObject initData = webResource.init(null, true, request, true, null);

		Response response = null;

		final String orderBy = getOrderByRealName(orderbyParam);
		final User user = initData.getUser();

		try {
			response = this.paginationUtil.getPage(request, user, filter, page, perPage, orderBy, direction);
		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
			Logger.error(this, e.getMessage(), e);
		}

		return response;
	}

	private String getOrderByRealName(final String orderbyParam) {
		if ("modDate".equals(orderbyParam)){
			return "mod_date";
		}else if ("variable".equals(orderbyParam)) {
			return "velocity_var_name";
		} else {
			return orderbyParam;
		}
	}

}
