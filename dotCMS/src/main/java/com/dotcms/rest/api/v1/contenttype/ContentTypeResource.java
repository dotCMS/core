package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.CopyContentTypeBean;
import com.dotcms.contenttype.business.FieldDiffCommand;
import com.dotcms.contenttype.business.FieldDiffItemsKey;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
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
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.diff.DiffItem;
import com.dotcms.util.diff.DiffResult;
import com.dotcms.util.pagination.ContentTypesPaginator;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.workflow.form.WorkflowSystemActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.COMMA;

/**
 * This REST Endpoint provides information related to Content Types in the current dotCMS repository.
 *
 * @author Will Ezell
 * @since Sep 11th, 2016
 */
@Path("/v1/contenttype")
@Tag(name = "Content Type")
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
	@Path("/{baseVariableName}/_copy")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response copyType(@Context final HttpServletRequest req,
								   @Context final HttpServletResponse res,
								   @PathParam("baseVariableName") final String baseVariableName,
								   final CopyContentTypeForm copyContentTypeForm) {

		final InitDataObject initData = this.webResource.init(null, req, res, true, null);
		final User user = initData.getUser();
		Response response = null;

		try {

			if (null == copyContentTypeForm) {

				return ExceptionMapperUtil.createResponse(null, "The Request needs a POST body");
			}

			Logger.debug(this, ()->String.format("Creating new content type '%s' based from  '%s' ", baseVariableName,  copyContentTypeForm.getName()));
			final HttpSession session = req.getSession(false);

			// Validate input
			final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);
			final ContentType type = contentTypeAPI.find(baseVariableName);

			if (null == type || (UtilMethods.isSet(type.id()) && !UUIDUtil.isUUID(type.id()))) {

				return ExceptionMapperUtil.createResponse(null, "ContentType 'id' if set, should be a uuid");
			}

			final ImmutableMap<Object, Object> responseMap = this.copyContentTypeAndDependencies(contentTypeAPI, type, copyContentTypeForm, user);

			// save the last one to the session to be compliant with #13719
			if(null != session) {
				session.removeAttribute(SELECTED_STRUCTURE_KEY);
			}

			response = Response.ok(new ResponseEntityView(responseMap)).build();
		} catch (IllegalArgumentException e) {
			Logger.error(this, e.getMessage(), e);
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

	@VisibleForTesting
	public static void setHostAndFolderAsIdentifer (final String folderPathOrIdentifier, final String hostOrId, final User user, final CopyContentTypeBean.Builder builder) {

		Host site = APILocator.systemHost();
		if (null != hostOrId) {

			if (Host.SYSTEM_HOST.equals(hostOrId)) {

				site = APILocator.systemHost();
			} else {

				site = Try.of(() -> UUIDUtil.isUUID(hostOrId) ? APILocator.getHostAPI().find(hostOrId, user, false) :
						APILocator.getHostAPI().findByName(hostOrId, user, false)).getOrElse(APILocator.systemHost());
			}

			builder.host(null == site? APILocator.systemHost().getIdentifier():site.getIdentifier());
		}

		if (null != folderPathOrIdentifier) {

			final Host finalSite = site;
			final String folderId =
					Try.of(() -> APILocator.getFolderAPI().findFolderByPath(folderPathOrIdentifier, finalSite, user, false).getIdentifier()).getOrNull();

			builder.folder(null != folderId ? folderId : folderPathOrIdentifier);
		}
	}

	@WrapInTransaction
	private ImmutableMap<Object, Object> copyContentTypeAndDependencies (final ContentTypeAPI contentTypeAPI, final ContentType type,
																		 final CopyContentTypeForm copyContentTypeForm, final User user)
			throws DotDataException, DotSecurityException {

		final CopyContentTypeBean.Builder builder = new CopyContentTypeBean.Builder()
				.sourceContentType(type).icon(copyContentTypeForm.getIcon()).name(copyContentTypeForm.getName())
				.newVariable(copyContentTypeForm.getVariable());

		setHostAndFolderAsIdentifer(copyContentTypeForm.getFolder(), copyContentTypeForm.getHost(), user, builder);
		final ContentType contentTypeSaved = contentTypeAPI.copyFrom(builder.build());

		// saving the workflow information
		final List<WorkflowScheme> workflowSchemes = this.workflowHelper.findSchemesByContentType(type.id(), user);
		final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappings = this.workflowHelper.findSystemActionsByContentType(type, user);

		this.workflowHelper.saveSchemesByContentType(contentTypeSaved.id(), user, workflowSchemes.stream().map(WorkflowScheme::getId).collect(Collectors.toSet()));
		for (final SystemActionWorkflowActionMapping systemActionWorkflowActionMapping : systemActionWorkflowActionMappings) {

			this.workflowHelper.mapSystemActionToWorkflowAction(new WorkflowSystemActionForm.Builder()
					.systemAction(systemActionWorkflowActionMapping.getSystemAction())
					.actionId(systemActionWorkflowActionMapping.getWorkflowAction().getId())
					.contentTypeVariable(contentTypeSaved.variable()).build(), user);
		}

		return ImmutableMap.builder()
				.putAll(new JsonContentTypeTransformer(contentTypeAPI.find(contentTypeSaved.variable())).mapObject())
				.put("workflows", this.workflowHelper.findSchemesByContentType(contentTypeSaved.id(), user))
				.put("systemActionMappings", this.workflowHelper.findSystemActionsByContentType(contentTypeSaved, user).stream()
						.collect(Collectors.toMap(mapping-> mapping.getSystemAction(), mapping->mapping)))
				.build();
	}

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

			response = Response.ok(new ResponseEntityView<>(retTypes)).build();
		} catch (IllegalArgumentException e) {
			Logger.error(this, e.getMessage(), e);
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
			final ContentType contentType = form.getContentType();

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
							.putAll(new JsonContentTypeTransformer(contentTypeAPI.find(tuple2._1.variable())).mapObject())
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

		if (!isNew) {
			this.handleFields(contentType, user, contentTypeAPI);
		}

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

	/**
	 * We need to handle in this way b/c when the content type exists the fields are not being updated
	 * @param newContentType
	 * @param user
	 * @param contentTypeAPI
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@WrapInTransaction
	private void handleFields(final ContentType newContentType, final User user, final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

		final ContentType currentContentType = contentTypeAPI.find(newContentType.id());

		final DiffResult<FieldDiffItemsKey, Field> diffResult = new FieldDiffCommand().applyDiff(currentContentType.fieldMap(), newContentType.fieldMap());

		if (!diffResult.getToDelete().isEmpty()) {

			APILocator.getContentTypeFieldLayoutAPI().deleteField(currentContentType, diffResult.getToDelete().
					values().stream().map(Field::id).collect(Collectors.toList()), user);
		}

		if (!diffResult.getToAdd().isEmpty()) {

			APILocator.getContentTypeFieldAPI().saveFields(new ArrayList<>(diffResult.getToAdd().values()), user);
		}

		if (!diffResult.getToUpdate().isEmpty()) {

			handleUpdateFieldAndFieldVariables(user, diffResult);
		}
	}

	private void handleUpdateFieldAndFieldVariables(final User user,
													final DiffResult<FieldDiffItemsKey, Field> diffResult) throws DotSecurityException, DotDataException {

		final List<Field> fieldToUpdate = new ArrayList<>();
		final List<Tuple2<Field, List<DiffItem>>> fieldVariableToUpdate = new ArrayList<>();

		for (final Map.Entry<FieldDiffItemsKey, Field> entry : diffResult.getToUpdate().entrySet()) {

			final Map<Boolean, List<DiffItem>> diffPartition = // split the differences between the ones that are for the field and the ones that are for field variables
					entry.getKey().getDiffItems().stream().collect(Collectors.partitioningBy(diff -> diff.getVariable().startsWith("fieldVariable.")));
			final List<DiffItem> fieldVariableList = diffPartition.get(Boolean.TRUE);  // field variable diffs
			final List<DiffItem> fieldList         = diffPartition.get(Boolean.FALSE); // field diffs
			if (UtilMethods.isSet(fieldList)) {
				Logger.debug(this, "Updating the field : " + entry.getValue().variable() + " diff: " + fieldList);
				fieldToUpdate.add(entry.getValue());
			}

			if (UtilMethods.isSet(fieldVariableList)) {
				Logger.debug(this, "Updating the field - field Variables : " + entry.getValue().variable() + " diff: " + fieldVariableList);
				fieldVariableToUpdate.add(Tuple.of(entry.getValue(), fieldVariableList));
			}
		}

		if (UtilMethods.isSet(fieldToUpdate)) { // any diff on fields, so update the fields (but not update field variables :( )
			APILocator.getContentTypeFieldAPI().saveFields(fieldToUpdate, user);
		}

		if (UtilMethods.isSet(fieldVariableToUpdate)) { // any diff on field variables, lets see what kind of diffs are.

			for (final Tuple2<Field, List<DiffItem>> fieldVariableTuple : fieldVariableToUpdate) {

				final Map<String, FieldVariable>  fieldVariableMap = fieldVariableTuple._1().fieldVariablesMap();
				for (final DiffItem diffItem : fieldVariableTuple._2()) {

					// normalizing the real varname
					final String fieldVariableVarName = StringUtils.replace(diffItem.getVariable(), "fieldVariable.", StringPool.BLANK);
					if ("delete".equals(diffItem.getDetail()) && fieldVariableMap.containsKey(fieldVariableVarName)) {

						APILocator.getContentTypeFieldAPI().delete(fieldVariableMap.get(fieldVariableVarName));
					}

					// if add or update, it is pretty much the same
					if ("add".equals(diffItem.getDetail()) || "update".equals(diffItem.getDetail())) {

						if ("update".equals(diffItem.getDetail()) && !fieldVariableMap.containsKey(fieldVariableVarName)) {
							// on update get the current field and gets the id
							continue;
						}

						APILocator.getContentTypeFieldAPI().save(fieldVariableMap.get(fieldVariableVarName), user);
					}
				}
			}
		}
	} // handleUpdateFieldAndFieldVariables.

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

	/**
	 * Returns the list of Content Type objects that match the specified filter and the optional pagination criteria.
	 * <p>Example:</p>
	 * <pre>
	 * {@code
	 *     {{serverURL}}/api/v1/contenttype/_filter
	 * }
	 * </pre>
	 * JSON body:
	 * <pre>
	 * {@code
	 *     {
	 *         "filter" : {
	 *             "data" : "calendarEvent,Vanityurl,webPageContent,DotAsset,persona",
	 *             "query": ""
	 *         },
	 *         "page": 0,
	 *         "perPage": 5
	 *     }
	 * }
	 * </pre>
	 *
	 * @param req  The current {@link HttpServletRequest} instance.
	 * @param res  The current {@link HttpServletResponse} instance.
	 * @param form The {@link FilteredContentTypesForm} containing the required information and optional pagination
	 *             parameters.
	 *
	 * @return The JSON response with the Content Types matching the specified Velocity Variable Names.
	 */
	@POST
	@Path("/_filter")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response filteredContentTypes(@Context final HttpServletRequest req,
											   @Context final HttpServletResponse res,
											   final FilteredContentTypesForm form) {
		if (null == form) {
			return ExceptionMapperUtil.createResponse(null, "Requests to '_filter' need a POST JSON body");
		}
		final InitDataObject initData = this.webResource.init(null, req, res, true, null);
		final User user = initData.getUser();
		Response response;
		final String types = getFilterValue(form, "types", StringPool.BLANK);
		final List<String> typeVarNames = UtilMethods.isSet(types) ? Arrays.asList(types.split(COMMA)) : null;
		final String filter = getFilterValue(form, "query", StringPool.BLANK);
		final Map<String, Object> extraParams = new HashMap<>();
		if (UtilMethods.isSet(typeVarNames)) {
			extraParams.put(ContentTypesPaginator.TYPES_PARAMETER_NAME, typeVarNames);
		}
		try {
			final PaginationUtil paginationUtil =
					new PaginationUtil(new ContentTypesPaginator(APILocator.getContentTypeAPI(user)));
			response = paginationUtil.getPage(req, user, filter, form.getPage(), form.getPerPage(), form.getOrderBy(),
					OrderDirection.valueOf(form.getDirection()), extraParams);
		} catch (final Exception e) {
			if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
				throw new ForbiddenException(e);
			}
			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
			Logger.error(this, e.getMessage(), e);
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
			response = Response.ok(new ResponseEntityView<>(types)).build();
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
										  @QueryParam("type") String types,
										  @QueryParam(ContentTypesPaginator.HOST_PARAMETER_ID) final String hostId) throws DotDataException {

		final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true, null);

		Response response = null;

		final String orderBy = getOrderByRealName(orderbyParam);
		final User user = initData.getUser();

		try {

			final Map<String, Object> extraParams = new HashMap<>();
			if(null!=types) {
				extraParams.put(ContentTypesPaginator.TYPE_PARAMETER_NAME,
						Arrays.asList(types.split(",")));
			}

			if(null!=hostId){
				extraParams.put(ContentTypesPaginator.HOST_PARAMETER_ID,hostId);
			}


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

	/**
	 * Utility method used to return a specific parameter from the Content Type Filtering Form. If not present, a
	 * default value will be returned.
	 *
	 * @param form         The {@link FilteredContentTypesForm} in the request.
	 * @param param        The parameter being requested.
	 * @param defaultValue The default value in case the parameter is not present or set.
	 *
	 * @return The form parameter or the specified default value.
	 */
	private <T> T getFilterValue(final FilteredContentTypesForm form, final String param, T defaultValue) {
		if (null == form || null == form.getFilter() || form.getFilter().isEmpty()) {
			return defaultValue;
		}
		return UtilMethods.isSet(form.getFilter().get(param)) ? (T) form.getFilter().get(param) : defaultValue;
	}

}
