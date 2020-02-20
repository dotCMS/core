package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.PermissionsUtil;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContentTypesPaginator;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.workflow.form.WorkflowSystemActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/v1/contenttype")
public class ContentTypeResource implements Serializable {
	private final WebResource 		webResource;
	private final ContentTypeHelper contentTypeHelper;
	private final PaginationUtil 	paginationUtil;
	private final WorkflowHelper 	workflowHelper;
	private final PermissionAPI     permissionAPI;

	public ContentTypeResource() {
		this(ContentTypeHelper.getInstance(), new WebResource(),
				new PaginationUtil(new ContentTypesPaginator()),
				WorkflowHelper.getInstance(), APILocator.getPermissionAPI());
	}

	@VisibleForTesting
	public ContentTypeResource(final ContentTypeHelper contentletHelper, final WebResource webresource,
							   final PaginationUtil paginationUtil, final WorkflowHelper workflowHelper,
							   final PermissionAPI permissionAPI) {

		this.webResource       = webresource;
		this.contentTypeHelper = contentletHelper;
		this.paginationUtil    = paginationUtil;
		this.workflowHelper    = workflowHelper;
		this.permissionAPI     = permissionAPI;
	}

	private static final long serialVersionUID = 1L;

	public static final String SELECTED_STRUCTURE_KEY = "selectedStructure";

	@POST
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response createType(@Context final HttpServletRequest req,
									 @Context final HttpServletResponse res,
									 final ContentTypeForm form)
			throws DotDataException {
		final InitDataObject initData = this.webResource.init(null, req, res, true, null);
		final User user = initData.getUser();

		Response response = null;

		try {

			Logger.debug(this, ()->String.format("Saving new content type '%s' ", form.getRequestJson()));
			final HttpSession session = req.getSession(false);
			final Iterable<ContentTypeForm.ContentTypeFormEntry> typesToSave = form.getIterable();
			final List<Map<Object, Object>> retTypes = new ArrayList<>();

			// Validate input
			for (final ContentTypeForm.ContentTypeFormEntry entry : typesToSave) {

				final ContentType type = entry.contentType;
				final Set<String> workflowsIds = new HashSet<>(entry.workflowsIds);

				if (UtilMethods.isSet(type.id()) && !UUIDUtil.isUUID(type.id())) {
					return ExceptionMapperUtil.createResponse(null, "ContentType 'id' if set, should be a uuid");
				}

				final Tuple2<ContentType, List<SystemActionWorkflowActionMapping>>  tuple2 =
						this.saveContentTypeAndDependencies(type, initData.getUser(), workflowsIds,
							form.getSystemActions(), APILocator.getContentTypeAPI(user, true), true);
				final ContentType contentTypeSaved = tuple2._1;

				ImmutableMap<Object, Object> responseMap = ImmutableMap.builder()
							.putAll(new JsonContentTypeTransformer(contentTypeSaved).mapObject())
						.put("workflows", this.workflowHelper.findSchemesByContentType(contentTypeSaved.id(), initData.getUser()))
						.put("systemActionMappings", tuple2._2.stream()
								.collect(Collectors.toMap(mapping-> mapping.getSystemAction(), mapping->mapping)))
						.build();
				retTypes.add(responseMap);

				// save the last one to the session to be compliant with #13719
				if(null != session){
                  session.removeAttribute(SELECTED_STRUCTURE_KEY);
				}
			}

			response = Response.ok(new ResponseEntityView(retTypes)).build();
		} catch (IllegalArgumentException e) {
			Logger.error(this, e.getMessage());
			response = ExceptionMapperUtil
					.createResponse(null, "Content-type is not valid (" + e.getMessage() + ")");
		}catch (DotStateException | DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			response = ExceptionMapperUtil
					.createResponse(null, "Content-type is not valid (" + e.getMessage() + ")");
		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@PUT
	@Path("/id/{idOrVar}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response updateType(@PathParam("idOrVar") final String idOrVar, final ContentTypeForm form,
							   @Context final HttpServletRequest req, @Context final HttpServletResponse res) throws DotDataException {

		final InitDataObject initData = this.webResource.init(null, req, res, false, null);
		final User user = initData.getUser();
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);

		Response response = null;

		try {
			ContentType contentType = form.getContentType();

			Logger.debug(this, String.format("Updating content type  '%s' ", form.getRequestJson()));

			if (!UtilMethods.isSet(contentType.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else {

				final ContentType currentContentType = contentTypeAPI.find(idOrVar);

				if (!currentContentType.id().equals(contentType.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field id '"+ idOrVar +"' does not match a content-type with id '"+ contentType.id() +"'");

				} else {

					final Tuple2<ContentType, List<SystemActionWorkflowActionMapping>> tuple2 = this.saveContentTypeAndDependencies(contentType, user,
							new HashSet<>(form.getWorkflowsIds()), form.getSystemActions(), contentTypeAPI, false);
					final ImmutableMap.Builder<Object, Object> builderMap =
							ImmutableMap.builder()
							.putAll(new JsonContentTypeTransformer(tuple2._1).mapObject())
							.put("workflows", this.workflowHelper.findSchemesByContentType(contentType.id(), initData.getUser()))
							.put("systemActionMappings", tuple2._2.stream()
									.collect(Collectors.toMap(mapping-> mapping.getSystemAction(), mapping->mapping)));

					response = Response.ok(new ResponseEntityView(builderMap.build())).build();
				}
			}
		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch ( DotStateException | DotDataException e) {

			response = ExceptionMapperUtil.createResponse(null, "Content-type is not valid ("+ e.getMessage() +")");

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@WrapInTransaction
	private Tuple2<ContentType, List<SystemActionWorkflowActionMapping>> saveContentTypeAndDependencies (final ContentType contentType,
																								   final User user,
																								   final Set<String> workflowsIds,
																								   final List<Tuple2<WorkflowAPI.SystemAction,String>> systemActionMappings,
																								   final ContentTypeAPI contentTypeAPI,
																								   final boolean isNew) throws DotSecurityException, DotDataException {

		final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappings = new ArrayList<>();
		final ContentType contentTypeSaved = contentTypeAPI.save(contentType);
		this.workflowHelper.saveSchemesByContentType(contentTypeSaved.id(), user, workflowsIds);
		if (UtilMethods.isSet(systemActionMappings)) {

			for (final Tuple2<WorkflowAPI.SystemAction,String> tuple2 : systemActionMappings) {

				final WorkflowAPI.SystemAction systemAction = tuple2._1;
				final String workflowActionId               = tuple2._2;
				if (UtilMethods.isSet(workflowActionId)) {

					Logger.warn(this, "Saving the system action: " + systemAction +
							", for content type: " + contentTypeSaved.variable() + ", with the workflow action: "
							+ workflowActionId );

					systemActionWorkflowActionMappings.add(this.workflowHelper.mapSystemActionToWorkflowAction(new WorkflowSystemActionForm.Builder()
							.systemAction(systemAction).actionId(workflowActionId)
							.contentTypeVariable(contentTypeSaved.variable()).build(), user));
				} else if (UtilMethods.isSet(systemAction)) {

					if (!isNew) {
						Logger.warn(this, "Deleting the system action: " + systemAction +
								", for content type: " + contentTypeSaved.variable());

						final SystemActionWorkflowActionMapping mappingDeleted =
								this.workflowHelper.deleteSystemAction(systemAction, contentTypeSaved, user);

						Logger.warn(this, "Deleted the system action mapping: " + mappingDeleted);
					}
				} else {

					throw new IllegalArgumentException("On System Action Mappings, a system action has been sent null or empty");
				}
			}
		}

		return Tuple.of(contentTypeSaved, systemActionWorkflowActionMappings);
	}

	@DELETE
	@Path("/id/{idOrVar}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response deleteType(@PathParam("idOrVar") final String idOrVar, @Context final HttpServletRequest req, @Context final HttpServletResponse res)
			throws DotDataException, JSONException {

		final InitDataObject initData = this.webResource.init(null, req, res, true, null);
		final User user = initData.getUser();

		ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);

		try {

			ContentType type = null;
			try {
				type = contentTypeAPI.find(idOrVar);
			} catch (NotFoundInDbException nfdb) {
				return Response.status(404).build();
			}

			contentTypeAPI.delete(type);

			JSONObject joe = new JSONObject();
			joe.put("deleted", type.id());

			Response response = Response.ok(new ResponseEntityView(joe.toString())).build();
			return response;

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
		} catch (Exception e) {
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Path("/id/{idOrVar}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response getType(
			@PathParam("idOrVar") final String idOrVar,
			@Context final HttpServletRequest req,
			@Context final HttpServletResponse res,
			@QueryParam("languageId") final Long languageId,
			@QueryParam("live") final Boolean paramLive)
			throws DotDataException {

		final InitDataObject initData = this.webResource.init(null, req, res, false, null);
		final User user = initData.getUser();
		ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
		Response response = Response.status(404).build();
		final Map<String, Object> resultMap = new HashMap<>();
		final HttpSession session = req.getSession(false);
		try {

			Logger.debug(this, ()-> "Getting the Type: " + idOrVar);

			final ContentType type = tapi.find(idOrVar);

			if(null != session && null != type){
				session.setAttribute(SELECTED_STRUCTURE_KEY, type.inode());
			}

			final boolean live = paramLive == null ?
					(PageMode.get(Try.of(() -> HttpServletRequestThreadLocal.INSTANCE.getRequest()).getOrNull())).showLive
					: paramLive;

			final ContentTypeInternationalization contentTypeInternationalization = languageId != null ?
					new ContentTypeInternationalization(languageId, live, user) : null;
			resultMap.putAll(new JsonContentTypeTransformer(type, contentTypeInternationalization).mapObject());

			resultMap.put("workflows", this.workflowHelper.findSchemesByContentType(type.id(), initData.getUser()));
			resultMap.put("systemActionMappings",
					this.workflowHelper.findSystemActionsByContentType(type, initData.getUser())
							.stream().collect(Collectors.toMap(mapping -> mapping.getSystemAction(),mapping -> mapping)));

			response = ("true".equalsIgnoreCase(req.getParameter("include_permissions")))?
					Response.ok(new ResponseEntityView(resultMap, PermissionsUtil.getInstance().getPermissionsArray(type, initData.getUser()))).build():
					Response.ok(new ResponseEntityView(resultMap)).build();
		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
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
			final List<BaseContentTypesView> types = contentTypeHelper.getTypes(request);
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
	 *     <li>filter: just return ContentTypes who content this pattern</li>
	 *     <li>n-limit: limit of items to return</li>
	 *     <li>n-offset: offset</li>
	 *     <li>fieldname: field to order by</li>
	 *     <li>order_direction: asc for upward order and desc for downward order</li>
	 * </ul>
	 *
	 * Url example: v1/contenttype?query=New%20L&limit=4&offset=5&orderby=name-asc
	 *
	 * @param httpRequest
	 * @return
	 */
	@GET
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getContentTypes(@Context final HttpServletRequest httpRequest,
										  @Context final HttpServletResponse httpResponse,
										  @QueryParam(PaginationUtil.FILTER)   final String filter,
										  @QueryParam(PaginationUtil.PAGE) final int page,
										  @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
										  @DefaultValue("upper(name)") @QueryParam(PaginationUtil.ORDER_BY) String orderbyParam,
										  @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) String direction,
										  @QueryParam("type") String types) throws DotDataException {

		final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true, null);

		Response response = null;

		final String orderBy = getOrderByRealName(orderbyParam);
		final User user = initData.getUser();

		try {

			final Map<String, Object> extraParams = types == null ? Collections.EMPTY_MAP :
					ImmutableMap.<String, Object>builder()
							.put(ContentTypesPaginator.TYPE_PARAMETER_NAME, Arrays.asList(types.split(",")))
							.build();

			final PaginationUtil paginationUtil = new PaginationUtil(new ContentTypesPaginator(APILocator.getContentTypeAPI(user)));

			response = paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy,
					OrderDirection.valueOf(direction), extraParams);
		} catch (IllegalArgumentException e) {
			throw new DotDataException(e.getMessage());
		} catch (Exception e) {
			if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
				throw new ForbiddenException(e);
			}
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
