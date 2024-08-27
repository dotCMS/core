package com.dotcms.rest.api.v1.contenttype;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;
import static com.liferay.util.StringPool.COMMA;

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
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
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
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.JSONP;

/**
 * This REST Endpoint provides information related to Content Types in the current dotCMS repository.
 *
 * @author Will Ezell
 * @since Sep 11th, 2016
 */
@Path("/v1/contenttype")
@Tag(name = "Content Type",
		description = "Endpoints that perform operations related to content types.",
		externalDocs = @ExternalDocumentation(description = "Additional Content Type API information",
				url = "https://www.dotcms.com/docs/latest/content-type-api")
)
public class ContentTypeResource implements Serializable {

	private static final String MAP_KEY_WORKFLOWS = "workflows";
	private static final String MAP_KEY_SYSTEM_ACTION_MAPPINGS = "systemActionMappings";

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
	@Operation(
			operationId = "postContentTypeCopy",
			summary = "Copies a content type",
			description = "Creates a new content type by copying an existing one.\n\nReturns resulting content type.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Content type copied successfully",
							content = @Content(mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"errors\": [\n" +
															"    {\n" +
															"      \"errorCode\": \"string\",\n" +
															"      \"message\": \"string\",\n" +
															"      \"fieldName\": \"string\"\n" +
															"    }\n" +
															"  ],\n" +
															"  \"entity\": {\n" +
															"    \"baseType\": \"string\",\n" +
															"    \"clazz\": \"string\",\n" +
															"    \"defaultType\": true,\n" +
															"    \"description\": \"string\",\n" +
															"    \"fields\": [],\n" +
															"    \"fixed\": true,\n" +
															"    \"folder\": \"string\",\n" +
															"    \"folderPath\": \"string\",\n" +
															"    \"host\": \"string\",\n" +
															"    \"iDate\": 0,\n" +
															"    \"icon\": \"string\",\n" +
															"    \"id\": \"string\",\n" +
															"    \"layout\": [],\n" +
															"    \"metadata\": {},\n" +
															"    \"modDate\": 0,\n" +
															"    \"multilingualable\": true,\n" +
															"    \"name\": \"string1\",\n" +
															"    \"siteName\": \"string\",\n" +
															"    \"sortOrder\": 0,\n" +
															"    \"system\": true,\n" +
															"    \"systemActionMappings\": {},\n" +
															"    \"variable\": \"string\",\n" +
															"    \"versionable\": true,\n" +
															"    \"workflows\": []\n" +
															"  },\n" +
															"  \"messages\": [\n" +
															"    {\n" +
															"      \"message\": \"string\"\n" +
															"    }\n" +
															"  ],\n" +
															"  \"i18nMessagesMap\": {\n" +
															"    \"additionalProp1\": \"string\",\n" +
															"    \"additionalProp2\": \"string\",\n" +
															"    \"additionalProp3\": \"string\"\n" +
															"  },\n" +
															"  \"permissions\": [\n" +
															"    \"string\"\n" +
															"  ],\n" +
															"  \"pagination\": {\n" +
															"    \"currentPage\": 0,\n" +
															"    \"perPage\": 0,\n" +
															"    \"totalEntries\": 0\n" +
															"  }\n" +
															"}"
											)
									}
							)
					),
					@ApiResponse(responseCode = "400", description = "Bad Request"),
					@ApiResponse(responseCode = "403", description = "Forbidden"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public final Response copyType(@Context final HttpServletRequest req,
								   @Context final HttpServletResponse res,
								   @PathParam("baseVariableName") @Parameter(
										   required = true,
										   description = "The variable name of the content type to copy.\n\n" +
														 "Example value: `htmlpageasset` (Default page content type)",
										   schema = @Schema(type = "string")
								   ) final String baseVariableName,
								   @RequestBody(
										   description = "Requires POST body consisting of a JSON object with the following properties:\n\n" +
														 "| Property |  Type  | Description |\n" +
														 "|----------|--------|-------------|\n" +
														 "| `name`   | String | **Required.** Name of new content type |\n" +
														 "| `variable` | String | System variable of new content type |\n" +
														 "| `folder`   | String | Folder in which new content type will live |\n" +
														 "| `host`   | String | Site or host to which the new content type will belong |\n" +
														 "| `icon`   | String | System icon to represent content type |\n\n" +
														 "Values not specified default to values of the original content type.",
										   required = true,
										   content = @Content(
												   schema = @Schema(implementation = CopyContentTypeForm.class),
												   examples = {
														   @ExampleObject(
																   value = "{\n" +
																		   "  \"name\": \"Copied Content Type Name\",\n" +
																		   "  \"variable\": \"copiedContentTypeVar\",\n" +
																		   "  \"folder\": \"SYSTEM_FOLDER\",\n" +
																		   "  \"host\": \"8a7d5e23-da1e-420a-b4f0-471e7da8ea2d\",\n" +
																		   "  \"icon\": \"event_note\"\n" +
																		   "}"
														   )
												   }
										   )
								   ) final CopyContentTypeForm copyContentTypeForm) {

		final InitDataObject initData = this.webResource.init(null, req, res, true, null);
		final User user = initData.getUser();
		Response response;

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

			response = Response.ok(new ResponseEntityView<>(responseMap)).build();
		} catch (final IllegalArgumentException e) {
			final String errorMsg = String.format("Missing required information when copying Content Type " +
					"'%s': %s", baseVariableName, ExceptionUtil.getErrorMessage(e));
			Logger.error(this, errorMsg, e);
			response = ExceptionMapperUtil.createResponse(null, errorMsg);
		} catch (final DotStateException | DotDataException e) {
			final String errorMsg = String.format("Failed to copy Content Type '%s': %s",
					baseVariableName, ExceptionUtil.getErrorMessage(e));
			Logger.error(this, errorMsg, e);
			response = ExceptionMapperUtil.createResponse(null, errorMsg);
		} catch (final DotSecurityException e) {
			Logger.error(this, String.format("User '%s' does not have permission to copy Content Type " +
					"'%s'", user.getUserId(), baseVariableName), e);
			throw new ForbiddenException(e);
		} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when copying Content Type " +
					"'%s': %s", baseVariableName, ExceptionUtil.getErrorMessage(e));
			Logger.error(this, errorMsg, e);
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

	/**
	 * Copies a Content Type -- along with the new information specified for it -- as well as the
	 * references to the Workflow Schemes that the original type is using.
	 *
	 * @param contentTypeAPI      The {@link ContentTypeAPI} instance to use to save the new
	 *                            Content Type.
	 * @param type                The original {@link ContentType} to copy.
	 * @param copyContentTypeForm The {@link CopyContentTypeForm} containing the new information
	 *                            for the copied type, such as, the new name, new icon, and new
	 *                            Velocity Variable Name.
	 * @param user                The {@link User} executing this action.
	 *
	 * @return An {@link ImmutableMap} containing the data from the new Content Type, its Workflow
	 * Schemes and system action mappings.
	 *
	 * @throws DotDataException     An error occurred when saving the new information.
	 * @throws DotSecurityException The specified User doesn't have the required permissions to
	 *                              perform this action.
	 */
	@WrapInTransaction
	private ImmutableMap<Object, Object> copyContentTypeAndDependencies(final ContentTypeAPI contentTypeAPI, final ContentType type,
																		 final CopyContentTypeForm copyContentTypeForm, final User user)
			throws DotDataException, DotSecurityException {

		final CopyContentTypeBean.Builder builder = new CopyContentTypeBean.Builder()
				.sourceContentType(type).icon(copyContentTypeForm.getIcon()).name(copyContentTypeForm.getName())
				.newVariable(copyContentTypeForm.getVariable());

		setHostAndFolderAsIdentifer(copyContentTypeForm.getFolder(), copyContentTypeForm.getHost(), user, builder);
		final ContentType contentTypeSaved = contentTypeAPI.copyFromAndDependencies(builder.build());

		return ImmutableMap.builder()
				.putAll(contentTypeHelper.contentTypeToMap(
						contentTypeAPI.find(contentTypeSaved.variable()), user))
				.put(MAP_KEY_WORKFLOWS,
						this.workflowHelper.findSchemesByContentType(contentTypeSaved.id(), user))
				.put(MAP_KEY_SYSTEM_ACTION_MAPPINGS,
						this.workflowHelper.findSystemActionsByContentType(contentTypeSaved, user)
								.stream()
						.collect(Collectors.toMap(SystemActionWorkflowActionMapping::getSystemAction, mapping->mapping)))
				.build();
	}

	/**
	 * Creates one or more Content Types specified in the Content Type Form parameter. This allows
	 * users to easily create more than one Content Type in a single request.
	 *
	 * @param req  The current instance of the {@link HttpServletRequest}.
	 * @param res  The current instance of the {@link HttpServletResponse}.
	 * @param form The {@link ContentTypeForm} containing the required information to create the
	 *             Content Type(s).
	 *
	 * @return The JSON response with the Content Type(s) created.
	 *
	 * @throws DotDataException An error occurs when persisting the Content Type(s) in the
	 *                          database.
	 */
	@POST
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	@Operation(
			operationId = "postContentTypeCreate",
			summary = "Creates one or more content types",
			description = "Creates one or more content types specified in the JSON payload.\n\n " +
						  "Returns a list entity containing the created content type objects.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Content type(s) created successfully",
							content = @Content(mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"entity\": [\n" +
															"    {\n" +
															"      \"baseType\": \"string\",\n" +
															"      \"clazz\": \"string\",\n" +
															"      \"defaultType\": true,\n" +
															"      \"description\": \"string\",\n" +
															"      \"fields\": [],\n" +
															"      \"fixed\": true,\n" +
															"      \"folder\": \"string\",\n" +
															"      \"folderPath\": \"string\",\n" +
															"      \"host\": \"string\",\n" +
															"      \"iDate\": 0,\n" +
															"      \"icon\": \"string\",\n" +
															"      \"id\": \"string\",\n" +
															"      \"layout\": [],\n" +
															"      \"metadata\": {},\n" +
															"      \"modDate\": 0,\n" +
															"      \"multilingualable\": true,\n" +
															"      \"name\": \"string\",\n" +
															"      \"owner\": \"string\",\n" +
															"      \"siteName\": \"string\",\n" +
															"      \"sortOrder\": 0,\n" +
															"      \"system\": true,\n" +
															"      \"systemActionMappings\": {},\n" +
															"      \"variable\": \"string\",\n" +
															"      \"versionable\": true,\n" +
															"      \"workflows\": []\n" +
															"    }\n" +
															"  ],\n" +
															"  \"errors\": [],\n" +
															"  \"i18nMessagesMap\": {},\n" +
															"  \"messages\": [],\n" +
															"  \"pagination\": null,\n" +
															"  \"permissions\": []\n" +
															"}"
											)
									}
							)
					),
					@ApiResponse(responseCode = "400", description = "Bad Request"),
					@ApiResponse(responseCode = "403", description = "Forbidden"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public final Response createType(@Context final HttpServletRequest req,
									 @Context final HttpServletResponse res,
									 @RequestBody(
											 description = "Payload may consist of a single content type JSON object, or a list " +
														   "containing multiple content type objects.\n\n" +
														   "Objects require `clazz` and `name` properties at minimum.\n\n" +
														   "May optionally include the following special properties:\n\n" +
														   "| Property | Value | Description |\n" +
														   "|-|-|-|\n" +
														   "| `systemActionMappings` | JSON Object | Maps " +
														   "[Default Workflow Actions](https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) (as keys) " +
														   "to workflow action identifiers (as values) for this content type.|\n" +
														   "| `workflow` | List of Strings | A list of identifiers of workflow schemes to be associated with the content type.",
											 required = true,
											 content = @Content(
													 schema = @Schema(implementation = ContentTypeForm.class),
													 examples = {
															 @ExampleObject(
																	 value = "[\n" +
																			 "  {\n" +
																			 "    \"clazz\": \"com.dotcms.contenttype.model.type.ImmutableSimpleContentType\",\n" +
																			 "    \"defaultType\": false,\n" +
																			 "    \"name\": \"The Content Type 1\",\n" +
																			 "    \"description\": \"THE DESCRIPTION\",\n" +
																			 "    \"host\": \"48190c8c-42c4-46af-8d1a-0cd5db894797\",\n" +
																			 "    \"owner\": \"dotcms.org.1\",\n" +
																			 "    \"variable\": \"TheContentType1\",\n" +
																			 "    \"fixed\": false,\n" +
																			 "    \"system\": false,\n" +
																			 "    \"folder\": \"SYSTEM_FOLDER\",\n" +
																			 "    \"systemActionMappings\": {\n" +
																			 "      \"NEW\": \"ceca71a0-deee-4999-bd47-b01baa1bcfc8\",\n" +
																			 "      \"PUBLISH\": \"ceca71a0-deee-4999-bd47-b01baa1bcfc8\"\n" +
																			 "    },\n" +
																			 "    \"workflow\": [\n" +
																			 "      \"d61a59e1-a49c-46f2-a929-db2b4bfa88b2\"\n" +
																			 "    ]\n" +
																			 "  },\n" +
																			 "  {\n" +
																			 "    \"clazz\": \"com.dotcms.contenttype.model.type.ImmutableSimpleContentType\",\n" +
																			 "    \"defaultType\": false,\n" +
																			 "    \"name\": \"The Content Type 2\",\n" +
																			 "    \"description\": \"THE DESCRIPTION\",\n" +
																			 "    \"host\": \"48190c8c-42c4-46af-8d1a-0cd5db894797\",\n" +
																			 "    \"owner\": \"dotcms.org.1\",\n" +
																			 "    \"variable\": \"TheContentType2\",\n" +
																			 "    \"fixed\": false,\n" +
																			 "    \"system\": false,\n" +
																			 "    \"folder\": \"SYSTEM_FOLDER\",\n" +
																			 "    \"workflow\": [\n" +
																			 "      \"d61a59e1-a49c-46f2-a929-db2b4bfa88b2\"\n" +
																			 "    ]\n" +
																			 "  }\n" +
																			 "]"
															 )
													 }
											 )
									 ) final ContentTypeForm form)
			throws DotDataException {
		final InitDataObject initData =
				new WebResource.InitBuilder(webResource)
						.requestAndResponse(req, res)
						.requiredBackendUser(false)
						.requiredFrontendUser(false)
						.rejectWhenNoUser(true)
						.init();
		final User user = initData.getUser();
		try {
			checkNotNull(form, "The 'form' parameter is required");
			Logger.debug(this, ()->String.format("Creating Content Type(s): %s", form.getRequestJson()));
			final HttpSession session = req.getSession(false);
			final Iterable<ContentTypeForm.ContentTypeFormEntry> typesToSave = form.getIterable();
			final List<Map<Object, Object>> savedContentTypes = new ArrayList<>();

			for (final ContentTypeForm.ContentTypeFormEntry entry : typesToSave) {
				final ContentType type = contentTypeHelper.evaluateContentTypeRequest(
						entry.contentType, user, true
				);

				if (UtilMethods.isSet(type.id()) && !UUIDUtil.isUUID(type.id())) {
					return ExceptionMapperUtil.createResponse(null, String.format("Content Type ID " +
							"'%s' is either not set, or is not a valid UUID", type.id()));
				}

				final Tuple2<ContentType, List<SystemActionWorkflowActionMapping>>  tuple2 =
						this.saveContentTypeAndDependencies(type, initData.getUser(),
								entry.workflows,
							form.getSystemActions(), APILocator.getContentTypeAPI(user, true), true);
				final ContentType contentTypeSaved = tuple2._1;
				final ImmutableMap<Object, Object> responseMap = ImmutableMap.builder()
						.putAll(contentTypeHelper.contentTypeToMap(contentTypeSaved, user))
						.put(MAP_KEY_WORKFLOWS,
								this.workflowHelper.findSchemesByContentType(contentTypeSaved.id(),
										initData.getUser()))
						.put(MAP_KEY_SYSTEM_ACTION_MAPPINGS, tuple2._2.stream()
								.collect(Collectors.toMap(SystemActionWorkflowActionMapping::getSystemAction, mapping->mapping)))
						.build();
				savedContentTypes.add(responseMap);
				// save the last one to the session to be compliant with #13719
				if(null != session){
                  session.removeAttribute(SELECTED_STRUCTURE_KEY);
				}
			}
			return Response.ok(new ResponseEntityView<>(savedContentTypes)).build();
		} catch (final IllegalArgumentException e) {
			final String errorMsg = String.format("Missing required information when creating Content Type(s): " +
					"%s", ExceptionUtil.getErrorMessage(e));
			Logger.error(this, errorMsg, e);
			return ExceptionMapperUtil.createResponse(null, errorMsg);
		}catch (final DotStateException | DotDataException e) {
			final String errorMsg = String.format("Failed to create Content Type(s): %s", ExceptionUtil.getErrorMessage(e));
			Logger.error(this, errorMsg, e);
			return ExceptionMapperUtil.createResponse(null, errorMsg);
		} catch (final DotSecurityException e) {
			Logger.error(this, String.format("User '%s' does not have permission to create " +
					"Content Type(s)", user.getUserId()), e);
			throw new ForbiddenException(e);
		} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when creating Content Type(s): " +
					"%s", ExceptionUtil.getErrorMessage(e));
			Logger.error(this, errorMsg, e);
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Updates the Content Type based on the given ID or Velocity variable name.
	 *
	 * @param idOrVar The ID or Velocity variable name of the Content Type to update.
	 * @param form    The {@link ContentTypeForm} containing the required information to update the
	 *                Content Type.
	 * @param req     The current instance of the {@link HttpServletRequest}.
	 * @param res     The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The JSON response with the updated information of the Content Type.
	 */
	@PUT
	@Path("/id/{idOrVar}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	@Operation(
			operationId = "putContentTypeUpdate",
			summary = "Updates a content type",
			description = "Updates the content type based on the given ID or Velocity variable name.\n\n" +
					"Returns a copy of the updated content type object.\n\n" +
					"> **Caution:** When updating a content type, any editable fields omitted from the request body " +
					"will be removed from the content type. To update selected properties without deleting others," +
					"submit the full JSON entity with the desired items edited.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Content type updated successfully",
							content = @Content(mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"entity\": {\n" +
															"    \"baseType\": \"string\",\n" +
															"    \"clazz\": \"string\",\n" +
															"    \"defaultType\": true,\n" +
															"    \"fields\": [],\n" +
															"    \"fixed\": true,\n" +
															"    \"folder\": \"string\",\n" +
															"    \"folderPath\": \"string\",\n" +
															"    \"host\": \"string\",\n" +
															"    \"iDate\": 0,\n" +
															"    \"icon\": \"string\",\n" +
															"    \"id\": \"string\",\n" +
															"    \"layout\": [],\n" +
															"    \"metadata\": {},\n" +
															"    \"modDate\": 0,\n" +
															"    \"multilingualable\": true,\n" +
															"    \"name\": \"string\",\n" +
															"    \"siteName\": \"string\",\n" +
															"    \"sortOrder\": 0,\n" +
															"    \"system\": true,\n" +
															"    \"systemActionMappings\": {},\n" +
															"    \"variable\": \"string\",\n" +
															"    \"versionable\": true,\n" +
															"    \"workflows\": []\n" +
															"  },\n" +
															"  \"errors\": [],\n" +
															"  \"i18nMessagesMap\": {},\n" +
															"  \"messages\": [],\n" +
															"  \"pagination\": null,\n" +
															"  \"permissions\": []\n" +
															"}"
											)
									}
							)
					),
					@ApiResponse(responseCode = "400", description = "Bad Request"),
					@ApiResponse(responseCode = "403", description = "Forbidden"),
					@ApiResponse(responseCode = "404", description = "Not Found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response updateType(@PathParam("idOrVar") @Parameter(
										required = true,
										description = "The ID or Velocity variable name of the content type to update.\n\n" +
												"Example value: `htmlpageasset` (Default page content type)",
										schema = @Schema(type = "string")
								) final String idOrVar,
								@RequestBody(
										description = "The minimum required properties for a successful update are " +
												"`clazz`, `id`, and `name`.\n\n" +
												"May also optionally include the following special properties:\n\n" +
												"| Property | Value | Description |\n" +
												"|-|-|-|\n" +
												"| `systemActionMappings` | JSON Object | Maps " +
												"[Default Workflow Actions](https://www.dotcms.com/docs/latest/managing-" +
												"workflows#DefaultActions) (as keys) " +
												"to workflow action identifiers (as values) for this content type.|\n" +
												"| `workflow` | List of Strings | A list of identifiers of workflow " +
												"schemes to be associated with the content type.",
										required = true,
										content = @Content(
												schema = @Schema(implementation = ContentTypeForm.class),
												examples = {
														@ExampleObject(
																value = "{\n" +
																		"  \"clazz\": \"com.dotcms.contenttype.model.type" +
																		".ImmutableSimpleContentType\",\n" +
																		"  \"defaultType\": false,\n" +
																		"  \"id\": \"39fecdb0-46cc-40a9-a056-f2e1a80ea78c\",\n" +
																		"  \"name\": \"The Content Type 2\",\n" +
																		"  \"description\": \"THE DESCRIPTION 2\",\n" +
																		"  \"host\": \"48190c8c-42c4-46af-8d1a-0cd5db894797\",\n" +
																		"  \"owner\": \"dotcms.org.1\",\n" +
																		"  \"variable\": \"TheContentType1\",\n" +
																		"  \"fixed\": false,\n" +
																		"  \"system\": false,\n" +
																		"  \"folder\": \"SYSTEM_FOLDER\",\n" +
																		"  \"workflow\": [\n" +
																		"    \"d61a59e1-a49c-46f2-a929-db2b4bfa88b2\"\n" +
																		"  ]\n" +
																		"}"
														)
												}
										)
								) final ContentTypeForm form,
								@Context final HttpServletRequest req, @Context final HttpServletResponse res) {
		final InitDataObject initData =
				new WebResource.InitBuilder(webResource)
						.requestAndResponse(req, res)
						.requiredBackendUser(false)
						.requiredFrontendUser(false)
						.rejectWhenNoUser(true)
						.init();
		final User user = initData.getUser();
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);
		try {
			checkNotNull(form, "The 'form' parameter is required");
			final ContentType contentType = contentTypeHelper.evaluateContentTypeRequest(
					idOrVar, form.getContentType(), user, false
			);
			Logger.debug(this, String.format("Updating content type: '%s'", form.getRequestJson()));
			checkNotEmpty(contentType.id(), BadRequestException.class,
					"Content Type 'id' attribute must be set");

			final Tuple2<ContentType, List<SystemActionWorkflowActionMapping>> tuple2 =
					this.saveContentTypeAndDependencies(contentType, user,
							form.getWorkflows(), form.getSystemActions(),
							contentTypeAPI, false);
			final ImmutableMap.Builder<Object, Object> builderMap =
					ImmutableMap.builder()
							.putAll(contentTypeHelper.contentTypeToMap(tuple2._1, user))
							.put(MAP_KEY_WORKFLOWS,
									this.workflowHelper.findSchemesByContentType(
											contentType.id(), initData.getUser()))
							.put(MAP_KEY_SYSTEM_ACTION_MAPPINGS, tuple2._2.stream()
									.collect(Collectors.toMap(
											SystemActionWorkflowActionMapping::getSystemAction,
											mapping -> mapping)));
			return Response.ok(new ResponseEntityView<>(builderMap.build())).build();
		} catch (final NotFoundInDbException e) {
			Logger.error(this, String.format("Content Type with ID or var name '%s' was not found", idOrVar), e);
			return ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
		} catch (final IllegalArgumentException e) {
			return ExceptionMapperUtil.createResponse(null, e.getMessage());
		} catch (final DotStateException | DotDataException e) {
			final String errorMsg = String.format("Failed to update Content Type with ID or var name " +
					"'%s': %s", idOrVar, ExceptionUtil.getErrorMessage(e));
			Logger.error(this, errorMsg, e);
			return ExceptionMapperUtil.createResponse(null, errorMsg);
		} catch (final DotSecurityException e) {
			Logger.error(this, String.format("User '%s' does not have permission to update Content Type with ID or var name " +
					"'%s'", user.getUserId(), idOrVar), e);
			throw new ForbiddenException(e);
		} catch (final Exception e) {
			Logger.error(this, String.format("An error occurred when updating Content Type with ID or var name " +
					"'%s': %s", idOrVar, ExceptionUtil.getErrorMessage(e)), e);
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Saves the specified Content Type and properly handles additional data associated to it, such
	 * as Workflow information.
	 *
	 * @param contentType          The {@link ContentType} to save.
	 * @param user                 The {@link User} executing this action.
	 * @param workflows            The {@link Set} of Workflow IDs to associate to the Content
	 *                             Type.
	 * @param systemActionMappings The {@link List} of {@link Tuple2} containing the
	 *                             {@link WorkflowAPI.SystemAction} and the {@link String}
	 *                             representing the Workflow Action ID.
	 * @param contentTypeAPI       The {@link ContentTypeAPI} instance to use.
	 * @param isNew                A {@link Boolean} indicating if the Content Type is new or not.
	 *
	 * @return A {@link Tuple2} containing the saved {@link ContentType} and the {@link List} of
	 * {@link SystemActionWorkflowActionMapping} associated to it.
	 *
	 * @throws DotSecurityException The specified User doesn't have the required permissions to
	 *                              perform this action.
	 * @throws DotDataException     An error occurs when persisting the Content Type in the
	 *                              database.
	 */
	@WrapInTransaction
	private Tuple2<ContentType, List<SystemActionWorkflowActionMapping>> saveContentTypeAndDependencies (final ContentType contentType,
																								   final User user,
																								   final List<WorkflowFormEntry> workflows,
																								   final List<Tuple2<WorkflowAPI.SystemAction,String>> systemActionMappings,
																								   final ContentTypeAPI contentTypeAPI,
																								   final boolean isNew) throws DotSecurityException, DotDataException {

		ContentType contentTypeSaved = contentTypeAPI.save(contentType);
		this.contentTypeHelper.saveSchemesByContentType(contentTypeSaved, workflows);

		if (!isNew) {
			this.handleFields(contentTypeSaved.id(), contentType.fieldMap(
					this.contentTypeHelper::generateFieldKey
			), user, contentTypeAPI);
		}

		// Make sure we have the correct layout for the content type
		contentTypeSaved = this.contentTypeHelper.fixLayoutIfNecessary(
				contentTypeSaved.id(), user
		);

		// Processing the content type action mappings
		final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappings =
				this.contentTypeHelper.processWorkflowActionMapping(
						contentTypeSaved, user, systemActionMappings, isNew
				);

		return Tuple.of(contentTypeSaved, systemActionWorkflowActionMappings);
	}

	/**
	 * We need to handle in this way b/c when the content type exists the fields are not being
	 * updated
	 *
	 * @param contentTypeId           the content type id
	 * @param newContentTypeFieldsMap the content type fields found in the request
	 * @param user                    the user performing the action
	 * @param contentTypeAPI          the content type api
	 * @throws DotDataException     if there is an error with the data
	 * @throws DotSecurityException if the user does not have the required permissions
	 */
	@WrapInTransaction
	private void handleFields(final String contentTypeId,
			final Map<String, Field> newContentTypeFieldsMap, final User user,
			final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

		final ContentType currentContentType = contentTypeAPI.find(contentTypeId);

		final DiffResult<FieldDiffItemsKey, Field> diffResult = new FieldDiffCommand(contentTypeId).
				applyDiff(
						currentContentType.fieldMap(this.contentTypeHelper::generateFieldKey),
						newContentTypeFieldsMap
				);

		if (!diffResult.getToDelete().isEmpty()) {
			APILocator.getContentTypeFieldAPI().deleteFields(
					diffResult.getToDelete().values().stream().
							map(Field::id).
							collect(Collectors.toList()), user
			);
		}

		if (!diffResult.getToAdd().isEmpty()) {
			APILocator.getContentTypeFieldAPI().saveFields(
					new ArrayList<>(diffResult.getToAdd().values()), user
			);
		}

		if (!diffResult.getToUpdate().isEmpty()) {
			handleUpdateFieldAndFieldVariables(user, diffResult);
		}
	}

	/**
	 * Handles the update of fields and field variables based on the difference result.
	 *
	 * @param user          The user performing the update.
	 * @param diffResult    The result of the field differences.
	 * @throws DotSecurityException If a security exception occurs.
	 * @throws DotDataException     If a data exception occurs.
	 */
	private void handleUpdateFieldAndFieldVariables(
			final User user, final DiffResult<FieldDiffItemsKey, Field> diffResult)
			throws DotSecurityException, DotDataException {

		final List<Field> fieldToUpdate = new ArrayList<>();
		final List<Tuple2<Field, List<DiffItem>>> fieldVariableToUpdate = new ArrayList<>();

		for (final Map.Entry<FieldDiffItemsKey, Field> entry : diffResult.getToUpdate().entrySet()) {

			final Map<Boolean, List<DiffItem>> diffPartition = // split the differences between the ones that are for the field and the ones that are for field variables
					entry.getKey().getDiffItems().stream().collect(Collectors.partitioningBy(diff -> diff.getVariable().startsWith("fieldVariable.")));
			final List<DiffItem> fieldVariableList = diffPartition.get(Boolean.TRUE);  // field variable diffs
			final List<DiffItem> fieldList         = diffPartition.get(Boolean.FALSE); // field diffs
			if (UtilMethods.isSet(fieldList)) {
				Logger.debug(this, "Updating the field : " + entry.getValue().variable() + " diff: "
						+ fieldList);
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

		// any diff on field variables, lets see what kind of diffs are.
		if (UtilMethods.isSet(fieldVariableToUpdate)) {
			handleUpdateFieldVariables(user, fieldVariableToUpdate);
		}
	}

	/**
	 * Handles the update of field variables for a given user and a list of field variable tuples.
	 *
	 * @param user                  The user object for which the field variables will be updated.
	 * @param fieldVariableToUpdate List of tuples containing the field and a list of diff items to
	 *                              update.
	 * @throws DotDataException     If there is an error accessing the data.
	 * @throws DotSecurityException If there is a security error.
	 */
	private void handleUpdateFieldVariables(
			final User user, final List<Tuple2<Field, List<DiffItem>>> fieldVariableToUpdate)
			throws DotDataException, DotSecurityException {

		for (final Tuple2<Field, List<DiffItem>> fieldVariableTuple : fieldVariableToUpdate) {
			handleUpdateFieldVariables(user, fieldVariableTuple);
		}
	}

	/**
	 * Handles the update of field variables for a user and field variable tuple.
	 *
	 * @param user               the user performing the update
	 * @param fieldVariableTuple the tuple containing the field and list of diff items
	 * @throws DotDataException     if there is an issue with data access
	 * @throws DotSecurityException if there is a security issue
	 */
	private void handleUpdateFieldVariables(
			final User user, final Tuple2<Field, List<DiffItem>> fieldVariableTuple)
			throws DotDataException, DotSecurityException {

		final Map<String, FieldVariable> fieldVariableMap =
				fieldVariableTuple._1().fieldVariablesMap();
		for (final DiffItem diffItem : fieldVariableTuple._2()) {

			final var detail = diffItem.getDetail();

			// normalizing the real varname
			final String fieldVariableVarName = StringUtils.replace(diffItem.getVariable(),
					"fieldVariable.", StringPool.BLANK);
			if ("delete".equals(detail) &&
					fieldVariableMap.containsKey(fieldVariableVarName)) {

				APILocator.getContentTypeFieldAPI()
						.delete(fieldVariableMap.get(fieldVariableVarName));
			}

			// if add or update, it is pretty much the same
			if ("add".equals(detail) || "update".equals(detail)) {

				if ("update".equals(detail) &&
						!fieldVariableMap.containsKey(fieldVariableVarName)) {
					// on update get the current field and gets the id
					continue;
				}

				APILocator.getContentTypeFieldAPI()
						.save(fieldVariableMap.get(fieldVariableVarName), user);
			}
		}
	}

	@DELETE
	@Path("/id/{idOrVar}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	@Operation(
			operationId = "deleteContentType",
			summary = "Deletes a content type",
			description = "Deletes the content type based on the provided ID or Velocity variable name.\n\n" +
					"Returns JSON string containing the identifier of the deleted content type.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Content type deleted successfully",
							content = @Content(mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"entity\": \"{\\\"deleted\\\":\\\"string\\\"}\",\n" +
															"  \"errors\": [],\n" +
															"  \"i18nMessagesMap\": {},\n" +
															"  \"messages\": [],\n" +
															"  \"pagination\": null,\n" +
															"  \"permissions\": []\n" +
															"}"
											)
									}
							)
					),
					@ApiResponse(responseCode = "403", description = "Forbidden"),
					@ApiResponse(responseCode = "404", description = "Content type not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response deleteType(@PathParam("idOrVar") @Parameter(
										required = true,
										description = "The ID or Velocity variable name of the content type to delete.",
										schema = @Schema(type = "string")
								) final String idOrVar,
							   @Context final HttpServletRequest req, @Context final HttpServletResponse res) throws JSONException {

		final InitDataObject initData = this.webResource.init(null, req, res, true, null);
		final User user = initData.getUser();

		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);

		try {
			ContentType type;
			try {
				type = contentTypeAPI.find(idOrVar);
			} catch (NotFoundInDbException nfdb) {
				return Response.status(404).build();
			}

			contentTypeAPI.delete(type);

			JSONObject joe = new JSONObject();
			joe.put("deleted", type.id());

			return Response.ok(new ResponseEntityView<>(joe.toString())).build();
		} catch (final DotSecurityException e) {
			throw new ForbiddenException(e);
		} catch (final Exception e) {
			Logger.error(this, String.format("Error deleting content type identified by (%s) ",idOrVar), e);
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Path("/id/{idOrVar}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	@Operation(
			operationId = "getContentTypeIdVar",
			summary = "Retrieves a single content type",
			description = "Returns one content type based on the provided ID or Velocity variable name.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Content type retrieved successfully",
							content = @Content(mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"entity\": [\n" +
															"    {\n" +
															"      \"baseType\": \"string\",\n" +
															"      \"clazz\": \"string\",\n" +
															"      \"defaultType\": true,\n" +
															"      \"description\": \"string\",\n" +
															"      \"fields\": [],\n" +
															"      \"fixed\": false,\n" +
															"      \"folder\": \"string\",\n" +
															"      \"folderPath\": \"string\",\n" +
															"      \"host\": \"string\",\n" +
															"      \"iDate\": 0,\n" +
															"      \"icon\": \"string\",\n" +
															"      \"id\": \"string\",\n" +
															"      \"layout\": [],\n" +
															"      \"metadata\": {},\n" +
															"      \"modDate\": 0,\n" +
															"      \"multilingualable\": true,\n" +
															"      \"name\": \"string\",\n" +
															"      \"siteName\": \"string\",\n" +
															"      \"sortOrder\": 0,\n" +
															"      \"system\": true,\n" +
															"      \"variable\": \"string\",\n" +
															"      \"systemActionMappings\": {},\n" +
															"      \"variable\": \"string\",\n" +
															"      \"versionable\": true,\n" +
															"      \"workflows\": []\n" +
															"    }\n" +
															"  ],\n" +
															"  \"errors\": [],\n" +
															"  \"i18nMessagesMap\": {},\n" +
															"  \"messages\": [],\n" +
															"  \"pagination\": {\n" +
															"    \"currentPage\": 0,\n" +
															"    \"perPage\": 0,\n" +
															"    \"totalEntries\": 0\n" +
															"  },\n" +
															"  \"permissions\": []\n" +
															"}\n"
											)
									}
							)
					),
					@ApiResponse(responseCode = "403", description = "Forbidden"),
					@ApiResponse(responseCode = "404", description = "Not Found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response getType(
			@PathParam("idOrVar") @Parameter(
					required = true,
					description = "The ID or Velocity variable name of the content type to retrieve.\n\n" +
									"Example: `htmlpageasset` (Default page content type)",
					schema = @Schema(type = "string")
			) final String idOrVar,
			@Context final HttpServletRequest req,
			@Context final HttpServletResponse res,
			@QueryParam("languageId") @Parameter(
					description = "The language ID for localization.",
					schema = @Schema(type = "integer")
			) final Long languageId,
			@QueryParam("live") @Parameter(
					description = "Determines whether live versions of language variables are used in the returned object.",
					schema = @Schema(type = "boolean")
			) final Boolean paramLive)
			throws DotDataException {

		final InitDataObject initData = this.webResource.init(null, req, res, false, null);
		final User user = initData.getUser();
		ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
		Response response = Response.status(404).build();
        final HttpSession session = req.getSession(false);
		try {

			Logger.debug(this, ()-> "Getting the Type: " + idOrVar);

			final ContentType type = tapi.find(idOrVar);
			if (null == type) {
				// Humoring sonarlint, this block should never be reached as the find method will
				// throw an exception if the type is not found.
				throw new NotFoundInDbException(
						String.format("Content Type with ID or var name '%s' was not found", idOrVar
						));
			}

			if (null != session) {
				session.setAttribute(SELECTED_STRUCTURE_KEY, type.inode());
			}

			final boolean live = paramLive == null ?
					(PageMode.get(Try.of(HttpServletRequestThreadLocal.INSTANCE::getRequest).getOrNull())).showLive
					: paramLive;

			final ContentTypeInternationalization contentTypeInternationalization = languageId != null ?
					new ContentTypeInternationalization(languageId, live, user) : null;
			final ImmutableMap<Object, Object> resultMap = ImmutableMap.builder()
					.putAll(contentTypeHelper.contentTypeToMap(type,
							contentTypeInternationalization, user))
					.put(MAP_KEY_WORKFLOWS, this.workflowHelper.findSchemesByContentType(
							type.id(), initData.getUser()))
					.put(MAP_KEY_SYSTEM_ACTION_MAPPINGS,
							this.workflowHelper.findSystemActionsByContentType(
									type, initData.getUser()).stream()
							.collect(Collectors.toMap(mapping -> mapping.getSystemAction(),
									mapping -> mapping))).build();

			response = ("true".equalsIgnoreCase(req.getParameter("include_permissions")))?
					Response.ok(new ResponseEntityView<>(resultMap, PermissionsUtil.getInstance().getPermissionsArray(type, initData.getUser()))).build():
					Response.ok(new ResponseEntityView<>(resultMap)).build();
		} catch (final DotSecurityException e) {
			throw new ForbiddenException(e);
		} catch (final NotFoundInDbException nfdb2) {
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
	@Operation(
			operationId = "postContentTypeFilter",
			summary = "Filters content types",
			description = "Returns the list of content type objects that match the specified filter, with optional pagination criteria.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Content types filtered successfully",
							content = @Content(mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"entity\": [\n" +
															"    {\n" +
															"      \"baseType\": \"string\",\n" +
															"      \"clazz\": \"string\",\n" +
															"      \"defaultType\": true,\n" +
															"      \"description\": \"string\",\n" +
															"      \"fixed\": false,\n" +
															"      \"folder\": \"string\",\n" +
															"      \"folderPath\": \"string\",\n" +
															"      \"host\": \"string\",\n" +
															"      \"iDate\": 0,\n" +
															"      \"icon\": \"string\",\n" +
															"      \"id\": \"string\",\n" +
															"      \"layout\": [],\n" +
															"      \"metadata\": {},\n" +
															"      \"modDate\": 0,\n" +
															"      \"multilingualable\": true,\n" +
															"      \"nEntries\": 0,\n" +
															"      \"name\": \"string\",\n" +
															"      \"siteName\": \"string\",\n" +
															"      \"sortOrder\": 0,\n" +
															"      \"system\": true,\n" +
															"      \"variable\": \"string\",\n" +
															"      \"versionable\": true,\n" +
															"      \"workflows\": []\n" +
															"    }\n" +
															"  ],\n" +
															"  \"errors\": [],\n" +
															"  \"i18nMessagesMap\": {},\n" +
															"  \"messages\": [],\n" +
															"  \"pagination\": {\n" +
															"    \"currentPage\": 0,\n" +
															"    \"perPage\": 0,\n" +
															"    \"totalEntries\": 0\n" +
															"  },\n" +
															"  \"permissions\": []\n" +
															"}\n"
											)
									}
							)
					),
					@ApiResponse(responseCode = "400", description = "Bad Request"),
					@ApiResponse(responseCode = "403", description = "Forbidden"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public final Response filteredContentTypes(@Context final HttpServletRequest req,
											   @Context final HttpServletResponse res,
											   @RequestBody(
													   description = "Requires POST body consisting of a JSON object with the following properties:\n\n" +
															   "| Property |  Type  | Description |\n" +
															   "|----------|--------|-------------|\n" +
															   "| `filter`   | JSON Object | Contains two properties: <table><tr><td>`query`</td><td>A simple query returning " +
															   								"full or partial matches.</td></tr><tr><td>`types`</td><td>A comma-separated list " +
															   								"of specific content type variables.</td></tr></table> |\n" +
															   "| `page` | Integer | Which page of results to show. Defaults to `1`. |\n" +
															   "| `perPage`   | Integer | Number of results to display per page. Defaults to `10`. |\n" +
															   "| `orderBy`   | String | Sorting parameter: `name` (default), `velocity_var_name`, `mod_date`, or `sort_order`. |\n" +
															   "| `direction`   | String | `ASC` (default) or `DESC` for ascending or descending. |",
													   required = true,
													   content = @Content(
															   schema = @Schema(implementation = FilteredContentTypesForm.class),
															   examples = {
																	   @ExampleObject(
																			   value = "{\n" +
																					   "  \"filter\": {\n" +
																					   "    \"query\": \"\",\n" +
																					   "    \"types\": \"Blog,Activity\"\n" +
																					   "  },\n" +
																					   "  \"page\": 1,\n" +
																					   "  \"perPage\": 10,\n" +
																					   "  \"orderBy\": \"name\",\n" +
																					   "  \"direction\": \"ASC\"\n" +
																					   "}"
																	   )
															   }
													   )
											   ) final FilteredContentTypesForm form) {
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
	@Operation(
			operationId = "getContentTypeBaseTypes",
			summary = "Retrieves base content types",
			description = "Returns a list of base content types.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Base content types retrieved successfully",
							content = @Content(mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"entity\": [\n" +
															"    {\n" +
															"      \"label\": \"string\",\n" +
															"      \"name\": \"string\",\n" +
															"      \"types\": null\n" +
															"    }\n" +
															"  ],\n" +
															"  \"errors\": [],\n" +
															"  \"i18nMessagesMap\": {},\n" +
															"  \"messages\": [],\n" +
															"  \"pagination\": null,\n" +
															"  \"permissions\": []\n" +
															"}"
											)
									}
							)
					),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public final Response getRecentBaseTypes(@Context final HttpServletRequest request) {
		Response response;
		try {
			final List<BaseContentTypesView> types = contentTypeHelper.getTypes(request);
			response = Response.ok(new ResponseEntityView<>(types)).build();
		} catch (Exception e) { // this is an unknown error, so we report as a 500.

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	} // getTypes.

	/**
	 * Returns a list of {@link ContentType} objects based on the filtering criteria. This is how
	 * you can call this endpoint:
	 * <pre>{@code
	 * GET http://localhost:8080/api/v1/contenttype?sites=48190c8c-42c4-46af-8d1a-0cd5db894797,SYSTEM_HOST,&per_page=40&&orderby=variabledirection=DESC
	 * }</pre>
	 * <p>If you want results composed of 10 items per page and you want the third page, and you
	 * don't have the Site's Identifier, you can call this URL:</p>
	 * <pre>{@code
	 * GET http://localhost:8080/api/v1/contenttype?sites=demo.dotcms.com&page=3&per_page=10
	 * }</pre>
	 *
	 * @param httpRequest  The current instance of the {@link HttpServletRequest}.
	 * @param httpResponse The current instance of the {@link HttpServletResponse}.
	 * @param filter       Filtering parameter used to pass down the Content Types name, Velocity
	 *                     Variable Name, or Inode. You can pass down part of the characters.
	 * @param page         The selected results page, for pagination purposes.
	 * @param perPage      The number of results to return per page, for pagination purposes.
	 * @param orderByParam The column name that will be used to sort the paginated results. For
	 *                     reference, please check
	 *                     {@link com.dotmarketing.common.util.SQLUtil#ORDERBY_WHITELIST}.
	 * @param direction    The direction of the sorting. It can be either "ASC" or "DESC".
	 * @param type         The Velocity variable name of the Content Type  to retrieve.
	 * @param siteId       The identifier of the Site where the requested Content Types live.
	 * @param sites        A comma-separated list of Site identifiers or Site Keys where the
	 *                     requested Content Types live.
	 *
	 * @return A JSON response with the paginated list of Content Types.
	 *
	 * @throws DotDataException An error occurred when retrieving information from the database.
	 */
	@GET
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	@Operation(
			operationId = "getContentType",
			summary = "Retrieves a list of content types",
			description = "Returns a list of content type objects based on the filtering criteria.",
			tags = {"Content Type"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Content types retrieved successfully",
							content = @Content(
									mediaType = "application/json",
									examples = {
											@ExampleObject(
													value = "{\n" +
															"  \"entity\": [\n" +
															"    {\n" +
															"      \"baseType\": \"string\",\n" +
															"      \"clazz\": \"string\",\n" +
															"      \"defaultType\": true,\n" +
															"      \"description\": \"string\",\n" +
															"      \"fixed\": true,\n" +
															"      \"folder\": \"string\",\n" +
															"      \"folderPath\": \"string\",\n" +
															"      \"host\": \"string\",\n" +
															"      \"iDate\": 0,\n" +
															"      \"icon\": \"string\",\n" +
															"      \"id\": \"string\",\n" +
															"      \"layout\": [],\n" +
															"      \"metadata\": {},\n" +
															"      \"modDate\": 0,\n" +
															"      \"multilingualable\": true,\n" +
															"      \"nEntries\": 0,\n" +
															"      \"name\": \"string\",\n" +
															"      \"siteName\": \"string\",\n" +
															"      \"sortOrder\": 0,\n" +
															"      \"system\": true,\n" +
															"      \"variable\": \"string\",\n" +
															"      \"versionable\": true,\n" +
															"      \"workflows\": []\n" +
															"    }\n" +
															"  ],\n" +
															"  \"errors\": [],\n" +
															"  \"i18nMessagesMap\": {},\n" +
															"  \"messages\": [],\n" +
															"  \"pagination\": {\n" +
															"    \"currentPage\": 0,\n" +
															"    \"perPage\": 0,\n" +
															"    \"totalEntries\": 0\n" +
															"  },\n" +
															"  \"permissions\": []\n" +
															"}\n"
											)
									}
							)
					),
					@ApiResponse(responseCode = "403", description = "Forbidden"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public final Response getContentTypes(@Context final HttpServletRequest httpRequest,
										  @Context final HttpServletResponse httpResponse,
										  @QueryParam(PaginationUtil.FILTER) @Parameter(schema = @Schema(type = "string"),
												  description = "String to filter/search for specific content types; leave blank to return all."
										  ) final String filter,
										  @QueryParam(PaginationUtil.PAGE) @Parameter(schema = @Schema(type = "integer"),
												  description = "Page number in response pagination.\n\nDefault: `1`"
										  ) final int page,
										  @QueryParam(PaginationUtil.PER_PAGE) @Parameter(schema = @Schema(type = "integer"),
												  description = "Number of results per page for pagination.\n\nDefault: `10`"
										  ) final int perPage,
										  @DefaultValue("upper(name)") @QueryParam(PaginationUtil.ORDER_BY) @Parameter(
												  schema = @Schema(type = "string"),
												  description = "Column(s) to sort the results. Multiple columns can be " +
														  "combined in a comma-separated list. Column names can also be set " +
														  "within a SQL string function, such as `upper()`.\n\n" +
														  "Some possible values:\n\n" +
														  "`name`, `velocity_var_name`, `mod_date`, `sort_order`\n\n" +
														  "`description`, `structuretype`, `category`, `inode`"
										  ) String orderByParam,
										  @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) @Parameter(
												  schema = @Schema(
														  type = "string",
														  allowableValues = {"ASC", "DESC"},
														  defaultValue = "ASC",
														  required = true
												  ),
												  description = "Sort direction: choose between ascending or descending."
										  ) String direction,
										  @QueryParam("type") @Parameter(
												  schema = @Schema(
														  type = "string",
														  allowableValues = {
																  "ANY", "CONTENT", "WIDGET",
																  "FORM", "FILEASSET", "HTMLPAGE", "PERSONA",
																  "VANITY_URL", "KEY_VALUE", "DOTASSET"
														  }
												  ),
												  description = "Variable name of [base content type](https://www.dotcms.com/docs/latest/base-content-types)."
										  ) String type,
										  @QueryParam(ContentTypesPaginator.HOST_PARAMETER_ID) @Parameter(schema = @Schema(type = "string"),
												  description = "Filter by site identifier."
										  ) final String siteId,
										  @QueryParam(ContentTypesPaginator.SITES_PARAMETER_NAME) @Parameter(schema = @Schema(type = "string"),
												  description = "Multi-site filter: Takes comma-separated list of site identifiers or keys."
										  ) final String sites) throws DotDataException {

		final User user = new WebResource.InitBuilder(this.webResource)
				.requestAndResponse(httpRequest, httpResponse)
				.rejectWhenNoUser(true)
				.init().getUser();
		final String orderBy = this.getOrderByRealName(orderByParam);
		try {
			final Map<String, Object> extraParams = new HashMap<>();
			if (null != type) {
				extraParams.put(ContentTypesPaginator.TYPE_PARAMETER_NAME, type);
			}
			if (null != siteId) {
				extraParams.put(ContentTypesPaginator.HOST_PARAMETER_ID,siteId);
			}
			if (UtilMethods.isSet(sites)) {
				extraParams.put(ContentTypesPaginator.SITES_PARAMETER_NAME,
						Arrays.asList(sites.split(COMMA)));
			}
			final PaginationUtil paginationUtil = new PaginationUtil(new ContentTypesPaginator(APILocator.getContentTypeAPI(user)));
			return paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy,
					OrderDirection.valueOf(direction), extraParams);
		} catch (final IllegalArgumentException e) {
			throw new DotDataException(String.format("An error occurred when listing Content Types: " +
					"%s", ExceptionUtil.getErrorMessage(e)));
		} catch (final Exception e) {
			if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
				throw new ForbiddenException(e);
			}
			Logger.error(this, String.format("An error occurred when listing Content Types: " +
					"%s", ExceptionUtil.getErrorMessage(e)), e);
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
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
	@SuppressWarnings("unchecked")
	private <T> T getFilterValue(final FilteredContentTypesForm form, final String param, T defaultValue) {
		if (null == form || null == form.getFilter() || form.getFilter().isEmpty()) {
			return defaultValue;
		}
		return UtilMethods.isSet(form.getFilter().get(param)) ? (T) form.getFilter().get(param) : defaultValue;
	}

}
