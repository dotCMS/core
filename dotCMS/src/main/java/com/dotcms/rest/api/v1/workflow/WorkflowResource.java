package com.dotcms.rest.api.v1.workflow;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.*;
import com.dotcms.rest.annotation.IncludePermissions;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.authentication.RequestUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.variant.VariantAPI;
import com.dotcms.workflow.form.BulkActionForm;
import com.dotcms.workflow.form.FireActionByNameForm;
import com.dotcms.workflow.form.FireActionForm;
import com.dotcms.workflow.form.FireBulkActionsForm;
import com.dotcms.workflow.form.FireMultipleActionForm;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowActionStepBean;
import com.dotcms.workflow.form.WorkflowActionStepForm;
import com.dotcms.workflow.form.WorkflowActionletActionBean;
import com.dotcms.workflow.form.WorkflowActionletActionForm;
import com.dotcms.workflow.form.WorkflowCopyForm;
import com.dotcms.workflow.form.WorkflowReorderBean;
import com.dotcms.workflow.form.WorkflowReorderWorkflowActionStepForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowSchemeImportObjectForm;
import com.dotcms.workflow.form.WorkflowSchemesForm;
import com.dotcms.workflow.form.WorkflowStepAddForm;
import com.dotcms.workflow.form.WorkflowStepUpdateForm;
import com.dotcms.workflow.form.WorkflowSystemActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.model.WorkflowTimelineItem;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.HttpHeaders;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.time.StopWatch;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.dotcms.rest.ResponseEntityView.OK;
import static com.dotcms.util.DotLambdas.not;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.FAIL_ACTION_CALLBACK;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SUCCESS_ACTION_CALLBACK;

/**
 * Encapsulates all the interaction with dotCMS Workflows. This REST Endpoint allows you to execute
 * operations such as:
 * <ul>
 *     <li>Create schemes, action into schemes, steps into schemes.</li>
 *     <li>Associated actions to steps.</li>
 *     <li>Get schemes, steps, actions and relation information.</li>
 *     <li>Get available actions for a content.</li>
 *     <li>Fire an action for a single content or bulk (action for a several contents).</li>
 * </ul>
 * <p>You can find more information in the
 * {@code dotcms-postman/src/main/resources/postman/documentation/Workflow_Resource_Tests.json} file. It's a complete
 * collection of examples on how to interact with this Resource.</p>
 *
 * @author jsanca
 * @since Dec 6th, 2017
 */
@Path("/v1/workflow")
@Tag(name = "Workflow",
        description = "Endpoints that perform operations related to workflows.",
        externalDocs = @ExternalDocumentation(description = "Additional Workflow API information",
                url = "https://www.dotcms.com/docs/latest/workflow-rest-api")
)
@ApiResponses(
        value = { // error codes only!
                @ApiResponse(responseCode = "401", description = "Invalid User"), // not logged in
                @ApiResponse(responseCode = "403", description = "Forbidden"), // no permission
                // @ApiResponse(responseCode = "405", description = "Method Not Allowed"), // wrong verb; unlikely a user will have to explicitly handle this
                @ApiResponse(responseCode = "406", description = "Not Acceptable"), // accept header mismatch
                @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
)
public class WorkflowResource {

    public  final static String VERSION       = "1.0";
    private static final String LISTING       = "listing";
    private static final String EDITING       = "editing";
    private static final String ASSIGN        = "assign";
    private static final String COMMENTS      = "comments";
    private static final String PUBLISH_DATE  = "publishDate";
    private static final String PUBLISH_TIME  = "publishTime";
    private static final String EXPIRE_DATE   = "expireDate";
    private static final String EXPIRE_TIME   = "expireTime";
    private static final String NEVER_EXPIRE  = "neverExpire";
    private static final String BINARY_FIELDS = "binaryFields";
    private static final String PREFIX_BINARY = "binary";
    private static final String ACTION_NAME   = "actionName";
    private static final String CONTENTLET    = "contentlet";
    private static final int CONTENTLETS_LIMIT = 100000;
    private static final String WORKFLOW_SUBMITTER = "workflow_submitter";
    public static final String INCLUDE_SEPARATOR = "includeSeparator";


    private final WorkflowHelper   workflowHelper;
    private final ContentHelper    contentHelper;
    private final WebResource      webResource;
    private final WorkflowAPI      workflowAPI;
    private final ResponseUtil     responseUtil;
    private final ContentletAPI    contentletAPI;
    private final PermissionAPI    permissionAPI;
    private final WorkflowImportExportUtil workflowImportExportUtil;
    private final MultiPartUtils   multiPartUtils;
    private final SystemActionApiFireCommandFactory systemActionApiFireCommandProvider;
    private final Set<String>   validRenderModeSet = ImmutableSet.of(LISTING, EDITING);


    /**
     * Default constructor.
     */
    @SuppressWarnings("unused")
    public WorkflowResource() {
        this(WorkflowHelper.getInstance(),
                ContentHelper.getInstance(),
                APILocator.getWorkflowAPI(),
                APILocator.getContentletAPI(),
                ResponseUtil.INSTANCE,
                APILocator.getPermissionAPI(),
                WorkflowImportExportUtil.getInstance(),
                new MultiPartUtils(),
                new WebResource(),
                SystemActionApiFireCommandFactory.getInstance());
    }

    @VisibleForTesting
    WorkflowResource(final WorkflowHelper workflowHelper,
                     final ContentHelper    contentHelper,
                     final WorkflowAPI      workflowAPI,
                     final ContentletAPI    contentletAPI,
                     final ResponseUtil     responseUtil,
                     final PermissionAPI    permissionAPI,
                     final WorkflowImportExportUtil workflowImportExportUtil,
                     final MultiPartUtils   multiPartUtils,
                     final WebResource webResource,
                     final SystemActionApiFireCommandFactory systemActionApiFireCommandProvider) {

        this.workflowHelper           = workflowHelper;
        this.contentHelper            = contentHelper;
        this.webResource              = webResource;
        this.responseUtil             = responseUtil;
        this.workflowAPI              = workflowAPI;
        this.permissionAPI            = permissionAPI;
        this.contentletAPI            = contentletAPI;
        this.multiPartUtils           = multiPartUtils;
        this.workflowImportExportUtil = workflowImportExportUtil;
        this.systemActionApiFireCommandProvider =
                systemActionApiFireCommandProvider;

    }

    /**
     * Returns all Workflow schemes, optionally associated with a content type. 401 if the user lacks permission.
     * @param request  HttpServletRequest
     * @param contentTypeId String content type id to get the schemes associated to it; if this is null, return
     *                      all schemes.
     * @param showArchived Boolean determines whether to include archived Workflow schemes. (Default: true)
     * @return Response
     */
    @GET
    @Path("/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowSchemes", summary = "Find workflow schemes",
            description = "Returns workflow schemes. Can be filtered by content type and/or live status " +
                          "through optional query parameters.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Scheme(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowSchemesView.class)
                            )
                    ),
                   @ApiResponse(responseCode = "404", description = "Workflow scheme not found")
            }
    )
    public final Response findSchemes(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @QueryParam("contentTypeId") @Parameter(
                                              description = "Optional filter parameter that takes a content type identifier and returns " +
                                                            "all [workflow schemes](https://www.dotcms.com/docs/latest/managing-workflows#Schemes) " +
                                                            "associated with that type.\n\n" +
                                                            "Leave blank to return all workflow schemes.\n\n" +
                                                            "Example value: `c541abb1-69b3-4bc5-8430-5e09e5239cc8` " +
                                                            "(Default page content type)",
                                              schema = @Schema(type = "string")
                                      ) final String contentTypeId,
                                      @DefaultValue("true") @QueryParam("showArchived") @Parameter(
                                              description = "If `true`, includes archived schemes in response.",
                                              schema = @Schema(type = "boolean", defaultValue = "true")
                                      ) final boolean showArchived) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId);
            final List<WorkflowScheme> schemes = (null != contentTypeId) ?
                    ((showArchived)?
                            this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser()):
                            this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser())
                                    .stream().filter(not(WorkflowScheme::isArchived)).collect(CollectionsUtils.toImmutableList())):
                    this.workflowHelper.findSchemes(showArchived);

            return Response.ok(new ResponseEntityView<>(schemes)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findSchemes exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSchemes.

    /**
     * Returns all actionlets classes
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/actionlets")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowActionlets", summary = "Find all workflow actionlets",
            description = "Returns a list of all workflow actionlets — a.k.a. [workflow sub-actions]" +
                            "(https://www.dotcms.com/docs/latest/workflow-sub-actions). " +
                          "The returned list is complete and does not use pagination.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow actionlets returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionletsView.class)
                            )
                    )
            }
    )
    public final Response findActionlets(@Context final HttpServletRequest request) {

        this.webResource.init
                (null, true, request, true, null);
        try {

            Logger.debug(this,
                    ()->"Getting the workflow actionlets");

            final List<WorkFlowActionlet> actionlets = this.workflowAPI.findActionlets();
            return Response.ok(new ResponseEntityView<>(actionlets)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findActionlets exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionlets.

    /**
     * Returns all actionlets associated to a workflow action. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String action id to get the actionlet associated to it, is this is null return
     *                      all the WorkflowActionClass
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}/actionlets")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowActionletsByActionId", summary = "Find workflow actionlets by workflow action",
            description = "Returns a list of the workflow actionlets — a.k.a. [workflow sub-actions](https://www.dotcms." +
                            "com/docs/latest/workflow-sub-actions) — associated with a specified workflow action.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow actionlets returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionClassesView.class),
                                    examples = @ExampleObject(
                                            value = "{\n" +
                                                    "  \"entity\": [\n" +
                                                    "    {\n" +
                                                    "      \"actionId\": \"string\",\n" +
                                                    "      \"actionlet\": {\n" +
                                                    "        \"actionClass\": \"string\",\n" +
                                                    "        \"howTo\": \"string\",\n" +
                                                    "        \"localizedHowto\": \"string\",\n" +
                                                    "        \"localizedName\": \"string\",\n" +
                                                    "        \"name\": \"string\",\n" +
                                                    "        \"nextStep\": null,\n" +
                                                    "        \"parameters\": [\n" +
                                                    "          {\n" +
                                                    "            \"displayName\": \"string\",\n" +
                                                    "            \"key\": \"string\",\n" +
                                                    "            \"defaultValue\": \"string\",\n" +
                                                    "            \"required\": true\n" +
                                                    "          }\n" +
                                                    "        ]\n" +
                                                    "      },\n" +
                                                    "      \"clazz\": \"string\",\n" +
                                                    "      \"id\": \"string\",\n" +
                                                    "      \"name\": \"string\",\n" +
                                                    "      \"order\": 0\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"errors\": [],\n" +
                                                    "  \"i18nMessagesMap\": {},\n" +
                                                    "  \"messages\": [],\n" +
                                                    "  \"pagination\": null,\n" +
                                                    "  \"permissions\": []\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response findActionletsByAction(@Context final HttpServletRequest request,
                                                 @PathParam("actionId") @Parameter(
                                                         required = true,
                                                         description = "Identifier of workflow action to examine for actionlets.\n\n" +
                                                                       "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                                                                       "(Default system workflow \"Publish\" action)",
                                                         schema = @Schema(type = "string")
                                                 ) final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {

            Logger.debug(this,
                    ()->"Getting the workflow actionlets for the action: " + actionId);

            final WorkflowAction action = this.workflowAPI.findAction(actionId, initDataObject.getUser());
            DotPreconditions.notNull(action, ()->"Action: " + actionId + ", does not exists", NotFoundInDbException.class);

            final List<WorkflowActionClass> actionClasses = this.workflowAPI.findActionClasses(action);
            return Response.ok(new ResponseEntityView<>(actionClasses)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findActionletsByAction exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionletsByAction.

    /**
     * Returns all schemes for the content type and include schemes non-archive . 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/schemes/schemescontenttypes/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowSchemesByContentTypeId", summary = "Find workflow schemes by content type id",
            description = "Fetches [workflow schemes](https://www.dotcms.com/docs/latest/managing-workflows#Schemes) " +
                    " associated with a content type by its identifier. Returns an entity containing two properties:\n\n" +
                          "| Property | Description |\n" +
                          "|----------|-------------|\n" +
                          "| `contentTypeSchemes` | A list of schemes associated with the specified content type. |\n" +
                          "| `schemes` | A list of non-archived schemes, irrespective of relation to the content type. |",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Scheme(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SchemesAndSchemesContentTypeView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Content type ID not found")
            }
    )
    public final Response findAllSchemesAndSchemesByContentType(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("contentTypeId") @Parameter(
                    required = true,
                    description = "Identifier of content type to examine for workflow schemes.\n\n" +
                                  "Example value: `c541abb1-69b3-4bc5-8430-5e09e5239cc8` (Default page content type)",
                    schema = @Schema(type = "string")
            ) final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId
                            + " and including All Schemes");
            final List<WorkflowScheme> schemes = this.workflowHelper.findSchemes();
            final List<WorkflowScheme> contentTypeSchemes = this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser());

            return Response.ok(new ResponseEntityView(
                    new SchemesAndSchemesContentTypeView(schemes, contentTypeSchemes)))
                    .build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findAllSchemesAndSchemesByContentType exception message: " + e
                            .getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);

        }
    } // findAllSchemesAndSchemesByContentType.

    /**
     * Return Steps associated to the scheme, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/steps")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowStepsBySchemeId", summary = "Find steps by workflow scheme ID",
            description = "Returns a list of [steps](https://www.dotcms.com/docs/latest/managing-workflows#Steps) " +
                    "associated with a [workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Scheme(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowStepsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow scheme not found")
            }
    )
    public final Response findStepsByScheme(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @PathParam("schemeId") @Parameter(
                                                    required = true,
                                                    description = "Identifier of workflow scheme.\n\n" +
                                                                  "Example value: `d61a59e1-a49c-46f2-a929-db2b4bfa88b2` " +
                                                                  "(Default system workflow)",
                                                    schema = @Schema(type = "string")
                                            ) final String schemeId) {

        this.webResource.init
                (null, request, response, true, null);

        try {
            Logger.debug(this, "Getting the workflow steps for the scheme: " + schemeId);
            final List<WorkflowStep> steps = this.workflowHelper.findSteps(schemeId);
            return Response.ok(new ResponseEntityView<>(steps)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findStepsByScheme exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);

        }
    } // findSteps.

    /**
     * Finds the Workflow Actions that are available for a specific Contentlet Inode. Here's an
     * example of how you can use this method:
     * <pre>
     *     GET http://localhost:8080/api/v1/workflow/contentlet/{CONTENTLET-INODE}/actions?renderMode={editing|listing}
     * </pre>
     *
     * @param request    The current instance of the {@link HttpServletRequest}.
     * @param inode      The Inode of the Contentlet.
     * @param renderMode This is a case-insensitive optional parameter. By default, this method will
     *                   run EDITING rendering mode. The available modes are specified via the
     *                   {{@link #validRenderModeSet}} variable.
     *
     * @return Response A {@link Response} object that contains the available actions for the
     * specified Contentlet.
     */
    @GET
    @Path("/contentlet/{inode}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowActionsByContentletInode", summary = "Finds workflow actions by content inode",
            description = "Returns a list of [workflow actions](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "associated with a [contentlet](https://www.dotcms.com/docs/latest/content#Contentlets) specified by inode.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Scheme(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Contentlet not found")
            }
    )
    public final Response findAvailableActions(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               @PathParam("inode") @Parameter(
                                                       required = true,
                                                       description = "Inode of contentlet to examine for workflow actions.\n\n",
                                                       schema = @Schema(type = "string")
                                               ) final String inode,
                                               @QueryParam("renderMode") @Parameter(
                                                       description = "*Optional.* Case-insensitive parameter indicating " +
                                                                     "how results are to be displayed.\n\n" +
                                                                     "In listing mode, all associated actions are returned; " +
                                                                     "in editing mode (the default), it returns only the actions " +
                                                                     "accessible to the contentlet's current workflow step.",
                                                       schema = @Schema(
                                                               type = "string",
                                                               allowableValues = {"EDITING", "LISTING"},
                                                               defaultValue = "EDITING"
                                                       )
                                               ) final String renderMode) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, ()->"Getting the available actions for the contentlet inode: " + inode);

            this.workflowHelper.checkRenderMode (renderMode, initDataObject.getUser(), this.validRenderModeSet);

            final WorkflowAPI.RenderMode mode = LISTING.equalsIgnoreCase(renderMode)?
                    WorkflowAPI.RenderMode.LISTING: WorkflowAPI.RenderMode.EDITING;
            final boolean includeSeparator = ConversionUtils.toBoolean(request.getParameter(INCLUDE_SEPARATOR), false);
            final List<WorkflowAction> actions = includeSeparator? this.workflowHelper.findAvailableActions(inode, initDataObject.getUser(), mode):
                    this.workflowHelper.findAvailableActionsSkippingSeparators(inode, initDataObject.getUser(), mode);
            return Response.ok(new ResponseEntityView<>(actions.stream()
                    .map(this::toWorkflowActionView).collect(Collectors.toList()))).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), String.format("An error occurred when finding available" +
                            " Workflow Actions for Contentlet Inode '%s' in mode '%s': %s", inode, renderMode,
                    ExceptionUtil.getErrorMessage(e)), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findAvailableActions.

    private WorkflowActionView toWorkflowActionView(final WorkflowAction workflowAction) {

        return convertToWorkflowActionView(workflowAction);
    }

    /**
     * Takes the information from a Workflow Action and transforms it into a View object that can
     * display it in JSON notation appropriately. Keep in mind that any new property you add to the
     * Workflow Action class will need to be added here as well.
     *
     * @param workflowAction The {@link WorkflowAction} that will be transformed.
     *
     * @return The {@link WorkflowActionView} that contains the information from the Workflow
     * Action.
     */
    public static WorkflowActionView convertToWorkflowActionView(final WorkflowAction workflowAction) {
        final WorkflowActionView workflowActionView = new WorkflowActionView();
        workflowActionView.setId(workflowAction.getId());
        workflowActionView.setName(workflowAction.getName());
        workflowActionView.setStepId(workflowAction.getSchemeId());
        workflowActionView.setSchemeId(workflowAction.getSchemeId());
        workflowActionView.setCondition(workflowAction.getCondition());
        workflowActionView.setNextStep(workflowAction.getNextStep());
        workflowActionView.setNextAssign(workflowAction.getNextAssign());
        workflowActionView.setIcon(workflowAction.getIcon());
        workflowActionView.setRoleHierarchyForAssign(workflowAction.isRoleHierarchyForAssign());
        workflowActionView.setRequiresCheckout(workflowAction.isRoleHierarchyForAssign());
        workflowActionView.setAssignable(workflowAction.isAssignable());
        workflowActionView.setCommentable(workflowAction.isCommentable());
        workflowActionView.setOrder(workflowAction.getOrder());
        workflowActionView.setSaveActionlet(workflowAction.hasSaveActionlet());
        workflowActionView.setPublishActionlet(workflowAction.hasPublishActionlet());
        workflowActionView.setUnpublishActionlet(workflowAction.hasUnpublishActionlet());
        workflowActionView.setArchiveActionlet(workflowAction.hasArchiveActionlet());
        workflowActionView.setPushPublishActionlet(workflowAction.hasPushPublishActionlet());
        workflowActionView.setUnarchiveActionlet(workflowAction.hasUnarchiveActionlet());
        workflowActionView.setDeleteActionlet(workflowAction.hasDeleteActionlet());
        workflowActionView.setDestroyActionlet(workflowAction.hasDestroyActionlet());
        workflowActionView.setShowOn(workflowAction.getShowOn());
        workflowActionView.setActionInputs(createActionInputViews(workflowAction));
        workflowActionView.setMetadata(workflowAction.getMetadata());
        workflowActionView.setCommentActionlet(workflowAction.hasCommentActionlet());
        workflowActionView.setResetable(workflowAction.hasResetActionlet());
        workflowActionView.setMoveActionletHashPath(workflowAction.hasMoveActionletHasPathActionlet());
        workflowActionView.setMoveActionlet(workflowAction.hasMoveActionletActionlet());

        return workflowActionView;
    }

    private static List<ActionInputView> createActionInputViews (final WorkflowAction workflowAction) {

        final List<ActionInputView> actionInputViews = new ArrayList<>();

        if (workflowAction.isAssignable()) {

            actionInputViews.add(new ActionInputView("assignable", Collections.emptyMap()));
        }

        if (workflowAction.isCommentable()) {

            actionInputViews.add(new ActionInputView("commentable", Collections.emptyMap()));
        }

        if (workflowAction.hasPushPublishActionlet()) {

            actionInputViews.add(new ActionInputView("pushPublish", Collections.emptyMap()));
        }

        // Has a move actionlet but the path is empty
        if (workflowAction.hasMoveActionletActionlet() && !workflowAction.hasMoveActionletHasPathActionlet()) {

            actionInputViews.add(new ActionInputView("moveable", Collections.emptyMap()));
        }

        return actionInputViews;
    }

    /**
     * Get the bulk actions based on the {@link BulkActionForm}
     * @param request HttpServletRequest
     * @param bulkActionForm String
     * @return Response
     */
    @POST
    @Path("/contentlet/actions/bulk")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postBulkActions", summary = "Finds available bulk workflow actions for content",
            description = "Returns a list of bulk actions available for " +
                    "[contentlets](https://www.dotcms.com/docs/latest/content#Contentlets) either by identifiers " +
                          "or by query, as specified in the body.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Zero or more bulk actions returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBulkActionView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response getBulkActions(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         @RequestBody(
                                                 description = "Body consists of a JSON object with either of the following properties:\n\n" +
                                                               "| Property | Type | Description |\n" +
                                                               "|-|-|-|\n" +
                                                               "| `contentletIds` | List of Strings | A list of individual contentlet identifiers. |\n" +
                                                               "| `query` | String | [Lucene query](https://www.dotcms.com/docs/latest/content-search-syntax#Lucene); " +
                                                                                    "uses all matching contentlets. |\n\n" +
                                                               "If both properties are present, the operation will use the list of identifiers and disregard " +
                                                               "the query.",
                                                 required = true,
                                                 content = @Content(
                                                         schema = @Schema(implementation = BulkActionForm.class),
                                                         examples = {
                                                                 @ExampleObject(
                                                                         value = "{\n" +
                                                                                 "  \"contentletIds\": [\n" +
                                                                                 "    \"651a4dc8-2124-45d8-8bd2-d8e68ad358a8\",\n" +
                                                                                 "    \"f8d60f79-e006-42e0-894f-5d3488b796f6\"\n" +
                                                                                 "  ],\n" +
                                                                                 "  \"query\": \"+contentType:*\"\n" +
                                                                                 "}"
                                                                 )
                                                        }
                                                 )
                                         ) final BulkActionForm bulkActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            DotPreconditions.notNull(bulkActionForm,"Expected Request body was empty.");
            Logger.debug(this, ()-> "Getting the bulk actions for the contentlets inodes: " + bulkActionForm);
            return Response.ok(new ResponseEntityView
                    (this.workflowHelper.findBulkActions(initDataObject.getUser(), bulkActionForm)))
                    .build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on getBulkActions, bulkActionForm: " + bulkActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // getBulkActions.

    @PUT
    @Path("/contentlet/actions/bulk/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putBulkActionsFire", summary = "Perform workflow actions on bulk content",
            description = "This operation allows you to specify a multiple content items (either by query or a list of " +
                          "identifiers), a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                          "to perform on them, and additional parameters as needed by the selected action.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBulkActionsResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final void fireBulkActions(@Context final HttpServletRequest request,
                                      @Suspended final AsyncResponse asyncResponse,
                                      @RequestBody(
                                              description = "Body consists of a JSON object with the following possible properties:\n\n" +
                                                      "| Property | Type | Description |\n" +
                                                      "|-|-|-|\n" +
                                                      "| `contentletIds` | List of Strings | A list of individual contentlet identifiers. |\n" +
                                                      "| `query` | String | [Lucene query](https://www.dotcms.com/docs/latest/content-search-syntax#Lucene); " +
                                                                                        "uses all matching contentlets. |\n" +
                                                      "| `workflowActionId` | String | The identifier of the workflow action to be performed on the " +
                                                                                        "selected content. |\n" +
                                                      "| `additionalParams` | Object | Further parameters and properties are conveyed here, depending " +
                                                                                        "on the particulars of the selected action.<br><br>For a " +
                                                                                        "complete list of possible parameters, refer to the various " +
                                                                                        "keys listed in `GET /workflow/actionlets`. |\n\n" +
                                                      "If both `contentletIds` and `query` properties are present, the operation will use the query and " +
                                                      "disregard the identifier list.",
                                              required = true,
                                              content = @Content(schema = @Schema(implementation = FireBulkActionsForm.class))
                                      ) final FireBulkActionsForm fireBulkActionsForm) {

        final InitDataObject initDataObject = this.webResource.init(null, request, new EmptyHttpResponse(), true, null);
        Logger.debug(this, ()-> "Fire bulk actions: " + fireBulkActionsForm);
        try {
            // check the form
            DotPreconditions.notNull(fireBulkActionsForm,"Expected Request body was empty.");
            ResponseUtil.handleAsyncResponse(() -> {
                try {
                    final BulkActionsResultView view = workflowHelper
                            .fireBulkActions(fireBulkActionsForm, initDataObject.getUser());
                    return Response.ok( new ResponseEntityView(view)).build();
                } catch (Exception e) {
                    asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
                }
                return null;
            }, asyncResponse);

        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to fire bulk actions by : " +fireBulkActionsForm + ", exception message: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    }

    @POST
    @Path("/contentlet/actions/_bulkfire")
    @JSONP
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postBulkActionsFire", summary = "Perform workflow actions on bulk content",
            description = "This operation allows you to specify a multiple content items (either by query or a list of " +
                    "identifiers), a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "to perform on them, and additional parameters as needed by the selected action.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EventOutput.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public EventOutput fireBulkActions(@Context final HttpServletRequest request,
                                       @RequestBody(
                                               description = "Body consists of a JSON object with the following possible properties:\n\n" +
                                                       "| Property | Type | Description |\n" +
                                                       "|-|-|-|\n" +
                                                       "| `contentletIds` | List of Strings | A list of individual contentlet identifiers. |\n" +
                                                       "| `query` | String | [Lucene query](https://www.dotcms.com/docs/latest/content-search-syntax#Lucene); " +
                                                                                        "uses all matching contentlets. |\n" +
                                                       "| `workflowActionId` | String | The identifier of the workflow action to be performed on the " +
                                                                                        "selected content. |\n" +
                                                       "| `additionalParams` | Object | Further parameters and properties are conveyed here, depending " +
                                                                                       "on the particulars of the selected action.<br><br>For a " +
                                                                                       "complete list of possible parameters, refer to the various " +
                                                                                       "keys listed in `GET /workflow/actionlets`. |\n\n" +
                                                       "If both `contentletIds` and `query` properties are present, the operation will perform the " +
                                                       "selected action on all contentlets indicated in both. Note that this will lead to the workflow " +
                                                       "action being performed on the same contentlet twice, if it appears in both.",
                                               required = true,
                                               content = @Content(schema = @Schema(implementation = FireBulkActionsForm.class))
                                       ) final FireBulkActionsForm fireBulkActionsForm)
            throws DotDataException, DotSecurityException {
        final InitDataObject initDataObject = this.webResource
                .init(null, request, new EmptyHttpResponse(), true, null);

        final EventOutput eventOutput = new EventOutput();

        DotConcurrentFactory.getInstance().getSubmitter().submit(()-> {

            final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();


            fireBulkActionsForm.getPopupParamsBean()
                    .getAdditionalParamsMap().put(SUCCESS_ACTION_CALLBACK,
                            (Consumer<Long>) delta -> {
                                eventBuilder.name("success");
                                eventBuilder.data(Map.class,
                                        Map.of("success", delta));
                                eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                                final OutboundEvent event = eventBuilder.build();
                                try {
                                    eventOutput.write(event);
                                } catch (Exception e) {
                                    throw new DotRuntimeException(e);
                                }
                            });

            fireBulkActionsForm.getPopupParamsBean()
                    .getAdditionalParamsMap().put(FAIL_ACTION_CALLBACK,
                            (BiConsumer<String, Exception>) (inode, e) -> {
                                eventBuilder.name("failure");
                                eventBuilder.data(Map.class,
                                        Map.of("failure", inode));
                                eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                                final OutboundEvent event = eventBuilder.build();
                                try {
                                    eventOutput.write(event);
                                } catch (Exception e1) {
                                    throw new DotRuntimeException(e1);
                                }
                            });

            try {
                workflowHelper.fireBulkActionsNoReturn(fireBulkActionsForm, initDataObject.getUser());
                Logger.info(this, "finished");
            } catch (DotSecurityException | DotDataException e) {
                throw new DotRuntimeException(e);
            }
        });

        return eventOutput;
    }

    /**
     * Returns a single action, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @IncludePermissions
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowActionByActionId", summary = "Find action by ID",
            description = "Returns a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) object.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response findAction(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @PathParam("actionId") @Parameter(
                                             required = true,
                                             description = "Identifier of the workflow action to return.\n\n" +
                                                     "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                                                     "(Default system workflow \"Publish\" action)",
                                             schema = @Schema(type = "string")
                                     ) final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, ()->"Finding the workflow action " + actionId);
            final WorkflowAction action = this.workflowHelper.findAction(actionId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(this.toWorkflowActionView(action))).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findAction, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // findAction.

    /**
     * Returns a single action condition evaluated, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}/condition")
    @JSONP
    @NoCache
    @IncludePermissions
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowConditionByActionId", summary = "Find condition by action ID",
            description = "Returns a string representing the \"condition\" on the selected action.\n\n" +
                    "More specifically: if the workflow action has anything in its [Custom Code]" +
                    "(https://www.dotcms.com/docs/latest/custom-workflow-actions) field, " +
                    "the result is evaluated as Velocity, and the output is returned.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Condition returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response evaluateActionCondition(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("actionId") @Parameter(
                    required = true,
                    description = "Identifier of a workflow action to check for condition.\n\n" +
                            "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                            "(Default system workflow \"Publish\" action)",
                    schema = @Schema(type = "string")
            ) final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, ()->"Finding the workflow action " + actionId);

            final String evaluated = workflowHelper.evaluateActionCondition(actionId, initDataObject.getUser(), request, response);
            return Response.ok(new ResponseEntityView(evaluated)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on evaluateActionCondition, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findAction.

    /**
     * Returns a single action associated to the step, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowActionByStepActionId", summary = "Find a workflow action within a step",
            description = "Returns a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "if it exists within a specific [step](https://www.dotcms.com/docs/latest/managing-workflows#Steps).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action returned successfully from step",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found within specified step")
            }
    )
    public final Response findActionByStep(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @PathParam("stepId") @Parameter(
                                                   required = true,
                                                   description = "Identifier of a workflow step.\n\n" +
                                                           "Example value: `ee24a4cb-2d15-4c98-b1bd-6327126451f3` " +
                                                           "(Default system workflow \"Draft\" step)",
                                                   schema = @Schema(type = "string")
                                           ) final String stepId,
                                           @PathParam("actionId") @Parameter(
                                                   required = true,
                                                   description = "Identifier of a workflow action.\n\n" +
                                                           "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                                                           "(Default system workflow \"Publish\" action)",
                                                   schema = @Schema(type = "string")
                                           ) final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, "Getting the workflow action " + actionId + " for the step: " + stepId);
            final WorkflowAction action = this.workflowHelper.findAction(actionId, stepId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(action)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findAction, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionByStep.

    /**
     * Returns a collection of actions associated to the step, if step does not have any will returns 200 and an empty list.
     * 401 if the user does not have permission.
     * 404 if the stepId does not exists.
     * @param request  HttpServletRequest
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowActionsByStepId", summary = "Find all actions in a workflow step",
            description = "Returns a list of [workflow actions](https://www.dotcms.com/docs/latest/managing" +
                    "-workflows#Actions) associated with a specified [workflow step](https://www.dotcms.com/" +
                    "docs/latest/managing-workflows#Steps).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Actions returned successfully from step",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow step not found")
            }
    )
    public final Response findActionsByStep(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @PathParam("stepId") @Parameter(
                                                    required = true,
                                                    description = "Identifier of a workflow step.\n\n" +
                                                            "Example value: `ee24a4cb-2d15-4c98-b1bd-6327126451f3` " +
                                                            "(Default system workflow \"Draft\" step)",
                                                    schema = @Schema(type = "string")
                                            ) final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        final User user = initDataObject.getUser();
        try {
            Logger.debug(this, "Getting the workflow actions for the step: " + stepId);
            final List<WorkflowAction> actions = this.workflowHelper.findActions(stepId, user);
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsByStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionByStep.

    /**
     * Returns a set of actions associated to the schemeId
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowActionsBySchemeId", summary = "Find all actions in a workflow scheme",
            description = "Returns a list of [workflow actions](https://www.dotcms.com/docs/latest/managing-" +
                    "workflows#Actions) associated with a specified [workflow scheme](https://www.dotcms.com/" +
                    "docs/latest/managing-workflows#Schemes).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Actions returned successfully from workflow scheme",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow scheme not found")
            }
    )
    public final Response findActionsByScheme(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @PathParam("schemeId") @Parameter(
                                                      required = true,
                                                      description = "Identifier of workflow scheme.\n\n" +
                                                              "Example value: `d61a59e1-a49c-46f2-a929-db2b4bfa88b2` " +
                                                              "(Default system workflow)",
                                                      schema = @Schema(type = "string")
                                              ) final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, "Getting the workflow actions: " + schemeId);
            final List<WorkflowAction> actions = this.workflowHelper.findActionsByScheme(schemeId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionsByScheme.

    /**
     * Returns a set of {@link WorkflowAction} associated to a set to schemes Id and {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @param request  HttpServletRequest
     * @param response {@link HttpServletResponse}
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @return Response
     */
    @POST
    @Path("/schemes/actions/{systemAction}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postFindActionsBySchemesAndSystemAction", summary = "Finds workflow actions by schemes and system action",
            description = "Returns a list of [workflow actions](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "associated with [workflow schemes](https://www.dotcms.com/docs/latest/managing-workflows#Schemes), further " +
                    "filtered by [default system actions](https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response findActionsBySchemesAndSystemAction(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam("systemAction") @Parameter(
                                                            required = true,
                                                            schema = @Schema(
                                                                    type = "string",
                                                                    allowableValues = {
                                                                            "NEW", "EDIT", "PUBLISH",
                                                                            "UNPUBLISH", "ARCHIVE", "UNARCHIVE",
                                                                            "DELETE", "DESTROY"
                                                                    }
                                                            ),
                                                            description = "Default system action."
                                                    ) final WorkflowAPI.SystemAction systemAction,
                                                    @RequestBody(
                                                            description = "Body consists of a JSON object containing " +
                                                                    "a single property called `schemes`, which contains a " +
                                                                    "list of workflow scheme identifier strings.",
                                                            required = true,
                                                            content = @Content(
                                                                    schema = @Schema(implementation = WorkflowSchemesForm.class),
                                                                    examples = @ExampleObject(
                                                                            value = "{\n" +
                                                                                    "  \"schemes\": [\n" +
                                                                                    "    \"d61a59e1-a49c-46f2-a929-db2b4bfa88b2\"\n" +
                                                                                    "  ]\n" +
                                                                                    "}"
                                                                    )
                                                            )
                                                    ) final WorkflowSchemesForm workflowSchemesForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        DotPreconditions.notNull(workflowSchemesForm,"Expected Request body was empty.");
        try {

            Logger.debug(this, ()->"Getting the actions for the schemes: " + workflowSchemesForm.getSchemes()
                    + " and system action: " + systemAction);

            return Response.ok(new ResponseEntityView<>(
                    this.workflowHelper.findActions(
                            workflowSchemesForm.getSchemes(), systemAction, initDataObject.getUser())))
                    .build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsBySchemesAndSystemAction, schemes: " + workflowSchemesForm.getSchemes() +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSystemActionsByScheme.

    /**
     * Returns a set of {@link SystemActionWorkflowActionMapping} associated to the schemeId
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/system/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getSystemActionMappingsBySchemeId", summary = "Find default system actions mapped to a workflow scheme",
            description = "Returns a list of [default system actions](https://www.dotcms.com/docs/latest/managing-" +
                    "workflows#DefaultActions) associated with a specified [workflow scheme](https://www.dotcms.com" +
                    "/docs/latest/managing-workflows#Schemes).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Actions returned successfully from workflow scheme",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySystemActionWorkflowActionMappings.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow scheme not found")
            }
    )
    public final Response findSystemActionsByScheme(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @PathParam("schemeId") @Parameter(
                                                      required = true,
                                                      description = "Identifier of workflow scheme.\n\n" +
                                                              "Example value: `d61a59e1-a49c-46f2-a929-db2b4bfa88b2` " +
                                                              "(Default system workflow)",
                                                      schema = @Schema(type = "string")
                                              ) final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this, "Getting the system actions for the scheme: " + schemeId);
            final List<SystemActionWorkflowActionMapping> systemActions =
                    this.workflowAPI.findSystemActionsByScheme(this.workflowAPI.findScheme(schemeId), initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(systemActions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findSystemActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSystemActionsByScheme.

    /**
     * Returns a set of {@link SystemActionWorkflowActionMapping} associated to the content type
     * @param request  HttpServletRequest
     * @param contentTypeVarOrId String
     * @return Response
     */
    @GET
    @Path("/contenttypes/{contentTypeVarOrId}/system/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getSystemActionMappingsByContentType", summary = "Find default system actions mapped to a content type",
            description = "Returns a list of [default system actions](https://www.dotcms.com/docs/latest/managing-" +
                    "workflows#DefaultActions) associated with a specified [content type](https://www.dotcms.com" +
                    "/docs/latest/content-types).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action(s) returned successfully from content type",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySystemActionWorkflowActionMappings.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Content Type not found")
            }
    )
    public final Response findSystemActionsByContentType(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam("contentTypeVarOrId") @Parameter(
                                                            required = true,
                                                            description = "The ID or Velocity variable of the content type to inspect" +
                                                                    "for default system action bindings.\n\n" +
                                                                    "Example value: `htmlpageasset` (Default page content type)",
                                                            schema = @Schema(type = "string")
                                                    ) final String contentTypeVarOrId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this, "Getting the system actions for the content type: " + contentTypeVarOrId);
            final User user = initDataObject.getUser();
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(contentTypeVarOrId);
            final List<SystemActionWorkflowActionMapping> systemActions =
                    this.workflowHelper.findSystemActionsByContentType(contentType, initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(systemActions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findSystemActionsByContentType, content type: " + contentTypeVarOrId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSystemActionsByScheme.

    /**
     * Will retrieve the references as a default {@link WorkflowAction}
     * @param request  HttpServletRequest
     * @param workflowActionId String
     * @return Response
     */
    @GET
    @Path("/system/actions/{workflowActionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getSystemActionsByActionId", summary = "Find default system actions by workflow action id",
            description = "Returns a list of [default system actions]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) associated with a " +
                    "specified [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySystemActionWorkflowActionMappings.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response getSystemActionsReferredByWorkflowAction(@Context final HttpServletRequest request,
                                                         @Context final HttpServletResponse response,
                                                         @PathParam("workflowActionId") @Parameter(
                                                                 required = true,
                                                                 description = "Identifier of the workflow action to return.\n\n" +
                                                                         "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                                                                         "(Default system workflow \"Publish\" action)",
                                                                 schema = @Schema(type = "string")
                                                         ) final String workflowActionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this, ()->"Getting the system actions for the workflow action id: " + workflowActionId);
            final User user = initDataObject.getUser();
            final WorkflowAction workflowAction = this.workflowHelper.findAction(workflowActionId, user);
            final List<SystemActionWorkflowActionMapping> systemActions =
                    this.workflowAPI.findSystemActionsByWorkflowAction(workflowAction, user);
            return Response.ok(new ResponseEntityView<>(systemActions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on getSystemActionsReferredByWorkflowAction, workflowActionId: " + workflowActionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // getSystemActionsReferredByWorkflowAction.


    /**
     * Saves an {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}, by default the action is associated to the schema,
     * however if the stepId is set will be automatically associated to the step too.
     * @param request                     HttpServletRequest
     * @param response                    HttpServletResponse
     * @param workflowSystemActionForm    WorkflowActionForm
     * @return Response
     */
    @PUT
    @Path("/system/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putSaveSystemActions", summary = "Save a default system action mapping",
            description = "This operation allows you to save a [default system action]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) mapping. This requires:\n\n" +
                    "1. Selecting a default system action to be mapped;\n" +
                    "2. Specifying a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                        "to be performed when that system action is called;\n" +
                    "3. Associating this mapping with either a [workflow scheme](https://www.dotcms.com/docs/latest" +
                        "/managing-workflows#Schemes) or a [content type](https://www.dotcms.com/docs/latest" +
                    "/content-types).\n\n" +
                    "See the request body below for further details.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBulkActionsResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response saveSystemAction(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @RequestBody(
                                                   description = "Body consists of a JSON object with the following properties:\n\n" +
                                                           "| Property | Type | Description |\n" +
                                                           "|-|-|-|\n" +
                                                           "| `systemAction` | String | A default system action, such as `NEW` or `PUBLISH`. |\n" +
                                                           "| `actionId` | String | The identifier of an action that will be performed " +
                                                                                    "by the specified system action. |\n" +
                                                           "| `schemeId` | String | The identifier of a workflow scheme to be associated " +
                                                                                    "with the system action. |\n" +
                                                           "| `contentTypeVariable` | String | The variable of a content type to be " +
                                                           "associated with the system action. Note that the content type must already " +
                                                           "have the schema assigned as one of its valid workflows in order to bind " +
                                                           "a system action from said schema. |\n\n" +
                                                           "If both the `schemeId` and `contentTypeVariable` are specified, the scheme " +
                                                           "identifier takes precedence, and the content type variable is disregarded.",
                                                   required = true,
                                                   content = @Content(schema = @Schema(implementation = WorkflowSystemActionForm.class))
                                           ) final WorkflowSystemActionForm workflowSystemActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            DotPreconditions.notNull(workflowSystemActionForm,"Expected Request body was empty.");
            Logger.debug(this, ()-> "Saving system action: " + workflowSystemActionForm.getSystemAction() +
                                            ", map to action id: " + workflowSystemActionForm.getActionId() +
                                            " and " + (UtilMethods.isSet(workflowSystemActionForm.getSchemeId())?
                                            "scheme: " + workflowSystemActionForm.getSchemeId():
                                            "var: "    + workflowSystemActionForm.getContentTypeVariable()));

            return Response.ok(new ResponseEntityView(
                    this.workflowHelper.mapSystemActionToWorkflowAction(workflowSystemActionForm, initDataObject.getUser())))
                    .build(); // 200
        }  catch (final Exception e) {

            Logger.error(this.getClass(),
                    "Exception on save, workflowActionForm: " + workflowSystemActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveSystemAction


    /**
     * Deletes an {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}, by default the action is associated to the schema,
     * however if the stepId is set will be automatically associated to the step too.
     * @param request                     HttpServletRequest
     * @param response                    HttpServletResponse
     * @param identifier                  String
     * @return Response
     */
    @DELETE
    @Path("/system/actions/{identifier}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "deleteSystemActionByActionId", summary = "Delete default system action binding by action id",
            description = "Deletes a [default system action]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) binding.\n\n" +
                    "Returns the deleted system action object.\n\n" +
                    "This method is minimally destructive, as it neither deletes a [workflow action](https://www.dotcms" +
                    ".com/docs/latest/managing-workflows#Actions), nor removes any system action category. Instead, it " +
                    "dissolves the association between the two, which can be re-established any time.\n\n" +
                    "To find a suitable identifier, you can use `GET /system/actions/{workflowActionId}` " +
                    "and find it in the immediate `identifier` property of any of the objects returned in the entity.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "System action binding deleted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySystemActionWorkflowActionMapping.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response deletesSystemAction(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @PathParam("identifier") @Parameter(
                                                      required = true,
                                                      description = "Identifier of the system action mapping to delete.\n\n" +
                                                              "Example value: `59995336-187e-442a-b398-04b9f137eabd` " +
                                                              "(Demo starter binding that maps `DELETE` system action to " +
                                                              "the \"Destroy\" workflow action for the Blog content type)",
                                                      schema = @Schema(type = "string")
                                              ) final String identifier) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, ()-> "Deleting system action: " + identifier);

            return Response.ok(new ResponseEntityView(
                    this.workflowHelper.deleteSystemAction(identifier, initDataObject.getUser())))
                    .build(); // 200
        }  catch (final Exception e) {

            Logger.error(this.getClass(),
                    "Exception on delete System Action, identifier: " + identifier +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveSystemAction

    /**
     * Saves an action, by default the action is associated to the schema, however if the stepId is set will be automatically associated to the step too.
     * @param request               HttpServletRequest
     * @param workflowActionForm    WorkflowActionForm
     * @return Response
     */
    @POST
    @Path("/actions")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "postActionsByWorkflowActionForm", summary = "Creates/saves a workflow action",
            description = "Creates or updates a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "from the properties specified in the payload. Returns the created workflow action.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow action created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response saveAction(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                                              @RequestBody(
                                                                      description = "Body consists of a JSON object containing " +
                                                                              "a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                                                                              "form. This includes the following properties:\n\n" +
                                                                              "| Property | Type | Description |\n" +
                                                                              "|-|-|-|\n" +
                                                                              "| `actionId` | String | The identifier of the workflow action to be updated. " +
                                                                                                        "If left blank, a new workflow action will be created. |\n" +
                                                                              "| `schemeId` | String | The [workflow scheme](https://www.dotcms.com/docs/latest" +
                                                                                                        "/managing-workflows#Schemes) under which the action will be created. |\n" +
                                                                              "| `stepId` | String |  The [workflow step](https://www.dotcms.com/docs/latest" +
                                                                                                        "/managing-workflows#Steps) with which to associate the action. |\n" +
                                                                              "| `actionName` | String | The name of the workflow action. Multiple actions of the " +
                                                                                                        "same name can coexist with different identifiers.  |\n" +
                                                                              "| `whoCanUse` | List of Strings | A list of identifiers representing [users]" +
                                                                                                        "(https://www.dotcms.com/docs/latest/user-management), " +
                                                                                                        "[role keys](https://www.dotcms.com/docs/latest/adding-roles), " +
                                                                                                        "or [other user categories](https://www.dotcms.com" +
                                                                                                        "/docs/latest/managing-workflows#ActionWho) allowed " +
                                                                                                        "to use this action. This list can be empty. |\n" +
                                                                              "| `actionIcon` | String | The icon to associate with the action. Example: `workflowIcon`.  |\n" +
                                                                              "| `actionCommentable` | Boolean | Whether this action supports comments.  |\n" +
                                                                              /* "| `requiresCheckout` | Boolean |   |\n" + // This is a deprecated, unnecessary, and broadly unused property. */
                                                                              "| `showOn` | List of Strings | List defining under which of the eight valid [workflow states]" +
                                                                                                        "(https://www.dotcms.com/docs/latest/managing-workflows#ActionShow) the " +
                                                                                                        "action is visible. States must be specified uppercase, such as `NEW` or " +
                                                                                                        "`LOCKED`. There is no single state for ALL; each state must be listed. |\n" +
                                                                              "| `actionNextStep` | String | The identifier of the step to enter after performing the action; " +
                                                                                                            "`currentstep` is also a valid value. |\n" +
                                                                              "| `actionNextAssign` | String | A user identifier or role key (such as `CMS Anonymous`) to serve as the " +
                                                                                                        " default entry in the assignment dropdown. |\n" +
                                                                              "| `actionCondition` | String | [Custom Velocity code](https://www.dotcms.com/docs/latest/managing-workflows#" +
                                                                                                        "ActionAssign) to be executed along with the action. |\n" +
                                                                              "| `actionAssignable` | Boolean | Whether this action can be assigned.  |\n" +
                                                                              "| `actionRoleHierarchyForAssign` | Boolean | If true, non-administrators cannot assign tasks to administrators.  |\n" +
                                                                              "| `metadata` | Object | Additional metadata to include in the action definition. |\n\n",
                                                                      required = true,
                                                                      content = @Content(
                                                                              schema = @Schema(implementation = WorkflowActionForm.class)
                                                                      )
                                                              ) final WorkflowActionForm workflowActionForm) throws NotFoundException {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        WorkflowAction newAction;

        try {
            DotPreconditions.notNull(workflowActionForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving new workflow action: " + workflowActionForm.getActionName());
            newAction = this.workflowHelper.saveAction(workflowActionForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(newAction)).build(); // 200

        }  catch (final Exception e) {

            Logger.error(this.getClass(),
                    "Exception on save, workflowActionForm: " + workflowActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // save

    /**
     * Saves an action separator to a schema and step.
     * @param request               {@link HttpServletRequest}
     * @param response              {@link HttpServletResponse}
     * @param workflowActionForm    {@link WorkflowActionSeparatorForm}
     * @return Response
     */
    @POST
    @Path("/actions/separator")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "addSeparatorAction", summary = "Creates workflow action separator",
            description = "Creates a [workflow action] separator(https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "from the properties specified in the payload. Returns the created workflow action.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow action created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = WorkflowAction.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final WorkflowActionView addSeparatorAction(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @RequestBody(
                                             description = "Body consists of a JSON object containing " +
                                                     "a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                                                     "form. This includes the following properties:\n\n" +
                                                     "| Property | Type | Description |\n" +
                                                     "|-|-|-|\n" +
                                                     "| `schemeId` | String | The [workflow scheme](https://www.dotcms.com/docs/latest" +
                                                     "/managing-workflows#Schemes) under which the action will be created. |\n" +
                                                     "| `stepId` | String |  The [workflow step](https://www.dotcms.com/docs/latest",
                                             required = true,
                                             content = @Content(
                                                     schema = @Schema(implementation = WorkflowActionSeparatorForm.class)
                                             )
                                     ) final WorkflowActionSeparatorForm workflowActionForm) throws NotFoundException {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        Logger.debug(this, ()-> "Saving new workflow action separator to the scheme id: " + workflowActionForm.getSchemeId() +
                " and step id: " + workflowActionForm.getStepId());
        DotPreconditions.notNull(workflowActionForm,"Expected Request body was empty.");
        final WorkflowActionForm.Builder builder = new WorkflowActionForm.Builder();
        builder.separator(workflowActionForm.getSchemeId(), workflowActionForm.getStepId());
        builder.whoCanUse(Collections.emptyList());
        final WorkflowAction newAction = this.workflowHelper.saveAction(builder.build(), initDataObject.getUser());
        return toWorkflowActionView(newAction);
    } // addSeparatorAction

    /**
     * Updates an existing action
     * @param request HttpServletRequest
     * @param actionId String
     * @param workflowActionForm WorkflowActionStepForm
     * @return Response
     */
    @PUT
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putSaveActionsByWorkflowActionForm", summary = "Update an existing workflow action",
            description = "Updates a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "based on the payload properties.\n\nReturns updated workflow action.\n\n",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated workflow action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowActionView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response updateAction(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("actionId") @Parameter(
                                               required = true,
                                               description = "Identifier of workflow action to update.\n\n" +
                                                       "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                                                       "(Default system workflow \"Publish\" action)",
                                               schema = @Schema(type = "string")
                                       ) final String actionId,
                                       @RequestBody(
                                               description = "Body consists of a JSON object containing " +
                                                       "the same form data as used above in `POST /v1/workflow/actions`. However, " +
                                                       "this endpoint uses the form's properties differently, as noted below:\n\n" +
                                                       "| Property | Type | Description |\n" +
                                                       "|-|-|-|\n" +
                                                       "| `schemeId` | String | The [workflow scheme](https://www.dotcms.com/docs/latest" +
                                                       "/managing-workflows#Schemes) under which the action will be created. |\n" +
                                                       "| `actionName` | String | The name of the workflow action. Multiple actions of the " +
                                                       "same name can coexist with different identifiers.  |\n" +
                                                       "| `whoCanUse` | List of Strings | A list of identifiers representing [users]" +
                                                       "(https://www.dotcms.com/docs/latest/user-management), " +
                                                       "[role keys](https://www.dotcms.com/docs/latest/adding-roles), " +
                                                       "or [other user categories](https://www.dotcms.com" +
                                                       "/docs/latest/managing-workflows#ActionWho) allowed to use this action. This list can be empty. |\n" +
                                                       "| `actionIcon` | String | The icon to associate with the action. Example: `workflowIcon`.  |\n" +
                                                       "| `actionCommentable` | Boolean | Whether this action supports comments.  |\n" +
                                                       /* "| `requiresCheckout` | Boolean |   |\n" + // This is a deprecated, unnecessary, and broadly unused property. */
                                                       "| `showOn` | List of Strings | List defining under which of the eight valid [workflow states]" +
                                                       "(https://www.dotcms.com/docs/latest/managing-workflows#ActionShow) the " +
                                                       "action is visible. States must be specified uppercase, such as `NEW` or " +
                                                       "`LOCKED`. There is no single state for ALL; each state must be listed. |\n" +
                                                       "| `actionNextStep` | String | The identifier of the step to enter after performing the action; " +
                                                                                    "`currentstep` is also a valid value. |\n" +
                                                       "| `actionNextAssign` | String | A user identifier or role key (such as `CMS Anonymous`) to serve as the " +
                                                       " default entry in the assignment dropdown. |\n" +
                                                       "| `actionCondition` | String | [Custom Velocity code](https://www.dotcms.com/docs/latest/managing-workflows#" +
                                                       "ActionAssign) to be executed along with the action. |\n" +
                                                       "| `actionAssignable` | Boolean | Whether this action can be assigned.  |\n" +
                                                       "| `actionRoleHierarchyForAssign` | Boolean | If true, non-administrators cannot assign tasks to administrators.  |\n" +
                                                       "| `metadata` | Object | Optional. Additional metadata to include in the action definition. |\n" +
                                                       "| `actionId` | String | Omit; not used in this endpoint. |\n" +
                                                       "| `stepId` | String | Omit; not used in this endpoint. |\n\n",
                                               required = true,
                                               content = @Content(schema = @Schema(implementation = WorkflowActionForm.class))
                                       ) final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        try {
            DotPreconditions.notNull(workflowActionForm,"Expected Request body was empty.");
            Logger.debug(this, "Updating action with id: " + actionId);
            final WorkflowAction workflowAction = this.workflowHelper.updateAction(actionId, workflowActionForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(workflowAction)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on updateAction, actionId: " +actionId+", workflowActionForm: " + workflowActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Saves an action into a step
     * @param request HttpServletRequest
     * @param workflowActionStepForm WorkflowActionStepForm
     * @return Response
     */
    @POST
    @Path("/steps/{stepId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postActionToStepById", summary = "Adds a workflow action to a workflow step",
            description = "Assigns a single [workflow action](https://www.dotcms.com/docs/latest" +
                    "/managing-workflows#Actions) to a [workflow step](https://www.dotcms.com/docs" +
                    "/latest/managing-workflows#Steps). Returns \"Ok\" on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow action added to step successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class),
                                    examples = @ExampleObject(
                                            value = "{\n" +
                                                    "  \"errors\": [\n" +
                                                    "    {\n" +
                                                    "      \"errorCode\": \"string\",\n" +
                                                    "      \"message\": \"string\",\n" +
                                                    "      \"fieldName\": \"string\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"entity\": \"Ok\",\n" +
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
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response saveActionToStep(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @PathParam("stepId") @Parameter(
                                                   required = true,
                                                   description = "Identifier of a workflow step to receive a new action.\n\n" +
                                                           "Example value: `ee24a4cb-2d15-4c98-b1bd-6327126451f3` " +
                                                           "(Default system workflow \"Draft\" step)",
                                                   schema = @Schema(type = "string")
                                           ) final String stepId,
                                           @RequestBody(
                                                   description = "Body consists of a JSON object with a single property:\n\n" +
                                                           "| Property | Type | Description |\n" +
                                                           "|-|-|-|\n" +
                                                           "| `actionId` | String | The identifier of the workflow action " +
                                                                                    "to assign to the step specified in the " +
                                                                                    "parameter. |\n\n",
                                                   required = true,
                                                   content = @Content(schema = @Schema(implementation = WorkflowActionStepForm.class),
                                                                       examples = @ExampleObject(
                                                                               value = "{\n" +
                                                                                       "  \"actionId\": " +
                                                                                       "\"b9d89c80-3d88-4311-8365-187323c96436\"\n" +
                                                                                       "}"
                                                                       )
                                                   )
                                           ) final WorkflowActionStepForm workflowActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            DotPreconditions.notNull(workflowActionStepForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving a workflow action " + workflowActionStepForm.getActionId()
                    + " in to a step: " + stepId);
            this.workflowHelper.saveActionToStep(new WorkflowActionStepBean.Builder().stepId(stepId)
                    .actionId(workflowActionStepForm.getActionId()).build(), initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on saveActionToStep, stepId: "+stepId+", saveActionToStep: " + workflowActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveActionToStep

    /**
     * Saves an actionlet into an action,
     * if the Parameters (key/value) are not null nor empty, overrides them.
     * @param request HttpServletRequest
     * @param workflowActionletActionForm WorkflowActionletActionForm
     * @return Response
     */
    @POST
    @Path("/actions/{actionId}/actionlets")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postAddActionletToActionById", summary = "Adds an actionlet to a workflow action",
            description = "Adds an actionlet — a.k.a. a [workflow sub-action]" +
                    "(https://www.dotcms.com/docs/latest/workflow-sub-actions) — to a [workflow action]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#Actions).\n\n" +
                    "Returns \"Ok\" on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow actionlet assigned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class),
                                    examples = @ExampleObject(
                                            value = "{\n" +
                                                    "  \"errors\": [\n" +
                                                    "    {\n" +
                                                    "      \"errorCode\": \"string\",\n" +
                                                    "      \"message\": \"string\",\n" +
                                                    "      \"fieldName\": \"string\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"entity\": \"Ok\",\n" +
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
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response saveActionletToAction(@Context final HttpServletRequest request,
                                                @PathParam("actionId") @Parameter(
                                                        required = true,
                                                        description = "Identifier of workflow action to receive actionlet.\n\n" +
                                                                "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                                                                "(Default system workflow \"Publish\" action)",
                                                        schema = @Schema(type = "string")
                                                ) final String actionId,
                                                @RequestBody(
                                                        description = "Body consists of a JSON object containing " +
                                                                "a workflow action form. This includes the following properties:\n\n" +
                                                                "| Property | Type | Description |\n" +
                                                                "|-|-|-|\n" +
                                                                "| `actionletClass` | String | The class of the actionlet to be assigned.<br><br>Example: " +
                                                                                                "`com.dotcms.rendering.js.JsScriptActionlet` |\n" +
                                                                "| `order` | Integer | The position of the actionlet within the action's sequence. |\n" +
                                                                "| `parameters` | Object | Further parameters and properties are conveyed here, depending " +
                                                                                            "on the particulars of the selected actionlet.<br><br>For a complete list of " +
                                                                                            "possible parameters, refer to the various keys listed in " +
                                                                                            "`GET /workflow/actionlets`. |\n\n",
                                                        required = true,
                                                        content = @Content(
                                                                schema = @Schema(implementation = WorkflowActionletActionForm.class)
                                                        )
                                                ) final WorkflowActionletActionForm workflowActionletActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {

            DotPreconditions.notNull(workflowActionletActionForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving a workflow actionlet " + workflowActionletActionForm.getActionletClass()
                    + " in to a action : " + actionId);
            this.workflowHelper.saveActionletToAction(new WorkflowActionletActionBean.Builder().actionId(actionId)
                            .actionletClass(workflowActionletActionForm.getActionletClass())
                            .order(workflowActionletActionForm.getOrder())
                            .parameters(workflowActionletActionForm.getParameters()).build()
                    , initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on saveActionletToAction, actionId: "+actionId+", saveActionletToAction: " + workflowActionletActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveActionletToAction



    /**
     * Deletes a step
     * @param request HttpServletRequest
     * @param stepId String
     */
    @DELETE
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "deleteWorkflowStepById", summary = "Delete a workflow step",
            description = "Deletes a [step](https://www.dotcms.com/docs/latest/managing-workflows#Steps) from a " +
                    "[workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).\n\n" +
                    "Returns the deleted workflow step object.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow step deleted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowStepView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final void deleteStep(@Context final HttpServletRequest request,
                                 @Suspended final AsyncResponse asyncResponse,
                                 @PathParam("stepId") @Parameter(
                                         required = true,
                                         description = "Identifier of a workflow step to delete.",
                                         schema = @Schema(type = "string")
                                 )  final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, new EmptyHttpResponse(), true, null);

        try {
            Logger.debug(this, "Deleting the step: " + stepId);
            ResponseUtil.handleAsyncResponse(this.workflowHelper.deleteStep(stepId, initDataObject.getUser()), asyncResponse);
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    } // deleteStep

    /**
     * Deletes an action associated to the step
     * @param request                   HttpServletRequest
     * @param stepId                   String
     * @return Response
     */
    @DELETE
    @Path("/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "deleteWorkflowActionFromStepByActionId", summary = "Remove a workflow action from a step",
            description = "Deletes an [action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) from a " +
                    "single [workflow step](https://www.dotcms.com/docs/latest/managing-workflows#Steps).\n\n" +
                    "Returns \"Ok\" on success.\n\n" +
                    "If the action exists on other steps, removing it from one step will not delete the action outright.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow action removed from step successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response deleteAction(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("actionId") @Parameter(
                                               required = true,
                                               description = "Identifier of the workflow action to remove.",
                                               schema = @Schema(type = "string")
                                       ) final String actionId,
                                       @PathParam("stepId") @Parameter(
                                               required = true,
                                               description = "Identifier of the step containing the action.",
                                               schema = @Schema(type = "string")
                                       )  final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, "Deleting the action: " + actionId + " for the step: " + stepId);
            this.workflowHelper.deleteAction(actionId, stepId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteAction, actionId: "+actionId+", stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Deletes an action associated to the scheme and all references into steps
     * @param request                   HttpServletRequest
     * @param actionId                  String
     * @return Response
     */
    @DELETE
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "deleteWorkflowActionByActionId", summary = "Delete a workflow action",
            description = "Deletes a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions) " +
                    "from all [steps](https://www.dotcms.com/docs/latest/managing-workflows#Steps) in which it appears.\n\n" +
                    "Returns \"Ok\" on success.\n\n",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow action deleted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response deleteAction(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("actionId") @Parameter(
                                               required = true,
                                               description = "Identifier of the workflow action to delete.",
                                               schema = @Schema(type = "string")
                                       ) final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, "Deleting the action: " + actionId);
            this.workflowHelper.deleteAction(actionId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Deletes an actionlet associated
     * @param request                   HttpServletRequest
     * @param actionletId                  String
     * @return Response
     */
    @DELETE
    @Path("/actionlets/{actionletId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "deleteWorkflowActionletFromAction", summary = "Remove an actionlet from a workflow action",
            description = "Removes an [actionlet](https://www.dotcms.com/docs/latest/workflow-sub-actions), or sub-action, " +
                    "from a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions). This deletes " +
                    "only the actionlet's binding to the action utilizing it, and leaves the actionlet category intact.\n\n" +
                    "To find the identifier, you can call `GET /workflow/actions/{actionId}/actionlets`.\n\n" +
                    "Returns \"Ok\" on success.\n\n",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow actionlet deleted from action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response deleteActionlet(@Context final HttpServletRequest request,
                                          @PathParam("actionletId") @Parameter(
                                                  required = true,
                                                  description = "Identifier of the actionlet to delete.",
                                                  schema = @Schema(type = "string")
                                          ) final String actionletId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

        try {

            Logger.debug(this, "Deleting the actionletId: " + actionletId);

            DotPreconditions.notNull(actionletId,
                    ()-> "Not is not allowed for the parameter actionletId on the method deleteActionlet.",
                    IllegalArgumentException.class);

            final WorkflowActionClass actionClass = this.workflowAPI.findActionClass(actionletId);

            DotPreconditions.notNull(actionClass,
                    ()-> "The Actionlet: " + actionletId + ", does not exists",
                    DoesNotExistException.class);

            this.workflowAPI.deleteActionClass(actionClass, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteActionlet, actionletId: " + actionletId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Change the order of the steps in a scheme
     * @param request                           HttpServletRequest
     * @param stepId                            String stepid to reorder
     * @param order                             int    order for the step
     * @return Response
     */
    @PUT
    @Path("/reorder/step/{stepId}/order/{order}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putReorderWorkflowStepsInScheme", summary = "Change the order of steps within a scheme",
            description = "Updates a [workflow step](https://www.dotcms.com/docs/latest/managing-workflows#Steps)'s " +
                    "order within a [scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes) by " +
                    "assigning it a numeric order.\n\nReturns \"Ok\" on success.\n\n",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow step reordered successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response reorderStep(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                        @PathParam("stepId") @Parameter(
                                                required = true,
                                                description = "Identifier of the step to reorder.\n\n" +
                                                        "Example: `ee24a4cb-2d15-4c98-b1bd-6327126451f3` (Default system workflow Draft step)",
                                                schema = @Schema(type = "string")
                                        ) final String stepId,
                                      @PathParam("order")  @Parameter(
                                              required = true,
                                              description = "Integer indicating the step's position in the order, with `0` as the first. " +
                                                      "All other steps numbers are adjusted accordingly, leaving no gaps.",
                                              schema = @Schema(type = "integer")
                                      )  final int order) {
        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, "Doing reordering of step: " + stepId + ", order: " + order);
            this.workflowHelper.reorderStep(stepId, order, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "WorkflowPortletAccessException on reorderStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // reorderStep


    /**
     * Updates an existing step
     * @param request HttpServletRequest
     * @param stepId String
     * @param stepForm WorkflowStepUpdateForm
     * @return Response
     */
    @PUT
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putUpdateWorkflowStepById", summary = "Update an existing workflow step",
            description = "Updates a [workflow step](https://www.dotcms.com/docs/latest/managing-workflows#Steps).\n\n" +
                    "Returns an object representing the updated step.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated workflow step successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowStepView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response updateStep(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @NotNull @PathParam("stepId") @Parameter(
                                             required = true,
                                             description = "Identifier of the step to update.\n\n" +
                                                     "Example: `ee24a4cb-2d15-4c98-b1bd-6327126451f3` (Default system workflow Draft step)",
                                             schema = @Schema(type = "string")
                                     ) final String stepId,
                                     @RequestBody(
                                             description = "Body consists of a JSON object containing " +
                                                     "a workflow step update form. This includes the following properties:\n\n" +
                                                     "| Property | Type | Description |\n" +
                                                     "|-|-|-|\n" +
                                                     "| `stepOrder` | Integer | The position of the step within the [workflow scheme]" +
                                                                                "(https://www.dotcms.com/docs/latest/managing-workflows#Schemes), " +
                                                                                "with `0` being the first. |\n" +
                                                     "| `stepName` | String | The name of the workflow step. |\n" +
                                                     "| `enableEscalation` | Boolean | Determines whether a step is capable of automatic escalation " +
                                                                                    "to the next step. (Read more about [schedule-enabled workflows]" +
                                                                                    "(https://www.dotcms.com/docs/latest/schedule-enabled-workflow).) |\n" +
                                                     "| `escalationAction` | String | The identifier of the workflow action to execute on automatic escalation. |\n" +
                                                     "| `escalationTime` | String | The time, in seconds, before the workflow automatically escalates. |\n" +
                                                     "| `stepResolved` | Boolean | If true, any content which enters this workflow step will be considered resolved.\n" +
                                                                                    "Content in a resolved step will not appear in the workflow queues of any users.\n |\n\n",
                                             required = true,
                                             content = @Content(
                                                     schema = @Schema(implementation = WorkflowStepUpdateForm.class)
                                             )
                                     ) final WorkflowStepUpdateForm stepForm) {
        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        Logger.debug(this, "updating step for scheme with stepId: " + stepId);
        try {
            DotPreconditions.notNull(stepForm,"Expected Request body was empty.");
            final WorkflowStep step = this.workflowHelper.updateStep(stepId, stepForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(step)).build();
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "WorkflowPortletAccessException on updateStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // updateStep

    /**
     * Creates a new step into a workflow
     * @param request HttpServletRequest
     * @param newStepForm WorkflowStepAddForm
     * @return Response
     */
    @POST
    @Path("/steps")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postAddWorkflowStep", summary = "Add a new workflow step",
            description = "Creates a [workflow step](https://www.dotcms.com/docs/latest/managing-workflows#Steps).\n\n" +
                    "Returns an object representing the step.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Created workflow step successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowStepView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response addStep(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @RequestBody(
                                          description = "Body consists of a JSON object containing " +
                                                  "a workflow step update form. This includes the following properties:\n\n" +
                                                  "| Property | Type | Description |\n" +
                                                  "|-|-|-|\n" +
                                                  "| `schemeId` | String | The identifier of the [workflow scheme](https://www.dotcms.com/docs" +
                                                                            "/latest/managing-workflows#Schemes) to which the step will be added. |\n" +
                                                  "| `stepName` | String | The name of the workflow step. |\n" +
                                                  "| `enableEscalation` | Boolean | Determines whether a step is capable of automatic escalation " +
                                                                            "to the next step. (Read more about [schedule-enabled workflows]" +
                                                                            "(https://www.dotcms.com/docs/latest/schedule-enabled-workflow).) |\n" +
                                                  "| `escalationAction` | String | The identifier of the workflow action to execute on automatic escalation. |\n" +
                                                  "| `escalationTime` | String | The time, in seconds, before the workflow automatically escalates. |\n" +
                                                  "| `stepResolved` | Boolean | If true, any content which enters this workflow step will be considered resolved.\n" +
                                                                            "Content in a resolved step will not appear in the workflow queues of any users.\n |\n\n",
                                          required = true,
                                          content = @Content(
                                                  schema = @Schema(implementation = WorkflowStepAddForm.class)
                                          )
                                  ) final WorkflowStepAddForm newStepForm) {
        String schemeId = null;
        try {
            DotPreconditions.notNull(newStepForm,"Expected Request body was empty.");
            schemeId = newStepForm.getSchemeId();
            final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
            Logger.debug(this, "updating step for scheme with schemeId: " + schemeId);
            final WorkflowStep step = this.workflowHelper.addStep(newStepForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(step)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on addStep, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }


    /**
     * Retrieves a step given its id.
     * @param request HttpServletRequest
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getFindWorkflowStepById", summary = "Retrieves a workflow step",
            description = "Returns a [workflow step](https://www.dotcms.com/docs/latest/managing-workflows#Steps) by identifier.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Found workflow step successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowStepView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Workflow step not found.")
            }
    )
    public final Response findStepById(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @NotNull @PathParam("stepId") @Parameter(
                                               required = true,
                                               description = "Identifier of the step to retrieve.\n\n" +
                                                       "Example: `ee24a4cb-2d15-4c98-b1bd-6327126451f3` (Default system workflow Draft step)",
                                               schema = @Schema(type = "string")
                                       ) final String stepId) {
        this.webResource.init(null, request, response, true, null);
        Logger.debug(this, "finding step by id stepId: " + stepId);
        try {
            final WorkflowStep step = this.workflowHelper.findStepById(stepId);
            return Response.ok(new ResponseEntityView(step)).build();
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findStepById, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Wrapper function around fireActionByNameMultipart, allowing the `/actions/fire` method receiving
     * multipart-form data also to be called from `/actions/firemultipart`.
     * Swagger UI doesn't allow endpoint overloading, so this was created as an alias — both to
     * surface the endpoint and preserve backwards compatibility.
     * The wrapped function receives the @Hidden annotation, which explicitly omits it from the UI.
     * All other Swagger-specific annotations have been moved off of the original and on to this one.
     */
    @PUT
    @Path("/actions/firemultipart")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(operationId = "putFireActionByNameMultipart", summary = "Fire action by name (multipart form) \uD83D\uDEA7",
            description = "(**Construction notice:** Still needs request body documentation. Coming soon!)\n\n" +
                    "Fires a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions), " +
                    "specified by name, on a target contentlet. Uses a multipart form to transmit its data.\n\n" +
                    "Returns a map of the resultant contentlet, with an additional " +
                    "`AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
                    "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fired action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireActionByNameMultipartNewPath(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @QueryParam("inode") @Parameter(
                                                            description = "Inode of the target content.",
                                                            schema = @Schema(type = "string")
                                                    ) final String inode,
                                                    @QueryParam("identifier") @Parameter(
                                                            description = "Identifier of target content.",
                                                            schema = @Schema(type = "string")
                                                    ) final String identifier,
                                                    @QueryParam("indexPolicy") @Parameter(
                                                            description = "Determines how target content is indexed.\n\n" +
                                                                    "| Value | Description |\n" +
                                                                    "|-------|-------------|\n" +
                                                                    "| `DEFER` | Content will be indexed asynchronously, outside of " +
                                                                                "the current process. Valid content will finish the " +
                                                                                "method in process and be returned before the content " +
                                                                                "becomes visible in the index. This is the default " +
                                                                                "index policy; it is resource-friendly and well-" +
                                                                                "suited to batch processing. |\n" +
                                                                    "| `WAIT_FOR` | The API call will not return from the content check " +
                                                                                "process until the content has been indexed. Ensures content " +
                                                                                "is promptly available for searching. |\n" +
                                                                    "| `FORCE` | Forces Elasticsearch to index the content **immediately**.<br>" +
                                                                                "**Caution:** Using this value may cause system performance issues; " +
                                                                                "it is not recommended for general use, though may be useful " +
                                                                                "for testing purposes. |\n\n",
                                                            schema = @Schema(
                                                                    type = "string",
                                                                    allowableValues = {"DEFER", "WAIT_FOR", "FORCE"},
                                                                    defaultValue = ""
                                                            )
                                                    ) final String indexPolicy,
                                                    @DefaultValue("-1") @QueryParam("language") @Parameter(
                                                            description = "Language version of target content.",
                                                            schema = @Schema(type = "string")
                                                    ) final String language,
                                                    @RequestBody(
                                                            description = "Multipart form. More details to follow.",
                                                         required = true,
                                                         content = @Content(
                                                                 schema = @Schema(implementation = FormDataMultiPart.class)
                                                         )
                                                 ) final FormDataMultiPart multipart) {
        return fireActionByNameMultipart(request, response, inode, identifier, indexPolicy, language, multipart);
    }

    /**
     * Fires a workflow action by name and multi part, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param multipart {@link FormDataMultiPart} Multipart form (if an inode is set, this param is not ignored).
     *
     * @return Response
     */
    @PUT
    @Path("/actions/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public final Response fireActionByNameMultipart(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @QueryParam("inode")            final String inode,
                                              @QueryParam("identifier")       final String identifier,
                                              @QueryParam("indexPolicy")      final String indexPolicy,
                                              @DefaultValue("-1") @QueryParam("language")         final String   language,
                                              final FormDataMultiPart multipart) {

      final InitDataObject initDataObject = new WebResource.InitBuilder()
          .requestAndResponse(request, new MockHttpResponse())
          .requiredAnonAccess(AnonymousAccess.WRITE)
          .init();
        String actionId = null;

        try {

            Logger.debug(this, ()-> "On Fire Action: inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            final FireActionByNameForm fireActionForm = this.processForm (multipart, initDataObject.getUser());
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            actionId = this.workflowHelper.getActionIdOnList // we do not want to do a double permission check here, since the WF API will check the permissions for the action later on
                    (fireActionForm.getActionName(), contentlet, APILocator.systemUser());

            Logger.debug(this, "fire ActionByName Multipart with the actionid: " + actionId);
            return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Fires a workflow action by name, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String} (Optional) to fire an action over the existing language (in combination of identifier).
     * @param fireActionForm {@link FireActionByNameForm} Fire Action by Name Form (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putFireActionByName", summary = "Fire workflow action by name",
            description = "Fires a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions), " +
                            "specified by name, on a target contentlet.\n\nReturns a map of the resultant contentlet, " +
                            "with an additional `AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
                            "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fired action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireActionByNameSinglePart(@Context final HttpServletRequest request,
                                                     @QueryParam("inode") @Parameter(
                                                             description = "Inode of the target content.",
                                                             schema = @Schema(type = "string")
                                                     ) final String inode,
                                                     @QueryParam("identifier") @Parameter(
                                                             description = "Identifier of target content.",
                                                             schema = @Schema(type = "string")
                                                     ) final String identifier,
                                                     @QueryParam("indexPolicy") @Parameter(
                                                             description = "Determines how target content is indexed.\n\n" +
                                                                     "| Value | Description |\n" +
                                                                     "|-------|-------------|\n" +
                                                                     "| `DEFER` | Content will be indexed asynchronously, outside of " +
                                                                     "the current process. Valid content will finish the " +
                                                                     "method in process and be returned before the content " +
                                                                     "becomes visible in the index. This is the default " +
                                                                     "index policy; it is resource-friendly and well-" +
                                                                     "suited to batch processing. |\n" +
                                                                     "| `WAIT_FOR` | The API call will not return from the content check " +
                                                                     "process until the content has been indexed. Ensures content " +
                                                                     "is promptly available for searching. |\n" +
                                                                     "| `FORCE` | Forces Elasticsearch to index the content **immediately**.<br>" +
                                                                     "**Caution:** Using this value may cause system performance issues; " +
                                                                     "it is not recommended for general use, though may be useful " +
                                                                     "for testing purposes. |\n\n",
                                                             schema = @Schema(
                                                                     type = "string",
                                                                     allowableValues = {"DEFER", "WAIT_FOR", "FORCE"},
                                                                     defaultValue = ""
                                                             )
                                                     ) final String indexPolicy,
                                                     @DefaultValue("-1") @QueryParam("language") @Parameter(
                                                             description = "Language version of target content.",
                                                             schema = @Schema(type = "string")
                                                     ) final String language,
                                                     @RequestBody(
                                                             description = "Body consists of a JSON object containing at minimum the " +
                                                                     "`actionName` property, specifying a workflow action to fire.\n\n" +
                                                                     "The full list of properties that may be used with this form " +
                                                                     "is as follows:\n\n" +
                                                                     "| Property | Type | Description |\n" +
                                                                     "|-|-|-|\n" +
                                                                     "| `actionName` | String | The name of the workflow action to perform. |\n" +
                                                                     "| `contentlet` | Object | An alternate way of specifying the target contentlet. " +
                                                                                                "If no identifier or inode is included via parameter, " +
                                                                                                "either one could instead be included in the body as a " +
                                                                                                "property of this object. |\n" +
                                                                     "| `comments` | String | Comments that will appear in the [workflow tasks]" +
                                                                                                "(https://www.dotcms.com/docs/latest/workflow-tasks) " +
                                                                                                "tool with the execution of this workflow action. |\n" +
                                                                     "| `individualPermissions` | Object | Allows setting granular permissions associated " +
                                                                                                "with the target. The object properties are the [system names " +
                                                                                                "of permissions](https://www.dotcms.com/docs/latest/user-permissions#Permissions), " +
                                                                                                "such as READ, PUBLISH, EDIT, etc. Their respective values " +
                                                                                                "are a list of user or role identifiers that should be granted " +
                                                                                                "the permission in question. Example: `\"READ\": " +
                                                                                                "[\"9ad24203-ae6a-4e5e-aa10-a8c38fd11f17\",\"MyRole\"]` |\n" +
                                                                     "| `assign` | String | The identifier of a user or role to next receive the " +
                                                                                                "workflow task assignment. |\n" +
                                                                     "| `pathToMove` | String | If the workflow action includes the Move actionlet, " +
                                                                                                "this property will specify the target path. This path " +
                                                                                                "must include a host, such as `//default/testfolder`, " +
                                                                                                "`//demo.dotcms.com/application`, etc. |\n" +
                                                                     "| `query` | String | Not used in this method. |\n" +
                                                                     "| `whereToSend` | String | For the [push publishing](push-publishing) actionlet; " +
                                                                                                "sets the push-publishing environment to receive the " +
                                                                                                "target content. Must be specified as an environment " +
                                                                                                "identifier. [Learn how to find environment IDs here.]" +
                                                                                                "(https://www.dotcms.com/docs/latest/push-publishing-endpoints#EnvironmentIds) |\n" +
                                                                     "| `iWantTo` | String | For the push publishing actionlet; " +
                                                                                                "this can be set to one of three values: <ul style=\"line-height:2rem;\"><li>`publish` for " +
                                                                                                "push publish;</li><li>`expire` for remove;</li><li>`publishexpire` " +
                                                                                                "for push remove.</li></ul> These are further configurable with the " +
                                                                                                "properties below that specify publishing and expiration " +
                                                                                                "dates, times, etc. |\n" +
                                                                     "| `publishDate` | String | For the push publishing actionlet; " +
                                                                                                "specifies a date to push the content. Format: `yyyy-MM-dd`.  |\n" +
                                                                     "| `publishTime` | String | For the push publishing actionlet; " +
                                                                                                "specifies a time to push the content. Format: `hh-mm`. |\n" +
                                                                     "| `expireDate` | String | For the push publishing actionlet; " +
                                                                                                "specifies a date to remove the content. Format: `yyyy-MM-dd`.  |\n" +
                                                                     "| `expireTime` | String | For the push publishing actionlet; " +
                                                                                                "specifies a time to remove the content. Format: `hh-mm`.  |\n" +
                                                                     "| `neverExpire` | Boolean | For the push publishing actionlet; " +
                                                                                                "a value of `true` invalidates the expiration time/date. |\n" +
                                                                     "| `filterKey` | String | For the push publishing actionlet; " +
                                                                                                "specifies a [push publishing filter](https://www.dotcms.com/docs/latest" +
                                                                                                "/push-publishing-filters) key, should the workflow action " +
                                                                                                "call for such. To retrieve a full list of push publishing " +
                                                                                                "filters and their keys, use `GET /v1/pushpublish/filters`. |\n" +
                                                                     "| `timezoneId` | String | For the push publishing actionlet; " +
                                                                                                "specifies the time zone to which the indicated times belong. " +
                                                                                                "Uses the [tz database](https://www.iana.org/time-zones). " +
                                                                                                "For a list of values, see [the database directly]" +
                                                                                                "(https://data.iana.org/time-zones/tz-link.html) or refer to " +
                                                                                                "[the Wikipedia entry listing tz database time zones]" +
                                                                                                "(https://en.wikipedia.org/wiki/List_of_tz_database_time_zones). |\n\n",
                                                             required = true,
                                                             content = @Content(
                                                                     schema = @Schema(implementation = FireActionByNameForm.class)
                                                             )
                                                     ) final FireActionByNameForm fireActionForm) {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
          .requestAndResponse(request, new MockHttpResponse())
          .requiredAnonAccess(AnonymousAccess.WRITE)
          .init();
        String actionId = null;

        try {

            Logger.debug(this, ()-> "On Fire Action: action name = '" + (null != fireActionForm? fireActionForm.getActionName(): StringPool.BLANK)
                    + "', inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);
            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            actionId = this.workflowHelper.getActionIdOnList // we do not want to do a double permission check here, since the WF API will check the permissions for the action later on
                    (fireActionForm.getActionName(), contentlet, APILocator.systemUser());

            Logger.debug(this, "fire ActionByName with the actionid: " + actionId);

            DotPreconditions.notNull(actionId, Sneaky.sneaked(()-> LanguageUtil.get(initDataObject.getUser().getLocale(),
                    "Unable-to-execute-workflows-InvalidActionName", fireActionForm.getActionName())),
                    DotContentletValidationException.class);

            return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private Response fireAction(final HttpServletRequest request,
                                final FireActionForm fireActionForm,
                                final User user,
                                final Contentlet contentlet,
                                final String actionId,
                                final Optional<SystemActionApiFireCommand> fireCommandOpt) throws DotDataException, DotSecurityException {

        Logger.debug(this, ()-> "Firing workflow action: " + actionId);

        if(null == contentlet || contentlet.getMap().isEmpty()) {

            Logger.debug(this, ()-> "On Fire Action: content is null or empty");
            throw new DoesNotExistException("contentlet-was-not-found");
        }

        final PageMode pageMode = PageMode.get(request);
        final IndexPolicy indexPolicy = contentlet.getIndexPolicy()!=null
                ? contentlet.getIndexPolicy()
                : IndexPolicyProvider.getInstance().forSingleContent();

        final ContentletDependencies.Builder formBuilder = new ContentletDependencies.Builder();
        formBuilder.respectAnonymousPermissions(pageMode.respectAnonPerms).
                workflowActionId(actionId).modUser(user)
                .indexPolicy(indexPolicy);

        if(fireActionForm != null) {

            formBuilder.workflowActionComments(fireActionForm.getComments())
                    .workflowAssignKey(fireActionForm.getAssign())
                    .workflowPublishDate(fireActionForm.getPublishDate())
                    .workflowPublishTime(fireActionForm.getPublishTime())
                    .workflowTimezoneId(fireActionForm.getTimezoneId())
                    .workflowExpireDate(fireActionForm.getExpireDate())
                    .workflowExpireTime(fireActionForm.getExpireTime())
                    .workflowNeverExpire(fireActionForm.getNeverExpire())
                    .workflowFilterKey(fireActionForm.getFilterKey())
                    .workflowWhereToSend(fireActionForm.getWhereToSend())
                    .workflowIWantTo(fireActionForm.getIWantTo())
                    .workflowPathToMove(fireActionForm.getPathToMove());

            this.processPermissions(fireActionForm, formBuilder);
        }

        if (contentlet.getMap().containsKey(Contentlet.RELATIONSHIP_KEY)) {
            formBuilder.relationships((ContentletRelationships) contentlet.getMap().get(Contentlet.RELATIONSHIP_KEY));
        }

        final Optional<List<Category>>categories = MapToContentletPopulator.
                INSTANCE.fetchCategories(contentlet, user, pageMode.respectAnonPerms);

        //Empty collection implies removal, so only when a value is present we must pass the collection
        categories.ifPresent(formBuilder::categories);
        final Contentlet basicContentlet = fireCommandOpt.isPresent()?
                fireCommandOpt.get().fire(contentlet, this.needSave(fireActionForm), formBuilder.build()):
                this.workflowAPI.fireContentWorkflow(contentlet, formBuilder.build());
        final Contentlet hydratedContentlet = Objects.nonNull(basicContentlet)?
                new DotTransformerBuilder().contentResourceOptions(false)
                    .content(basicContentlet).build().hydrate().get(0): basicContentlet;

        if (Objects.nonNull(basicContentlet) && Objects.nonNull(basicContentlet.getVariantId())) {
            hydratedContentlet.setVariantId(basicContentlet.getVariantId());
        }
        return Response.ok(
                new ResponseEntityView<>(this.workflowHelper.contentletToMap(hydratedContentlet))
        ).build(); // 200
    }

    private void processPermissions(final FireActionForm fireActionForm,
                                    final ContentletDependencies.Builder formBuilder) {

        if (null != fireActionForm.getIndividualPermissions()) {

            final List<Permission> permissions = new ArrayList<>();
            for(final Map.Entry<PermissionAPI.Type, List<String>> entry :
                    fireActionForm.getIndividualPermissions().entrySet()) {

                entry.getValue().forEach(roleId -> permissions.add(
                        new Permission(null, this.mapRoleId(roleId), entry.getKey().getType())));
            }

            formBuilder.permissions(permissions);
        }
    }

    protected String mapRoleId (final String roleIdOrKey) {

        final Role role = Try.of(()-> APILocator.getRoleAPI().loadRoleByKey(roleIdOrKey)).getOrNull();
        return null != role? role.getId(): roleIdOrKey;
    }

    private boolean needSave (final FireActionForm fireActionForm) {

        return null != fireActionForm && UtilMethods.isSet(fireActionForm.getContentletFormData());
    }

    /**
     * Fires a workflow with default action, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String} (Optional) to fire an action over the existing language (in combination of identifier).
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * (if an inode is set, this param is not ignored).
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * @return Response
     */
    @PUT
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "putFireDefaultSystemAction", summary = "Fire system action by name",
            description = "Fire a [default system action](https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) " +
                    "by name on a target contentlet.\n\nReturns a map of the resultant contentlet, " +
                    "with an additional `AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
                    "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fired action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireActionDefaultSinglePart(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @QueryParam("inode") @Parameter(
                                              description = "Inode of the target content.",
                                              schema = @Schema(type = "string")
                                      ) final String inode,
                                      @QueryParam("identifier") @Parameter(
                                              description = "Identifier of target content.",
                                              schema = @Schema(type = "string")
                                      ) final String identifier,
                                      @QueryParam("indexPolicy") @Parameter(
                                              description = "Determines how target content is indexed.\n\n" +
                                                      "| Value | Description |\n" +
                                                      "|-------|-------------|\n" +
                                                      "| `DEFER` | Content will be indexed asynchronously, outside of " +
                                                                    "the current process. Valid content will finish the " +
                                                                    "method in process and be returned before the content " +
                                                                    "becomes visible in the index. This is the default " +
                                                                    "index policy; it is resource-friendly and well-" +
                                                                    "suited to batch processing. |\n" +
                                                      "| `WAIT_FOR` | The API call will not return from the content check " +
                                                                    "process until the content has been indexed. Ensures content " +
                                                                    "is promptly available for searching. |\n" +
                                                      "| `FORCE` | Forces Elasticsearch to index the content **immediately**.<br>" +
                                                                    "**Caution:** Using this value may cause system performance issues; " +
                                                                    "it is not recommended for general use, though may be useful " +
                                                                    "for testing purposes. |\n\n",
                                              schema = @Schema(
                                                      type = "string",
                                                      allowableValues = {"DEFER", "WAIT_FOR", "FORCE"},
                                                      defaultValue = ""
                                              )
                                      ) final String indexPolicy,
                                      @DefaultValue("-1") @QueryParam("language") @Parameter(
                                              description = "Language version of target content.",
                                              schema = @Schema(type = "string")
                                      ) final String language,
                                      @DefaultValue("DEFAULT") @QueryParam("variantName") @Parameter(
                                              description = "Variant name",
                                              schema = @Schema(type = "string")
                                      ) final String variantName,
                                      @PathParam("systemAction") @Parameter(
                                              required = true,
                                              schema = @Schema(
                                                      type = "string",
                                                      allowableValues = {
                                                              "NEW", "EDIT", "PUBLISH",
                                                              "UNPUBLISH", "ARCHIVE", "UNARCHIVE",
                                                              "DELETE", "DESTROY"
                                                      }
                                              ),
                                              description = "Default system action."
                                      ) final WorkflowAPI.SystemAction systemAction,
                                      @RequestBody(
                                              description = "Optional body consists of a JSON object containing a FireActionByNameForm " +
                                                      "object — a form that appears in similar functions, as well, but implemented with " +
                                                      "minor differences across methods. As such, some properties are unused.\n\n" +
                                                      "The full list of properties that may be used with this form is as follows:\n\n" +
                                                      "| Property | Type | Description |\n" +
                                                      "|-|-|-|\n" +
                                                      "| `actionName` | String | Not used in this method. |\n" +
                                                      "| `contentlet` | Object | An alternate way of specifying the target contentlet. " +
                                                                                "If no identifier or inode is included via parameter, " +
                                                                                "either one could instead be included in the body as a " +
                                                                                "property of this object. |\n" +
                                                      "| `comments` | String | Comments that will appear in the [workflow tasks]" +
                                                                                "(https://www.dotcms.com/docs/latest/workflow-tasks) " +
                                                                                "tool with the execution of this workflow action. |\n" +
                                                      "| `individualPermissions` | Object | Allows setting granular permissions associated " +
                                                                                "with the target. The object properties are the [system names " +
                                                                                "of permissions](https://www.dotcms.com/docs/latest/user-permissions#Permissions), " +
                                                                                "such as READ, PUBLISH, EDIT, etc. Their respective values " +
                                                                                "are a list of user or role identifiers that should be granted " +
                                                                                "the permission in question. Example: `\"READ\": " +
                                                                                "[\"9ad24203-ae6a-4e5e-aa10-a8c38fd11f17\",\"MyRole\"]` |\n" +
                                                      "| `assign` | String | The identifier of a user or role to next receive the " +
                                                                                "workflow task assignment. |\n" +
                                                      "| `pathToMove` | String | If the workflow action includes the Move actionlet, " +
                                                                                "this property will specify the target path. This path " +
                                                                                "must include a host, such as `//default/testfolder`, " +
                                                                                "`//demo.dotcms.com/application`, etc. |\n" +
                                                      "| `query` | String | Not used in this method. |\n" +
                                                      "| `whereToSend` | String | For the [push publishing](push-publishing) actionlet; " +
                                                                                "sets the push-publishing environment to receive the " +
                                                                                "target content. Must be specified as an environment " +
                                                                                "identifier. [Learn how to find environment IDs here.]" +
                                                                                "(https://www.dotcms.com/docs/latest/push-publishing-endpoints#EnvironmentIds) |\n" +
                                                      "| `iWantTo` | String | For the push publishing actionlet; " +
                                                                                "this can be set to one of three values: <ul style=\"line-height:2rem;\"><li>`publish` for " +
                                                                                "push publish;</li><li>`expire` for remove;</li><li>`publishexpire` " +
                                                                                "for push remove.</li></ul> These are further configurable with the " +
                                                                                "properties below that specify publishing and expiration " +
                                                                                "dates, times, etc. |\n" +
                                                      "| `publishDate` | String | For the push publishing actionlet; " +
                                                                                "specifies a date to push the content. Format: `yyyy-MM-dd`.  |\n" +
                                                      "| `publishTime` | String | For the push publishing actionlet; " +
                                                                                "specifies a time to push the content. Format: `hh-mm`. |\n" +
                                                      "| `expireDate` | String | For the push publishing actionlet; " +
                                                                                "specifies a date to remove the content. Format: `yyyy-MM-dd`.  |\n" +
                                                      "| `expireTime` | String | For the push publishing actionlet; " +
                                                                                "specifies a time to remove the content. Format: `hh-mm`.  |\n" +
                                                      "| `neverExpire` | Boolean | For the push publishing actionlet; " +
                                                                                "a value of `true` invalidates the expiration time/date. |\n" +
                                                      "| `filterKey` | String | For the push publishing actionlet; " +
                                                                                "specifies a [push publishing filter](https://www.dotcms.com/docs/latest" +
                                                                                "/push-publishing-filters) key, should the workflow action " +
                                                                                "call for such. To retrieve a full list of push publishing " +
                                                                                "filters and their keys, use `GET /v1/pushpublish/filters`. |\n" +
                                                      "| `timezoneId` | String | For the push publishing actionlet; " +
                                                                                "specifies the time zone to which the indicated times belong. " +
                                                                                "Uses the [tz database](https://www.iana.org/time-zones). " +
                                                                                "For a list of values, see [the database directly]" +
                                                                                "(https://data.iana.org/time-zones/tz-link.html) or refer to " +
                                                                                "[the Wikipedia entry listing tz database time zones]" +
                                                                                "(https://en.wikipedia.org/wiki/List_of_tz_database_time_zones). |\n\n",
                                              content = @Content(
                                                      schema = @Schema(implementation = FireActionByNameForm.class)
                                              )
                                      ) final FireActionForm fireActionForm) {

          final InitDataObject initDataObject = new WebResource.InitBuilder()
          .requestAndResponse(request, response)
          .requiredAnonAccess(AnonymousAccess.WRITE)
          .init();
      
        try {

            Logger.debug(this, ()-> "On Fire Action: systemAction = " + systemAction + ", inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final PageMode mode   = PageMode.get(request);
            final long languageId = LanguageUtil.getLanguageId(language);
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode, variantName);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            this.checkContentletState (contentlet, systemAction);

            final Optional<WorkflowAction> workflowActionOpt = // ask to see if there is any default action by content type or scheme
                    this.workflowAPI.findActionMappedBySystemActionContentlet
                            (contentlet, systemAction, initDataObject.getUser());

            if (workflowActionOpt.isPresent()) {

                final WorkflowAction workflowAction = workflowActionOpt.get();
                final String actionId = workflowAction.getId();

                Logger.info(this, "Using the default action: " + workflowAction +
                        ", for the system action: " + systemAction);

                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(workflowAction,
                                this.needSave(fireActionForm), systemAction);

                return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, fireCommandOpt);
            } else {

                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(systemAction);

                if (fireCommandOpt.isPresent()) {

                    return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, null, fireCommandOpt);
                }

                final ContentType contentType = contentlet.getContentType();
                throw new DoesNotExistException("For the contentType: " + (null != contentType?contentType.variable():"unknown") +
                        " systemAction = " + systemAction);
            }
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, systemAction: " + systemAction +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    } // fireAction.

    /**
     * Fires a workflow with default action to perform over a collection of contentlets (Post)
     *
     *
     * The result of the execution is a streaming of the json, it will return a set of maps
     *
     * @param request    {@link HttpServletRequest}
     * @param response   {@link HttpServletResponse}
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * @return Response
     */
    @POST
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    //@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/octet-stream")
    @Operation(operationId = "postFireSystemActionByNameMulti", summary = "Fire system action by name over multiple contentlets",
            description = "Fire a [default system action](https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) " +
                    "by name on multiple target contentlets.\n\nReturns a list of resultant contentlet maps, each with an additional  " +
            "`AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
            "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fired action successfully",
                            content = @Content(mediaType = "application/octet-stream",
                                    schema = @Schema(implementation = ResponseEntityView.class),
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": {\n" +
                                            "    \"results\": [\n" +
                                            "      {\n" +
                                            "        \"c2701eced2b59f0bbd55b3d9667878ce\": {\n" +
                                            "          \"AUTO_ASSIGN_WORKFLOW\": false,\n" +
                                            "          \"archived\": false,\n" +
                                            "          \"baseType\": \"string\",\n" +
                                            "          \"body\": \"string\",\n" +
                                            "          \"body_raw\": \"string\",\n" +
                                            "          \"contentType\": \"string\",\n" +
                                            "          \"creationDate\": 1725051866540,\n" +
                                            "          \"folder\": \"string\",\n" +
                                            "          \"hasLiveVersion\": false,\n" +
                                            "          \"hasTitleImage\": false,\n" +
                                            "          \"host\": \"string\",\n" +
                                            "          \"hostName\": \"string\",\n" +
                                            "          \"identifier\": \"c2701eced2b59f0bbd55b3d9667878ce\",\n" +
                                            "          \"inode\": \"string\",\n" +
                                            "          \"languageId\": 1,\n" +
                                            "          \"live\": false,\n" +
                                            "          \"locked\": false,\n" +
                                            "          \"modDate\": 1727438483022,\n" +
                                            "          \"modUser\": \"string\",\n" +
                                            "          \"modUserName\": \"string\",\n" +
                                            "          \"owner\": \"string\",\n" +
                                            "          \"ownerName\": \"string\",\n" +
                                            "          \"publishDate\": 1727438483051,\n" +
                                            "          \"publishUser\": \"string\",\n" +
                                            "          \"publishUserName\": \"string\",\n" +
                                            "          \"sortOrder\": 0,\n" +
                                            "          \"stInode\": \"string\",\n" +
                                            "          \"title\": \"string\",\n" +
                                            "          \"titleImage\": \"string\",\n" +
                                            "          \"url\": \"string\",\n" +
                                            "          \"working\": false\n" +
                                            "        }\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"summary\": {\n" +
                                            "      \"affected\": 1,\n" +
                                            "      \"failCount\": 0,\n" +
                                            "      \"successCount\": 1,\n" +
                                            "      \"time\": 45\n" +
                                            "    }\n" +
                                            "  },\n" +
                                            "  \"errors\": [],\n" +
                                            "  \"i18nMessagesMap\": {},\n" +
                                            "  \"messages\": [],\n" +
                                            "  \"permissions\": []\n" +
                                            "}")
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireMultipleActionDefault(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam("systemAction") @Parameter(
                                                            required = true,
                                                            schema = @Schema(
                                                                    type = "string",
                                                                    allowableValues = {
                                                                            "NEW", "EDIT", "PUBLISH",
                                                                            "UNPUBLISH", "ARCHIVE", "UNARCHIVE",
                                                                            "DELETE", "DESTROY"
                                                                    }
                                                            ),
                                                            description = "Default system action."
                                                    ) final WorkflowAPI.SystemAction systemAction,
                                                    @RequestBody(
                                                            description = "Optional body consists of a JSON object containing various properties, " +
                                                                    "some of which are specific to certain actionlets.\n\n" +
                                                                    "The full list of properties that may be used with this form is as follows:\n\n" +
                                                                    "| Property | Type | Description |\n" +
                                                                    "|-|-|-|\n" +
                                                                    "| `contentlet` | List of Objects | Multiple contentlet objects to serve " +
                                                                                                "as the target of the selected default system action; requires, at minimum, " +
                                                                                                "an identifier in each. |\n" +
                                                                    "| `comments` | String | Comments that will appear in the [workflow tasks]" +
                                                                                                "(https://www.dotcms.com/docs/latest/workflow-tasks) " +
                                                                                                "tool with the execution of this workflow action. |\n" +
                                                                    "| `assign` | String | The identifier of a user or role to next receive the " +
                                                                                                "workflow task assignment. |\n" +
                                                                    "| `whereToSend` | String | For the [push publishing](push-publishing) actionlet; " +
                                                                                                "sets the push-publishing environment to receive the " +
                                                                                                "target content. Must be specified as an environment " +
                                                                                                "identifier. [Learn how to find environment IDs here.]" +
                                                                                                "(https://www.dotcms.com/docs/latest/push-publishing-endpoints#EnvironmentIds) |\n" +
                                                                    "| `iWantTo` | String | For the push publishing actionlet; " +
                                                                                                "this can be set to one of three values: <ul style=\"line-height:2rem;\"><li>`publish` for " +
                                                                                                "push publish;</li><li>`expire` for remove;</li><li>`publishexpire` " +
                                                                                                "for push remove.</li></ul> These are further configurable with the " +
                                                                                                "properties below that specify publishing and expiration " +
                                                                                                "dates, times, etc. |\n" +
                                                                    "| `publishDate` | String | For the push publishing actionlet; " +
                                                                                                "specifies a date to push the content. Format: `yyyy-MM-dd`.  |\n" +
                                                                    "| `publishTime` | String | For the push publishing actionlet; " +
                                                                                                "specifies a time to push the content. Format: `hh-mm`. |\n" +
                                                                    "| `expireDate` | String | For the push publishing actionlet; " +
                                                                                                "specifies a date to remove the content. Format: `yyyy-MM-dd`.  |\n" +
                                                                    "| `expireTime` | String | For the push publishing actionlet; " +
                                                                                                "specifies a time to remove the content. Format: `hh-mm`.  |\n" +
                                                                    "| `neverExpire` | Boolean | For the push publishing actionlet; " +
                                                                                                "a value of `true` invalidates the expiration time/date. |\n" +
                                                                    "| `filterKey` | String | For the push publishing actionlet; " +
                                                                                                "specifies a [push publishing filter](https://www.dotcms.com/docs/latest" +
                                                                                                "/push-publishing-filters) key, should the workflow action " +
                                                                                                "call for such. To retrieve a full list of push publishing " +
                                                                                                "filters and their keys, use `GET /v1/pushpublish/filters`. |\n" +
                                                                    "| `timezoneId` | String | For the push publishing actionlet; " +
                                                                                                "specifies the time zone to which the indicated times belong. " +
                                                                                                "Uses the [tz database](https://www.iana.org/time-zones). " +
                                                                                                "For a list of values, see [the database directly]" +
                                                                                                "(https://data.iana.org/time-zones/tz-link.html) or refer to " +
                                                                                                "[the Wikipedia entry listing tz database time zones]" +
                                                                                                "(https://en.wikipedia.org/wiki/List_of_tz_database_time_zones). |\n\n",
                                                            content = @Content(
                                                                    schema = @Schema(implementation = FireMultipleActionForm.class),
                                                                    examples = @ExampleObject(value = "{\n" +
                                                                            "  \"contentlet\": [\n" +
                                                                            "    {\n" +
                                                                            "      \"identifier\": \"d684c0a9abeeeceea8b9a7e32fc272ae\"\n" +
                                                                            "    },\n" +
                                                                            "    { \"identifier\": \"c2701eced2b59f0bbd55b3d9667878ce\" }\n" +
                                                                            "  ],\n" +
                                                                            "  \"comments\": \"test comment\"\n" +
                                                                            "}")
                                                            )
                                                    ) final FireMultipleActionForm fireActionForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response).requiredAnonAccess(AnonymousAccess.WRITE).init();

        Logger.debug(this, ()-> "On Fire Multiple Actions: systemAction = " + systemAction);

        final PageMode mode   = PageMode.get(request);

        return Response.ok(new MultipleContentletStreamingOutput(systemAction,
                fireActionForm, request, mode, initDataObject))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    } // fireMultipleActionDefault.

    private class MultipleContentletStreamingOutput implements StreamingOutput {

        private final List<Map<String, Object>> contentletsToSaveList;
        private final WorkflowAPI.SystemAction systemAction;
        private final FireMultipleActionForm fireActionForm;
        private final HttpServletRequest request;
        private final PageMode mode;
        private final InitDataObject initDataObject;

        private MultipleContentletStreamingOutput(final WorkflowAPI.SystemAction systemAction,
                                                  final FireMultipleActionForm fireActionForm,
                                                  final HttpServletRequest request,
                                                  final PageMode mode,
                                                  final InitDataObject initDataObject) {

            this.contentletsToSaveList = fireActionForm.getContentletsFormData();
            this.systemAction           = systemAction;
            this.fireActionForm = fireActionForm;
            this.request = request;
            this.mode = mode;
            this.initDataObject = initDataObject;
        }

        @Override
        public void write(final OutputStream output) throws IOException, WebApplicationException {

            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            WorkflowResource.this.saveMultipleContentletsByDefaultAction(this.contentletsToSaveList, this.systemAction, this.fireActionForm,
                    this.request, this.mode, this.initDataObject, output, objectMapper);
        }
    }

    /**
     * This method fires the workflow for multiple contentlets
     * @param contentletsToSaveList
     * @param systemAction
     * @param fireActionForm
     * @param request
     * @param mode
     * @param initDataObject
     * @param outputStream
     * @param objectMapper
     */
    private void saveMultipleContentletsByDefaultAction(final List<Map<String, Object>> contentletsToSaveList,
                                                 final WorkflowAPI.SystemAction systemAction,
                                                 final FireMultipleActionForm fireActionForm,
                                                 final HttpServletRequest request,
                                                 final PageMode mode,
                                                 final InitDataObject initDataObject,
                                                 final OutputStream outputStream,
                                                 final ObjectMapper objectMapper) {

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter(WORKFLOW_SUBMITTER,
                new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(2).maxPoolSize(5).queueCapacity(CONTENTLETS_LIMIT).build());
        final CompletionService<Map<String, Object>> completionService = new ExecutorCompletionService<>(dotSubmitter);
        final List<Future<Map<String, Object>>> futures = new ArrayList<>();
        final HttpServletRequest statelessRequest = RequestUtil.INSTANCE.createStatelessRequest(request);

        for (final Map<String, Object> contentMap : contentletsToSaveList) {

            // this triggers the save
            final Future<Map<String, Object>> future = completionService.submit(() -> {

                HttpServletRequestThreadLocal.INSTANCE.setRequest(statelessRequest);
                final Map<String, Object> resultMap = new HashMap<>();
                final String inode      = (String) contentMap.get("inode");
                final String identifier = (String) contentMap.get("identifier");
                final long languageId   = ConversionUtils.toLong(contentMap.get("languageId"), -1l);
                final User user         = initDataObject.getUser();
                final FireActionForm singleFireActionForm = new FireActionForm.Builder()
                    .contentlet(contentMap).assign(fireActionForm.getAssign())
                    .comments(fireActionForm.getComments()).expireDate(fireActionForm.getExpireDate())
                    .neverExpire(fireActionForm.getNeverExpire()).filterKey(fireActionForm.getFilterKey())
                    .iWantTo(fireActionForm.getIWantTo()).publishDate(fireActionForm.getPublishDate())
                    .publishTime(fireActionForm.getPublishTime()).timezoneId(fireActionForm.getTimezoneId()).whereToSend(fireActionForm.getWhereToSend()).build();
                final IndexPolicy indexPolicy = MapToContentletPopulator.recoverIndexPolicy(
                        singleFireActionForm.getContentletFormData(),
                        IndexPolicyProvider.getInstance().forSingleContent(), request);


                try {

                    fireTransactionalAction(systemAction, singleFireActionForm, request, mode,
                            initDataObject, resultMap, inode, identifier, languageId, user, indexPolicy);
                } catch (Exception e) {

                    final String id = UtilMethods.isSet(identifier) ? identifier :
                            (UtilMethods.isSet(inode) ? inode:
                                    null != singleFireActionForm && null != singleFireActionForm.getContentletFormData() ?
                                            singleFireActionForm.getContentletFormData().toString() :
                                            "unknown contentlet" + System.currentTimeMillis());
                    Logger.error(this, "Error in contentlet: " + id + ", msg: " + e.getMessage()
                            + ", running the action: " + systemAction, e);
                    resultMap.put(id, UtilMethods.isSet(identifier)?
                            ActionFail.newInstanceById(user, identifier, e):ActionFail.newInstance(user, inode, e));
                }

                return resultMap;
            });

            futures.add(future);
        }

        printResponseEntityViewResult(outputStream, objectMapper, completionService, futures);
    }

    ////////////////////////////

    /**
     * Fires a workflow with default action to perform a merge (Patch) between the fields sent in the body form and
     * the existing contentlets.
     *
     * Users may merge just one contentlet by using:
     * - identifier + lang (optional)
     * - inode
     * - a query
     *
     * The result of the execution is a streaming of the json, it will return a set of maps
     *
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String} (Optional) to fire an action over the existing language (in combination of identifier).
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * (if an inode is set, this param is not ignored).
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * @return Response
     */
    @PATCH
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    //@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Produces("application/octet-stream")
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "patchFireMergeSystemAction", summary = "Modify specific fields on multiple contentlets",
            description = "Assigns values to the specified fields across multiple [contentlets](https://www.dotcms.com" +
                    "/docs/latest/content#Contentlets) simultaneously.\n\n" +
                    "Can use a [Lucene query](https://www.dotcms.com/docs/latest/content-search-syntax#Lucene) in its " +
                    "body to select all resulting content items.\n\n" +
                    "Returns a list of resultant contentlet maps, each with an additional  " +
                    "`AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
                    "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contentlet(s) modified successfully",
                            content = @Content(mediaType = "application/octet-stream",
                                    schema = @Schema(implementation = ResponseEntityView.class),
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": {\n" +
                                            "    \"results\": [\n" +
                                            "      {\n" +
                                            "        \"c2701eced2b59f0bbd55b3d9667878ce\": {\n" +
                                            "          \"AUTO_ASSIGN_WORKFLOW\": false,\n" +
                                            "          \"archived\": false,\n" +
                                            "          \"baseType\": \"string\",\n" +
                                            "          \"body\": \"string\",\n" +
                                            "          \"body_raw\": \"string\",\n" +
                                            "          \"contentType\": \"string\",\n" +
                                            "          \"creationDate\": 1725051866540,\n" +
                                            "          \"folder\": \"string\",\n" +
                                            "          \"hasLiveVersion\": false,\n" +
                                            "          \"hasTitleImage\": false,\n" +
                                            "          \"host\": \"string\",\n" +
                                            "          \"hostName\": \"string\",\n" +
                                            "          \"identifier\": \"c2701eced2b59f0bbd55b3d9667878ce\",\n" +
                                            "          \"inode\": \"string\",\n" +
                                            "          \"languageId\": 1,\n" +
                                            "          \"live\": false,\n" +
                                            "          \"locked\": false,\n" +
                                            "          \"modDate\": 1727438483022,\n" +
                                            "          \"modUser\": \"string\",\n" +
                                            "          \"modUserName\": \"string\",\n" +
                                            "          \"owner\": \"string\",\n" +
                                            "          \"ownerName\": \"string\",\n" +
                                            "          \"publishDate\": 1727438483051,\n" +
                                            "          \"publishUser\": \"string\",\n" +
                                            "          \"publishUserName\": \"string\",\n" +
                                            "          \"sortOrder\": 0,\n" +
                                            "          \"stInode\": \"string\",\n" +
                                            "          \"title\": \"string\",\n" +
                                            "          \"titleImage\": \"string\",\n" +
                                            "          \"url\": \"string\",\n" +
                                            "          \"working\": false\n" +
                                            "        }\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"summary\": {\n" +
                                            "      \"affected\": 1,\n" +
                                            "      \"failCount\": 0,\n" +
                                            "      \"successCount\": 1,\n" +
                                            "      \"time\": 45\n" +
                                            "    }\n" +
                                            "  },\n" +
                                            "  \"errors\": [],\n" +
                                            "  \"i18nMessagesMap\": {},\n" +
                                            "  \"messages\": [],\n" +
                                            "  \"permissions\": []\n" +
                                            "}")
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Content Type not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireMergeActionDefault(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @QueryParam("inode") @Parameter(
                                                    description = "Inode of the target content.",
                                                    schema = @Schema(type = "string")
                                            ) final String inode,
                                            @QueryParam("identifier") @Parameter(
                                                    description = "Identifier of target content.",
                                                    schema = @Schema(type = "string")
                                            ) final String identifier,
                                            @DefaultValue("-1") @QueryParam("language") @Parameter(
                                                    description = "Language version of target content.",
                                                    schema = @Schema(type = "string")
                                            ) final String language,
                                            @QueryParam("offset") @Parameter(
                                                    description = "Numeric offset for query results; useful for paginating.",
                                                    schema = @Schema(type = "integer")
                                            ) final int offset,
                                            @PathParam("systemAction") @Parameter(
                                                    required = true,
                                                    schema = @Schema(
                                                            type = "string",
                                                            allowableValues = {
                                                                    "NEW", "EDIT", "PUBLISH",
                                                                    "UNPUBLISH", "ARCHIVE", "UNARCHIVE",
                                                                    "DELETE", "DESTROY"
                                                            }
                                                    ),
                                                    description = "Default system action."
                                            ) final WorkflowAPI.SystemAction systemAction,
                                                 @RequestBody(
                                                         description = "Optional body consists of a JSON object containing various properties, " +
                                                                 "some of which are specific to certain actionlets.\n\n" +
                                                                 "The full list of properties that may be used with this form is as follows:\n\n" +
                                                                 "| Property | Type | Description |\n" +
                                                                 "|-|-|-|\n" +
                                                                 "| `query` | String | A Lucene query that can target multiple contentlets for " +
                                                                                             "editing. Example: `+contentType:htmlpageasset` for all " +
                                                                                             "dotCMS pages. |\n" +
                                                                 "| `contentlet` | Object | An alternate way of specifying the target contentlet. " +
                                                                                             "If no identifier or inode is included via parameter, " +
                                                                                             "either one could instead be included in the body as a " +
                                                                                             "property of this object. |\n" +
                                                                 "| `comments` | String | Comments that will appear in the [workflow tasks]" +
                                                                                             "(https://www.dotcms.com/docs/latest/workflow-tasks) " +
                                                                                             "tool with the execution of this workflow action. |\n" +
                                                                 "| `individualPermissions` | Object | Allows setting granular permissions associated " +
                                                                                             "with the target. The object properties are the [system names " +
                                                                                             "of permissions](https://www.dotcms.com/docs/latest/user-permissions#Permissions), " +
                                                                                             "such as READ, PUBLISH, EDIT, etc. Their respective values " +
                                                                                             "are a list of user or role identifiers that should be granted " +
                                                                                             "the permission in question. Example: `\"READ\": " +
                                                                                             "[\"9ad24203-ae6a-4e5e-aa10-a8c38fd11f17\",\"MyRole\"]` |\n" +
                                                                 "| `assign` | String | The identifier of a user or role to next receive the " +
                                                                                             "workflow task assignment. |\n" +
                                                                 "| `pathToMove` | String | If the workflow action includes the Move actionlet, " +
                                                                                             "this property will specify the target path. This path " +
                                                                                             "must include a host, such as `//default/testfolder`, " +
                                                                                             "`//demo.dotcms.com/application`, etc. |\n" +
                                                                 "| `whereToSend` | String | For the [push publishing](push-publishing) actionlet; " +
                                                                                             "sets the push-publishing environment to receive the " +
                                                                                             "target content. Must be specified as an environment " +
                                                                                             "identifier. [Learn how to find environment IDs here.]" +
                                                                                             "(https://www.dotcms.com/docs/latest/push-publishing-endpoints#EnvironmentIds) |\n" +
                                                                 "| `iWantTo` | String | For the push publishing actionlet; " +
                                                                                             "this can be set to one of three values: <ul style=\"line-height:2rem;\"><li>`publish` for " +
                                                                                             "push publish;</li><li>`expire` for remove;</li><li>`publishexpire` " +
                                                                                             "for push remove.</li></ul> These are further configurable with the " +
                                                                                             "properties below that specify publishing and expiration " +
                                                                                             "dates, times, etc. |\n" +
                                                                 "| `publishDate` | String | For the push publishing actionlet; " +
                                                                                             "specifies a date to push the content. Format: `yyyy-MM-dd`.  |\n" +
                                                                 "| `publishTime` | String | For the push publishing actionlet; " +
                                                                                             "specifies a time to push the content. Format: `hh-mm`. |\n" +
                                                                 "| `expireDate` | String | For the push publishing actionlet; " +
                                                                                             "specifies a date to remove the content. Format: `yyyy-MM-dd`.  |\n" +
                                                                 "| `expireTime` | String | For the push publishing actionlet; " +
                                                                                             "specifies a time to remove the content. Format: `hh-mm`.  |\n" +
                                                                 "| `neverExpire` | Boolean | For the push publishing actionlet; " +
                                                                                             "a value of `true` invalidates the expiration time/date. |\n" +
                                                                 "| `filterKey` | String | For the push publishing actionlet; " +
                                                                                             "specifies a [push publishing filter](https://www.dotcms.com/docs/latest" +
                                                                                             "/push-publishing-filters) key, should the workflow action " +
                                                                                             "call for such. To retrieve a full list of push publishing " +
                                                                                             "filters and their keys, use `GET /v1/pushpublish/filters`. |\n" +
                                                                 "| `timezoneId` | String | For the push publishing actionlet; " +
                                                                                             "specifies the time zone to which the indicated times belong. " +
                                                                                             "Uses the [tz database](https://www.iana.org/time-zones). " +
                                                                                             "For a list of values, see [the database directly]" +
                                                                                             "(https://data.iana.org/time-zones/tz-link.html) or refer to " +
                                                                                             "[the Wikipedia entry listing tz database time zones]" +
                                                                                             "(https://en.wikipedia.org/wiki/List_of_tz_database_time_zones). |\n\n",
                                                         content = @Content(
                                                                 schema = @Schema(implementation = FireActionForm.class),
                                                                 examples = @ExampleObject(value = "{\n" +
                                                                         "    \"comments\":\"Publish an existing Generic content\",\n" +
                                                                         "    \"query\":\"+contentType:webPageContent AND title:testcontent\",\n" +
                                                                         "    \"contentlet\": {\n" +
                                                                         "        \"title\":\"TestContentNowWithCaps\"\n" +
                                                                         "    }\n" +
                                                                         "}")
                                                         )
                                                 ) final FireActionForm fireActionForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response).requiredAnonAccess(AnonymousAccess.WRITE).init();

        final String query = null != fireActionForm? fireActionForm.getQuery():StringPool.BLANK;
        Logger.debug(this, ()-> "On Fire Merge Action: systemAction = " + systemAction + ", inode = " + inode +
                ", identifier = " + identifier + ", language = " + language + ", query: " + query);

        final PageMode mode   = PageMode.get(request);
        final long languageId = LanguageUtil.getLanguageId(language);
        final List<SingleContentQuery> contentletsToMergeList = new ArrayList<>();
        if (UtilMethods.isNotSet(query)) { // it is single update

            contentletsToMergeList.add(new SingleContentQuery(identifier, inode, languageId,
                    MapToContentletPopulator.recoverIndexPolicy(fireActionForm.getContentletFormData(),
                            IndexPolicyProvider.getInstance().forSingleContent(), request)));
        } else {

            final List<ContentletSearch> contentletSearches  = this.contentletAPI.searchIndex(query, Config.getIntProperty("", CONTENTLETS_LIMIT),
                    offset, null,
                    initDataObject.getUser(), mode.respectAnonPerms);

            final IndexPolicy indexPolicy = MapToContentletPopulator.recoverIndexPolicy(
                    (Objects.isNull(fireActionForm) || Objects.isNull(fireActionForm.getContentletFormData()))?Map.of():fireActionForm.getContentletFormData(),
                    contentletSearches.size()> 10? IndexPolicy.DEFER: IndexPolicy.WAIT_FOR, request);

            for (final ContentletSearch contentletSearch : contentletSearches) {

                contentletsToMergeList.add(new SingleContentQuery(contentletSearch.getIdentifier(),
                        contentletSearch.getInode(), languageId, indexPolicy));
            }
        }

        return Response.ok(new MergeContentletStreamingOutput(contentletsToMergeList, systemAction,
                fireActionForm, request, mode, initDataObject))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    } // fireMergeActionDefault.


    private class MergeContentletStreamingOutput implements StreamingOutput {

        private final List<SingleContentQuery> contentletsToMergeList;
        private final WorkflowAPI.SystemAction systemAction;
        private final FireActionForm fireActionForm;
        private final HttpServletRequest request;
        private final PageMode mode;
        private final InitDataObject initDataObject;

        private MergeContentletStreamingOutput(final List<SingleContentQuery> contentletsToMergeList,
                                               final WorkflowAPI.SystemAction systemAction,
                                               final FireActionForm fireActionForm,
                                               final HttpServletRequest request,
                                               final PageMode mode,
                                               final InitDataObject initDataObject) {

            this.contentletsToMergeList = contentletsToMergeList;
            this.systemAction           = systemAction;
            this.fireActionForm = fireActionForm;
            this.request = request;
            this.mode = mode;
            this.initDataObject = initDataObject;
        }

        @Override
        public void write(final OutputStream output) throws IOException, WebApplicationException {

            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            WorkflowResource.this.mergeContentletsByDefaultAction(this.contentletsToMergeList, this.systemAction, this.fireActionForm,
                    this.request, this.mode, this.initDataObject, output, objectMapper);
        }
    }

    private void mergeContentletsByDefaultAction(final List<SingleContentQuery> contentletsToMergeList,
                                                     final WorkflowAPI.SystemAction systemAction,
                                                     final FireActionForm fireActionForm,
                                                     final HttpServletRequest request,
                                                     final PageMode mode,
                                                     final InitDataObject initDataObject,
                                                     final OutputStream outputStream,
                                                     final ObjectMapper objectMapper) {

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter("workflow_submitter",
                new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(2).maxPoolSize(5).queueCapacity(CONTENTLETS_LIMIT).build());
        final CompletionService<Map<String, Object>> completionService = new ExecutorCompletionService<>(dotSubmitter);
        final List<Future<Map<String, Object>>> futures = new ArrayList<>();
        final HttpServletRequest statelessRequest = RequestUtil.INSTANCE.createStatelessRequest(request);


        for (final SingleContentQuery singleContentQuery : contentletsToMergeList) {

            // this triggers the merges
            final Future<Map<String, Object>> future = completionService.submit(() -> {

                HttpServletRequestThreadLocal.INSTANCE.setRequest(statelessRequest);
                final Map<String, Object> resultMap = new HashMap<>();
                final String inode      = singleContentQuery.getInode();
                final String identifier = singleContentQuery.getIdentifier();
                final long languageId   = singleContentQuery.getLanguage();
                final User user         = initDataObject.getUser();
                final IndexPolicy indexPolicy = singleContentQuery.getIndexPolicy();

                try {

                    fireTransactionalAction(systemAction, fireActionForm, statelessRequest, mode,
                            initDataObject, resultMap, inode, identifier, languageId, user, indexPolicy);
                } catch (Exception e) {

                    final String id = UtilMethods.isSet(identifier)?identifier:inode;
                    Logger.error(this, "Error in contentlet: " + id + ", msg: " + e.getMessage(), e);
                    resultMap.put(id, UtilMethods.isSet(identifier)?
                            ActionFail.newInstanceById(user, identifier, e):ActionFail.newInstance(user, inode, e));
                }

                return resultMap;
            });

            futures.add(future);
        }

        printResponseEntityViewResult(outputStream, objectMapper, completionService, futures);
    }

    @WrapInTransaction
    private void fireTransactionalAction(final SystemAction systemAction,
                                         final FireActionForm fireActionForm,
                                         final HttpServletRequest request,
                                         final PageMode mode, InitDataObject initDataObject,
                                         final Map<String, Object> resultMap,
                                         final String inode, String identifier,
                                         final long languageId,
                                         final User user,
                                         final IndexPolicy indexPolicy) throws DotDataException, DotSecurityException {

        final Contentlet contentlet = this.getContentlet // this do the merge
                (inode, identifier, languageId,
                        () -> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                        fireActionForm, initDataObject, mode);

        contentlet.setIndexPolicy(indexPolicy);
        this.checkContentletState(contentlet, systemAction);

        String contentletId = null != identifier? identifier: contentlet.getIdentifier();

        final Optional<WorkflowAction> workflowActionOpt = this.workflowAPI.findActionMappedBySystemActionContentlet
                (contentlet, systemAction, user);

        final Response restResponse = this.mergeContentlet(systemAction, fireActionForm, request, user, contentlet, workflowActionOpt);
        final  Map<String, Object> contentletMap = (Map<String, Object>)ResponseEntityView.class.cast(restResponse.getEntity()).getEntity();
        contentletId = !UtilMethods.isSet(contentletId)? (String)contentletMap.get("identifier"):contentletId;
        resultMap.put(contentletId, contentletMap);
    }

    private void printResponseEntityViewResult(final OutputStream outputStream,
                                               final ObjectMapper objectMapper,
                                               final CompletionService<Map<String, Object>> completionService,
                                               final List<Future<Map<String, Object>>> futures) {

        try {

            ResponseUtil.beginWrapResponseEntityView(outputStream, true);
            ResponseUtil.beginWrapProperty(outputStream, "results", false);
            outputStream.write(StringPool.OPEN_BRACKET.getBytes(StandardCharsets.UTF_8));
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int successCount = 0;
            int failCount    = 0;
            // now recover the N results
            for (int i = 0; i < futures.size(); i++) {

                try {

                    Logger.info(this, "Recovering the result " + (i + 1) + " of " + futures.size());
                    final Map<String, Object> resultMap = completionService.take().get();
                    objectMapper.writeValue(outputStream, resultMap);

                    if (isFail(resultMap)) {
                        failCount++;
                    } else {
                        successCount++;
                    }

                    if (i < futures.size()-1) {
                        outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (InterruptedException | ExecutionException | IOException e) {

                    Logger.error(this, e.getMessage(), e);
                }
            }
            stopWatch.stop();

            outputStream.write(StringPool.CLOSE_BRACKET.getBytes(StandardCharsets.UTF_8));
            outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));

            ResponseUtil.wrapProperty(outputStream, "summary",
                    objectMapper.writeValueAsString(Map.of("time", stopWatch.getTime(),
                            "affected", futures.size(),
                            "successCount", successCount,
                            "failCount", failCount)));
            outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));

            ResponseUtil.endWrapResponseEntityView(outputStream, true);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    private boolean isFail(final Map<String, Object> resultMap) {

        final String id = resultMap.keySet().stream().findFirst().get();
        return resultMap.get(id) instanceof ActionFail;
    }

    private Response mergeContentlet(SystemAction systemAction, FireActionForm fireActionForm, HttpServletRequest request, User user,
                                                Contentlet contentlet, Optional<WorkflowAction> workflowActionOpt) throws DotDataException, DotSecurityException {
        if (workflowActionOpt.isPresent()) {

            final WorkflowAction workflowAction = workflowActionOpt.get();
            final String actionId = workflowAction.getId();

            Logger.info(this, "Using the default action: " + workflowAction +
                    ", for the system action: " + systemAction);

            final Optional<SystemActionApiFireCommand> fireCommandOpt =
                    this.systemActionApiFireCommandProvider.get(workflowAction,
                            this.needSave(fireActionForm), systemAction);

            return this.fireAction(request, fireActionForm, user, contentlet, actionId, fireCommandOpt);
        } else {

            final Optional<SystemActionApiFireCommand> fireCommandOpt =
                    this.systemActionApiFireCommandProvider.get(systemAction);

            if (fireCommandOpt.isPresent()) {

                return this.fireAction(request, fireActionForm, user, contentlet, null, fireCommandOpt);
            }

            final ContentType contentType = contentlet.getContentType();
            throw new DoesNotExistException("For the contentType: " + (null != contentType ? contentType.variable() : "unknown") +
                    " systemAction = " + systemAction);
        }
    }


    /**
     * Check preconditions.
     * If contentlet can not be found, 404
     * if contentlet is not can not be a default action: UNPUBLISH, UNARCHIVE, DELETE, DESTROY
     * @param contentlet
     * @param systemAction
     * @throws NotFoundInDbException
     */
    private void checkContentletState(final Contentlet contentlet, final SystemAction systemAction)
            throws NotFoundInDbException {

        if (null == contentlet) {

            throw new NotFoundInDbException("Not Contentlet Found");
        }

        if (contentlet.isNew()) {

            if (    systemAction == SystemAction.UNPUBLISH ||
                    systemAction == SystemAction.UNARCHIVE ||
                    systemAction == SystemAction.DELETE    ||
                    systemAction == SystemAction.DESTROY) {

                throw new IllegalArgumentException("A new Contentlet can not fire any of these actions: [EDIT, UNPUBLISH, UNARCHIVE, DELETE, DESTROY]");
            }
        }
    }

    /**
     * Wrapper function around fireActionMultipart, allowing the `/actions/{actionId}/fire` method receiving
     * multipart-form data also to be called from `/actions/{actionId}/firemultipart`.
     * Swagger UI doesn't allow endpoint overloading, so this was created as an alias — both to
     * surface the endpoint and preserve backwards compatibility.
     * The wrapped function receives the @Hidden annotation, which explicitly omits it from the UI.
     * All other Swagger-specific annotations have been moved off of the original and on to this one.
     */

    @PUT
    @Path("/actions/{actionId}/firemultipart")
    @JSONP
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "putFireActionByIdMultipart", summary = "Fire action by ID (multipart form) \uD83D\uDEA7",
            description = "(**Construction notice:** Still needs request body documentation. Coming soon!)\n\n" +
                    "Fires a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions), " +
                    "specified by identifier, on a target contentlet. Uses a multipart form to transmit its data.\n\n" +
                    "Returns a map of the resultant contentlet, with an additional " +
                    "`AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
                    "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fired action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireActionMultipartNewPath(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam ("actionId") @Parameter(
                    required = true,
                    description = "Identifier of a workflow action.\n\n" +
                            "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                            "(Default system workflow \"Publish\" action)",
                    schema = @Schema(type = "string")
            ) final String actionId,
            @QueryParam("inode") @Parameter(
                    description = "Inode of the target content.",
                    schema = @Schema(type = "string")
            ) final String inode,
            @QueryParam("identifier") @Parameter(
                    description = "Identifier of target content.",
                    schema = @Schema(type = "string")
            ) final String identifier,
            @QueryParam("indexPolicy") @Parameter(
                    description = "Determines how target content is indexed.\n\n" +
                            "| Value | Description |\n" +
                            "|-------|-------------|\n" +
                            "| `DEFER` | Content will be indexed asynchronously, outside of " +
                            "the current process. Valid content will finish the " +
                            "method in process and be returned before the content " +
                            "becomes visible in the index. This is the default " +
                            "index policy; it is resource-friendly and well-" +
                            "suited to batch processing. |\n" +
                            "| `WAIT_FOR` | The API call will not return from the content check " +
                            "process until the content has been indexed. Ensures content " +
                            "is promptly available for searching. |\n" +
                            "| `FORCE` | Forces Elasticsearch to index the content **immediately**.<br>" +
                            "**Caution:** Using this value may cause system performance issues; " +
                            "it is not recommended for general use, though may be useful " +
                            "for testing purposes. |\n\n",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"DEFER", "WAIT_FOR", "FORCE"},
                            defaultValue = ""
                    )
            ) final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") @Parameter(
                    description = "Language version of target content.",
                    schema = @Schema(type = "string")
            ) final String language,
            @RequestBody(
                    description = "Multipart form. More details to follow.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FormDataMultiPart.class)
                    )
            ) final FormDataMultiPart multipart) {
        return fireActionMultipart(request, response, actionId, inode, identifier, indexPolicy,
                language, multipart);
    }

    /**
     * Fires a workflow action with multi part body
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param actionId   {@link String} (Required) action id to fire
     * (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/{actionId}/fire")
    @JSONP
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Hidden
    public final Response fireActionMultipart(@Context               final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam ("actionId")         final String actionId,
            @QueryParam("inode")            final String inode,
            @QueryParam("identifier")       final String identifier,
            @QueryParam("indexPolicy")      final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") final String   language,
            final FormDataMultiPart multipart) {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .init();

        try {

            Logger.debug(this, ()-> "On Fire Action Multipart: action Id " + actionId + ", inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            final FireActionForm fireActionForm = this.processForm (multipart, initDataObject.getUser());
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    } // fire.

    /**
     * Wrapper function around fireActionDefaultMultipart, allowing the `/actions/default/fire/{systemAction}`
     * method receiving multipart-form data also to be called from `/actions/default/firemultipart/{systemAction}`.
     * Swagger UI doesn't allow endpoint overloading, so this was created as an alias — both to
     * surface the endpoint and preserve backwards compatibility.
     * The wrapped function receives the @Hidden annotation, which explicitly omits it from the UI.
     * All other Swagger-specific annotations have been moved off of the original and on to this one.
     */
    @PUT
    @Path("/actions/default/firemultipart/{systemAction}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(operationId = "putFireActionByIdMultipart", summary = "Fire action by ID (multipart form) \uD83D\uDEA7",
            description = "(**Construction notice:** Still needs request body documentation. Coming soon!)\n\n" +
                    "Fires a default [system action](https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) " +
                    "on target contentlet. Uses a multipart form to transmit its data.\n\n" +
                    "Returns a map of the resultant contentlet, with an additional " +
                    "`AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
                    "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fired action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireActionDefaultMultipartNewPath(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("inode") @Parameter(
                    description = "Inode of the target content.",
                    schema = @Schema(type = "string")
            ) final String inode,
            @QueryParam("identifier") @Parameter(
                    description = "Identifier of target content.",
                    schema = @Schema(type = "string")
            ) final String identifier,
            @QueryParam("indexPolicy") @Parameter(
                    description = "Determines how target content is indexed.\n\n" +
                            "| Value | Description |\n" +
                            "|-------|-------------|\n" +
                            "| `DEFER` | Content will be indexed asynchronously, outside of " +
                                        "the current process. Valid content will finish the " +
                                        "method in process and be returned before the content " +
                                        "becomes visible in the index. This is the default " +
                                        "index policy; it is resource-friendly and well-" +
                                        "suited to batch processing. |\n" +
                            "| `WAIT_FOR` | The API call will not return from the content check " +
                                        "process until the content has been indexed. Ensures content " +
                                        "is promptly available for searching. |\n" +
                            "| `FORCE` | Forces Elasticsearch to index the content **immediately**.<br>" +
                                        "**Caution:** Using this value may cause system performance issues; " +
                                        "it is not recommended for general use, though may be useful " +
                                        "for testing purposes. |\n\n",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"DEFER", "WAIT_FOR", "FORCE"},
                            defaultValue = ""
                    )
            ) final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") @Parameter(
                    description = "Language version of target content.",
                    schema = @Schema(type = "string")
            ) final String language,
            @PathParam("systemAction") @Parameter(
                    required = true,
                    schema = @Schema(
                            type = "string",
                            allowableValues = {
                                    "NEW", "EDIT", "PUBLISH",
                                    "UNPUBLISH", "ARCHIVE", "UNARCHIVE",
                                    "DELETE", "DESTROY"
                            }
                    ),
                    description = "Default system action."
            ) final WorkflowAPI.SystemAction systemAction,
            final FormDataMultiPart multipart) {
        return fireActionDefaultMultipart(request, response, inode, identifier, indexPolicy, language,
                systemAction, multipart);
    }
    /**
     * Fires a workflow with default action with multi part body
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public final Response fireActionDefaultMultipart(
                                              @Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @QueryParam("inode")       final String inode,
                                              @QueryParam("identifier")  final String identifier,
                                              @QueryParam("indexPolicy") final String indexPolicy,
                                              @DefaultValue("-1") @QueryParam("language") final String   language,
                                              @PathParam("systemAction") final WorkflowAPI.SystemAction systemAction,
                                              final FormDataMultiPart multipart) {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .init();

        try {

            Logger.debug(this, ()-> "On Fire Action Multipart: systemAction = " + systemAction + ", inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            final FireActionForm fireActionForm = this.processForm (multipart, initDataObject.getUser());
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            this.checkContentletState (contentlet, systemAction);

            final Optional<WorkflowAction> workflowActionOpt =
                    this.workflowAPI.findActionMappedBySystemActionContentlet
                        (contentlet, systemAction, initDataObject.getUser());

            if (workflowActionOpt.isPresent()) {

                final WorkflowAction workflowAction = workflowActionOpt.get();
                final String actionId = workflowAction.getId();
                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(workflowAction,
                                this.needSave(fireActionForm), systemAction);

                return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, fireCommandOpt);
            } else {

                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(systemAction);

                if (fireCommandOpt.isPresent()) {

                    return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, null, fireCommandOpt);
                }

                final ContentType contentType = contentlet.getContentType();
                throw new DoesNotExistException("For the contentType: " + (null != contentType?contentType.variable():"unknown") +
                        " systemAction = " + systemAction);
            }
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, systemAction: " + systemAction +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    } // fire.

    /**
     * Fires a workflow action by action id, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param actionId   {@link String} (Required) action id to fire
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/{actionId}/fire")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "putFireActionById", summary = "Fire action by ID",
            description = "Fires a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions), " +
                    "specified by identifier, on a target contentlet.\n\nReturns a map of the resultant contentlet, " +
                    "with an additional `AUTO_ASSIGN_WORKFLOW` property, which can be referenced by delegate " +
                    "services that handle automatically assigning workflow schemes to content with none.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fired action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response fireActionSinglePart(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam ("actionId") @Parameter(
                    required = true,
                    description = "Identifier of a workflow action.\n\n" +
                            "Example value: `b9d89c80-3d88-4311-8365-187323c96436` " +
                            "(Default system workflow \"Publish\" action)",
                    schema = @Schema(type = "string")
            ) final String actionId,
            @QueryParam("inode") @Parameter(
                    description = "Inode of the target content.",
                    schema = @Schema(type = "string")
            ) final String inode,
            @QueryParam("identifier") @Parameter(
                    description = "Identifier of target content.",
                    schema = @Schema(type = "string")
            ) final String identifier,
            @QueryParam("indexPolicy") @Parameter(
                    description = "Determines how target content is indexed.\n\n" +
                            "| Value | Description |\n" +
                            "|-------|-------------|\n" +
                            "| `DEFER` | Content will be indexed asynchronously, outside of " +
                                            "the current process. Valid content will finish the " +
                                            "method in process and be returned before the content " +
                                            "becomes visible in the index. This is the default " +
                                            "index policy; it is resource-friendly and well-" +
                                            "suited to batch processing. |\n" +
                            "| `WAIT_FOR` | The API call will not return from the content check " +
                                            "process until the content has been indexed. Ensures content " +
                                            "is promptly available for searching. |\n" +
                            "| `FORCE` | Forces Elasticsearch to index the content **immediately**.<br>" +
                                            "**Caution:** Using this value may cause system performance issues; " +
                                            "it is not recommended for general use, though may be useful " +
                                            "for testing purposes. |\n\n",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"DEFER", "WAIT_FOR", "FORCE"},
                            defaultValue = ""
                    )
            ) final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") @Parameter(
                    description = "Language version of target content.",
                    schema = @Schema(type = "string")
            ) final String language,
            @RequestBody(
                    description = "Optional body consists of a JSON object containing various properties, " +
                            "some of which are specific to certain actionlets.\n\n" +
                            "The full list of properties that may be used with this form is as follows:\n\n" +
                            "| Property | Type | Description |\n" +
                            "|-|-|-|\n" +
                            "| `contentlet` | Object | An alternate way of specifying the target contentlet. " +
                                                        "If no identifier or inode is included via parameter, " +
                                                        "either one could instead be included in the body as a " +
                                                        "property of this object. |\n" +
                            "| `comments` | String | Comments that will appear in the [workflow tasks]" +
                                                        "(https://www.dotcms.com/docs/latest/workflow-tasks) " +
                                                        "tool with the execution of this workflow action. |\n" +
                            "| `individualPermissions` | Object | Allows setting granular permissions associated " +
                                                        "with the target. The object properties are the [system names " +
                                                        "of permissions](https://www.dotcms.com/docs/latest/user-permissions#Permissions), " +
                                                        "such as READ, PUBLISH, EDIT, etc. Their respective values " +
                                                        "are a list of user or role identifiers that should be granted " +
                                                        "the permission in question. Example: `\"READ\": " +
                                                        "[\"9ad24203-ae6a-4e5e-aa10-a8c38fd11f17\",\"MyRole\"]` |\n" +
                            "| `assign` | String | The identifier of a user or role to next receive the " +
                                                        "workflow task assignment. |\n" +
                            "| `pathToMove` | String | If the workflow action includes the Move actionlet, " +
                                                        "this property will specify the target path. This path " +
                                                        "must include a host, such as `//default/testfolder`, " +
                                                        "`//demo.dotcms.com/application`, etc. |\n" +
                            "| `query` | String | Not used in this method. |\n" +
                            "| `whereToSend` | String | For the [push publishing](push-publishing) actionlet; " +
                                                        "sets the push-publishing environment to receive the " +
                                                        "target content. Must be specified as an environment " +
                                                        "identifier. [Learn how to find environment IDs here.]" +
                                                        "(https://www.dotcms.com/docs/latest/push-publishing-endpoints#EnvironmentIds) |\n" +
                            "| `iWantTo` | String | For the push publishing actionlet; " +
                                                        "this can be set to one of three values: <ul style=\"line-height:2rem;\"><li>`publish` for " +
                                                        "push publish;</li><li>`expire` for remove;</li><li>`publishexpire` " +
                                                        "for push remove.</li></ul> These are further configurable with the " +
                                                        "properties below that specify publishing and expiration " +
                                                        "dates, times, etc. |\n" +
                            "| `publishDate` | String | For the push publishing actionlet; " +
                                                        "specifies a date to push the content. Format: `yyyy-MM-dd`.  |\n" +
                            "| `publishTime` | String | For the push publishing actionlet; " +
                                                        "specifies a time to push the content. Format: `hh-mm`. |\n" +
                            "| `expireDate` | String | For the push publishing actionlet; " +
                                                        "specifies a date to remove the content. Format: `yyyy-MM-dd`.  |\n" +
                            "| `expireTime` | String | For the push publishing actionlet; " +
                                                        "specifies a time to remove the content. Format: `hh-mm`.  |\n" +
                            "| `neverExpire` | Boolean | For the push publishing actionlet; " +
                                                        "a value of `true` invalidates the expiration time/date. |\n" +
                            "| `filterKey` | String | For the push publishing actionlet; " +
                                                        "specifies a [push publishing filter](https://www.dotcms.com/docs/latest" +
                                                        "/push-publishing-filters) key, should the workflow action " +
                                                        "call for such. To retrieve a full list of push publishing " +
                                                        "filters and their keys, use `GET /v1/pushpublish/filters`. |\n" +
                            "| `timezoneId` | String | For the push publishing actionlet; " +
                                                        "specifies the time zone to which the indicated times belong. " +
                                                        "Uses the [tz database](https://www.iana.org/time-zones). " +
                                                        "For a list of values, see [the database directly]" +
                                                        "(https://data.iana.org/time-zones/tz-link.html) or refer to " +
                                                        "[the Wikipedia entry listing tz database time zones]" +
                                                        "(https://en.wikipedia.org/wiki/List_of_tz_database_time_zones). |\n\n",
                    content = @Content(
                            schema = @Schema(implementation = FireActionForm.class)
                    )
            ) final FireActionForm fireActionForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .init();

        Logger.debug(this, ()-> "On Fire Action: action Id " + actionId + ", inode = " + inode +
                ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode = PageMode.get(request);
        //if inode is set we use it to look up a contentlet
        final Contentlet contentlet = this.getContentlet
                (inode, identifier, languageId,
                        ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                        fireActionForm, initDataObject, mode);

        if (UtilMethods.isSet(indexPolicy)) {
            contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
        }

        return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
    } // fireAction.

    private LinkedHashSet<String> getBinaryFields(final Map<String, Object> mapContent) {

        List array = (List) mapContent.getOrDefault(BINARY_FIELDS, Collections.EMPTY_LIST);


        final LinkedHashSet<String> hashSet = new LinkedHashSet<>();
        array.forEach(a -> hashSet.add(a.toString()));

        return hashSet;

    }

    private FireActionByNameForm processForm(final FormDataMultiPart multipart, final User user)
            throws IOException, JSONException, DotSecurityException, DotDataException {

        Map<String, Object> contentletMap = Collections.emptyMap();
        final FireActionByNameForm.Builder fireActionFormBuilder = new FireActionByNameForm.Builder();
        final Tuple2<Map<String,Object>, List<File>> multiPartContent =
                this.multiPartUtils.getBodyMapAndBinariesFromMultipart(multipart);

        final LinkedHashSet<String> incomingBinaryFields = this.getBinaryFields(multiPartContent._1);

        if (multiPartContent._1.containsKey(CONTENTLET)) {

            contentletMap = this.convertToContentletMap((JSONObject)multiPartContent._1.get(CONTENTLET));
        }

        this.validateMultiPartContent(contentletMap, incomingBinaryFields);
        this.processFireActionFormValues(fireActionFormBuilder, multiPartContent._1);
        this.processFiles(contentletMap, multiPartContent._2, incomingBinaryFields, user);
        fireActionFormBuilder.contentlet(contentletMap);

        return fireActionFormBuilder.build();
    }

    private Map<String, Object> convertToContentletMap(final JSONObject contentletJson) throws IOException {

        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper().
                readValue(contentletJson.toString(), Map.class);
    }

    private void validateMultiPartContent (final Map<String, Object> contentMap,
                                           final LinkedHashSet<String> binaryFields) {

        for (final String binaryField : binaryFields) {

            if (contentMap.containsKey(binaryField)) {

                throw new BadRequestException("The binary field: " + binaryField
                        + " can not be part of the " + BINARY_FIELDS + " and part of the request body");
            }
        }
    }

    private List<String> getBinaryFields (final List<Field> fields, final Map<String, Object> contentMap,
                                          final int binaryFileSize) {

        // if not any binaryField set, all fields that starts with "binary" will be used.
        final List<String> binaryFields = fields.stream()
                .filter(field -> LegacyFieldTransformer.buildLegacyFieldContent(field).startsWith(PREFIX_BINARY))
                .map(Field::variable)
                .filter(variable -> !contentMap.containsKey(variable)) // if it is not already set, probably to remove
                .collect(Collectors.toList());

        // the binary fields are not set by the user on the body request
        // the size of the binary field should be the same of the
        return binaryFields.size() <= binaryFileSize? binaryFields: binaryFields.subList(0, binaryFields.size());
    }

    private String getContentTypeInode (final Map<String, Object> contentMap, final User user, final List<File> binaryFiles) {

        this.contentHelper.checkOrSetContentType(contentMap, user, binaryFiles);
        return MapToContentletPopulator.INSTANCE.getContentTypeInode(contentMap);
    }

    private void processFiles(final Map<String, Object> contentMap, final List<File> binaryFiles,
                              final LinkedHashSet<String> argBinaryFields, final User user) throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI      = APILocator.getContentTypeAPI(APILocator.systemUser());
        final String         contentTypeInode    = this.getContentTypeInode(contentMap, user, binaryFiles);

        if (!UtilMethods.isSet(contentTypeInode)) {

            throw new BadRequestException("The content type or base type, is not set or is invalid");
        }

        final List<Field>    fields              = contentTypeAPI.find(contentTypeInode).fields();
        final List<String>   binaryFields        = argBinaryFields.size() > 0?
                new ArrayList<>(argBinaryFields) : this.getBinaryFields (fields, contentMap, binaryFiles.size());

        if (UtilMethods.isSet(contentTypeInode) && binaryFields.size() > 0) {

            final Map<String, Field> fieldsMap =
                    fields.stream().collect(Collectors.toMap(Field::variable, field -> field));
            //final int size                     =
            //        Math.min(binaryFiles.size(), binaryFields.size()); // if the user sent more files than fields, we took the min of them.

            for (int i = 0; i < binaryFields.size(); ++i) {

                final String fieldName = binaryFields.get(i);
                if (fieldsMap.containsKey(fieldName)) {

                    final Field field = fieldsMap.get(fieldName);
                    final File binary = i < binaryFiles.size()? binaryFiles.get(i): null;
                    if(binary != null){
                        // if we send null in this map the underlying APIs will understand it as if the binary needs to be removed from the map
                        // Therefore the new version of the contentlet will end up losing pieces of content
                        // if we want to remove the binary the we need to explicitly set something like image=null in the map
                        // Not sending the binary will cause no difference.
                       contentMap.put(field.variable(), binary);
                    }
                }
            }
        }
    }

    private void processFireActionFormValues(final FireActionByNameForm.Builder fireActionFormBuilder,
                                             final Map<String, Object> contentMap) {

        if (contentMap.containsKey(ASSIGN)) {

            fireActionFormBuilder.assign((String)contentMap.get(ASSIGN));
            contentMap.remove(ASSIGN);
        }

        if (contentMap.containsKey(COMMENTS)) {

            fireActionFormBuilder.comments((String)contentMap.get(COMMENTS));
            contentMap.remove(COMMENTS);
        }

        if (contentMap.containsKey(PUBLISH_DATE)) {

            fireActionFormBuilder.publishDate((String)contentMap.get(PUBLISH_DATE));
            contentMap.remove(PUBLISH_DATE);
        }

        if (contentMap.containsKey(PUBLISH_TIME)) {

            fireActionFormBuilder.publishTime((String)contentMap.get(PUBLISH_TIME));
            contentMap.remove(PUBLISH_TIME);
        }

        if (contentMap.containsKey(EXPIRE_DATE)) {

            fireActionFormBuilder.expireDate((String)contentMap.get(EXPIRE_DATE));
            contentMap.remove(EXPIRE_DATE);
        }

        if (contentMap.containsKey(EXPIRE_TIME)) {

            fireActionFormBuilder.expireTime((String)contentMap.get(EXPIRE_TIME));
            contentMap.remove(EXPIRE_TIME);
        }

        if (contentMap.containsKey(NEVER_EXPIRE)) {

            fireActionFormBuilder.neverExpire((String)contentMap.get(NEVER_EXPIRE));
            contentMap.remove(NEVER_EXPIRE);
        }

        if (contentMap.containsKey(Contentlet.WHERE_TO_SEND)) {

            fireActionFormBuilder.whereToSend((String)contentMap.get(Contentlet.WHERE_TO_SEND));
            contentMap.remove(Contentlet.WHERE_TO_SEND);
        }

        if (contentMap.containsKey(ACTION_NAME)) {

            fireActionFormBuilder.actionName((String)contentMap.get(ACTION_NAME));
            contentMap.remove(ACTION_NAME);
        }

        if (contentMap.containsKey(Contentlet.FILTER_KEY)) {
            fireActionFormBuilder.filterKey((String)contentMap.get(Contentlet.FILTER_KEY));
            contentMap.remove(Contentlet.FILTER_KEY);
        }

        if (contentMap.containsKey(Contentlet.I_WANT_TO)) {
            fireActionFormBuilder.filterKey((String)contentMap.get(Contentlet.I_WANT_TO));
            contentMap.remove(Contentlet.I_WANT_TO);
        }

        if (contentMap.containsKey(Contentlet.PATH_TO_MOVE)) {
            fireActionFormBuilder.pathToMove((String)contentMap.get(Contentlet.PATH_TO_MOVE));
            contentMap.remove(Contentlet.PATH_TO_MOVE);
        }
    }

    private Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final FireActionForm fireActionForm,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        return getContentlet(inode, identifier, language, sessionLanguage, fireActionForm, initDataObject, pageMode, VariantAPI.DEFAULT_VARIANT.name());
    }
    private Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final FireActionForm fireActionForm,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode,
                                     final String variantName) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        PageMode mode = pageMode;
        final String finalInode      = UtilMethods.isSet(inode)? inode:
                (String)Try.of(()->fireActionForm.getContentletFormData().get("inode")).getOrNull();
        final String finalIdentifier = UtilMethods.isSet(identifier)? identifier:
                (String)Try.of(()->fireActionForm.getContentletFormData().get("identifier")).getOrNull();

        if(UtilMethods.isSet(finalInode)) {

            Logger.debug(this, ()-> "Fire Action, looking for content by inode: " + finalInode);

            final Contentlet currentContentlet = this.contentletAPI.find
                    (finalInode, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.notNull(currentContentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);

            contentlet = createContentlet(fireActionForm, initDataObject, currentContentlet,mode);
        } else if (UtilMethods.isSet(finalIdentifier)) {

            Logger.debug(this, ()-> "Fire Action, looking for content by identifier: " + finalIdentifier
                    + " and language id: " + language);

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<Contentlet> currentContentlet =  language <= 0?
                    this.workflowHelper.getContentletByIdentifier(finalIdentifier, mode, initDataObject.getUser(), variantName, sessionLanguage):
                    this.contentletAPI.findContentletByIdentifierOrFallback
                            (finalIdentifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms, variantName);



            DotPreconditions.isTrue(currentContentlet.isPresent(), ()-> "contentlet-was-not-found", DoesNotExistException.class);

            contentlet = createContentlet(fireActionForm, initDataObject, currentContentlet.get(), mode);
        } else {

            //otherwise the information must be grabbed from the request body.
            Logger.debug(this, ()-> "Fire Action, creating a new contentlet");
            DotPreconditions.notNull(fireActionForm, ()-> "When no inode is sent the info on the Request body becomes mandatory.");
            contentlet = this.populateContentlet(fireActionForm, initDataObject.getUser(), mode);
        }

        return contentlet;
    }



    private Contentlet createContentlet(final FireActionForm fireActionForm,
                                        final InitDataObject initDataObject,
                                        final Contentlet currentContentlet,final PageMode mode) throws DotSecurityException {

        Contentlet contentlet = new Contentlet();
        contentlet.getMap().putAll(currentContentlet.getMap());
        if (Objects.nonNull(currentContentlet.getVariantId())) {
            contentlet.setVariantId(currentContentlet.getVariantId());
        }

        if (null != fireActionForm && null != fireActionForm.getContentletFormData() && null != contentlet) {

            contentlet = this.populateContentlet(fireActionForm, contentlet, initDataObject.getUser(),mode);
        }
        if (contentlet.getInode().isEmpty() && !currentContentlet.getInode().isEmpty()) {
            contentlet.setInode(currentContentlet.getInode());
        }
        return contentlet;
    }

    /**
     * Internal utility to populate a contentlet from a given form object
     * @param fireActionForm FireActionForm
     * @param user User
     * @return Contentlet
     * @throws DotSecurityException
     */
    private Contentlet populateContentlet(final FireActionForm fireActionForm, final User user,final PageMode mode)
            throws DotSecurityException {

        return this.populateContentlet(fireActionForm, new Contentlet(), user,mode);
    } // populateContentlet.

    /**
     * Internal utility to populate a contentlet from a given form object
     * @param fireActionForm {@link FireActionForm}
     * @param contentletInput {@link Contentlet}
     * @param user {@link User}
     * @return Contentlet
     * @throws DotSecurityException
     */
    private Contentlet populateContentlet(final FireActionForm fireActionForm, final Contentlet contentletInput, final User user,final PageMode mode)
            throws DotSecurityException {

        if (contentletInput.isNew()) {
            // checks if has content type assigned, otherwise tries to see
            // if can figure out the content type based on a base type.
            this.contentHelper.checkOrSetContentType(fireActionForm.getContentletFormData(), user);
        }

        final Contentlet contentlet = this.contentHelper.populateContentletFromMap
                (contentletInput, fireActionForm.getContentletFormData());

        final Supplier<String> errorMessageSupplier = () -> {

            String message = "no-permissions-contenttype";

            try {
                message = LanguageUtil.get(user.getLocale(),
                        message, user.getUserId(), contentlet.getContentType().id());
            } catch (LanguageException e) {
                throw new ForbiddenException(message);
            }

            return message;
        };

        if (null == contentlet || null == contentlet.getContentType()) {

            throw new DotContentletValidationException("Workflow-does-not-exists-content-type");
        }

        try {
            if (!this.permissionAPI.doesUserHavePermission(contentlet.getContentType(),
                    PermissionAPI.PERMISSION_READ, user, mode.respectAnonPerms)) {
                throw new DotSecurityException(errorMessageSupplier.get());
            }
        } catch (DotDataException e) {
            throw new DotSecurityException(errorMessageSupplier.get(), e);
        }

        contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE, fireActionForm.getPublishDate());
        contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME, fireActionForm.getPublishTime());
        contentlet.setStringProperty(Contentlet.WORKFLOW_TIMEZONE_ID, fireActionForm.getTimezoneId());
        contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE,  fireActionForm.getExpireDate());
        contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME,  fireActionForm.getExpireTime());
        contentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE, fireActionForm.getNeverExpire());
        contentlet.setStringProperty(Contentlet.WHERE_TO_SEND,   fireActionForm.getWhereToSend());
        contentlet.setStringProperty(Contentlet.FILTER_KEY, fireActionForm.getFilterKey());
        contentlet.setStringProperty(Contentlet.I_WANT_TO, fireActionForm.getFilterKey());
        if (UtilMethods.isSet(fireActionForm.getPathToMove())) {
            contentlet.setStringProperty(Contentlet.PATH_TO_MOVE, fireActionForm.getPathToMove());
        }

        for(Field constant : contentlet.getContentType().fields()) {
          if(constant instanceof ConstantField)
            contentlet.getMap().put(constant.variable(), constant.values());
        }
        
        return contentlet;
    } // populateContentlet.

    /**
     * Change the order of an action associated to the step
     * @param request                           HttpServletRequest
     * @param workflowReorderActionStepForm     WorkflowReorderBean
     * @return Response
     */
    @PUT
    @Path("/reorder/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putReorderWorkflowActionsInStep", summary = "Change the order of actions within a workflow step",
            description = "Updates a [workflow action](https://www.dotcms.com/docs/latest/managing-workflows#Actions)'s " +
                    "order within a [step](https://www.dotcms.com/docs/latest/managing-workflows#Steps) by assigning it " +
                    "a numeric order.\n\nReturns \"Ok\" on success.\n\n",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated workflow action successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "404", description = "Workflow step or action not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response reorderAction(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response,
                                        @PathParam("stepId") @Parameter(
                                                required = true,
                                                description = "Identifier of the step containing the action.",
                                                schema = @Schema(type = "string")
                                        ) final String stepId,
                                        @PathParam("actionId") @Parameter(
                                                required = true,
                                                description = "Identifier of the action to reorder.",
                                                schema = @Schema(type = "string")
                                        ) final String actionId,
                                        final @RequestBody(
                                                description = "Body consists of a JSON object containing the single property " +
                                                        "`order`, which is assigned an integer value.",
                                                required = true,
                                                content = @Content(schema = @Schema(implementation = WorkflowReorderWorkflowActionStepForm.class))
                                        ) WorkflowReorderWorkflowActionStepForm workflowReorderActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {
            DotPreconditions.notNull(workflowReorderActionStepForm,"Expected Request body was empty.");
            Logger.debug(this, "Doing reordering of: " + workflowReorderActionStepForm);
            this.workflowHelper.reorderAction(
                    new WorkflowReorderBean.Builder().stepId(stepId).actionId(actionId)
                            .order(workflowReorderActionStepForm.getOrder()).build(),
                    initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on reorderAction, workflowReorderActionStepForm: " + workflowReorderActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // reorderAction

    /**
     * Do an export of the scheme with all dependencies to rebuild it (such as steps and actions)
     * in addition the permission (who can use) will be also returned.
     * @param httpServletRequest HttpServletRequest
     * @param workflowSchemeImportForm WorkflowSchemeImportObjectForm
     * @return Response
     */
    @POST
    @Path("/schemes/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postImportScheme", summary = "Import a workflow scheme",
            description = "Import a [workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).\n\n" +
                    "Returns \"OK\" on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Imported workflow scheme successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response importScheme(@Context final HttpServletRequest  httpServletRequest,
                                       @Context final HttpServletResponse httpServletResponse,
                                       @RequestBody(
                                               description = "Body consists of a JSON object containing two properties: \n\n" +
                                                       "| Property | Type | Description |\n" +
                                                       "|-|-|-|\n" +
                                                       "| `workflowObject` | Object | An entire scheme along with steps and actions, " +
                                                                                            "such as received from the corresponding export " +
                                                                                            "method. |\n" +
                                                       "| `permissions` | List of Objects | A list of permission objects, such as received " +
                                                                                            "from the corresponding export method. |\n\n" +
                                                       "The simplest way to perform an import is to pass the full value of the `entity` property " +
                                                       "returned by the corresponding Workflow Scheme Export endpoint as the data payload.",
                                               required = true,
                                               content = @Content(
                                                       schema = @Schema(implementation = WorkflowSchemeImportObjectForm.class)
                                               )
                                       ) final WorkflowSchemeImportObjectForm workflowSchemeImportForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, httpServletRequest, httpServletResponse, true, null);
        Response response;

        try {
            DotPreconditions.notNull(workflowSchemeImportForm,"Expected Request body was empty.");
            Logger.debug(this, "Importing the workflow schemes");

            this.workflowAPI.isUserAllowToModifiedWorkflow(initDataObject.getUser());

            final WorkflowSchemeImportExportObject exportObject = new WorkflowSchemeImportExportObject();
            exportObject.setSchemes(workflowSchemeImportForm.getWorkflowImportObject().getSchemes());
            exportObject.setSteps  (workflowSchemeImportForm.getWorkflowImportObject().getSteps());
            exportObject.setActions(workflowSchemeImportForm.getWorkflowImportObject().getActions());
            exportObject.setActionSteps(workflowSchemeImportForm.getWorkflowImportObject().getActionSteps());
            exportObject.setActionClasses(workflowSchemeImportForm.getWorkflowImportObject().getActionClasses());
            exportObject.setActionClassParams(workflowSchemeImportForm.getWorkflowImportObject().getActionClassParams());
            exportObject.setSchemeSystemActionWorkflowActionMappings(workflowSchemeImportForm.getWorkflowImportObject().getSchemeSystemActionWorkflowActionMappings());

            this.workflowHelper.importScheme (
                    exportObject,
                    workflowSchemeImportForm.getPermissions(),
                    initDataObject.getUser());
            response     = Response.ok(new ResponseEntityView("OK")).build(); // 200
        } catch (Exception e){

            Logger.error(this.getClass(),
                    "Exception on importScheme, Error importing schemes", e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    } // importScheme.

    /**
     * Do an export of the scheme with all dependencies to rebuild it (such as steps and actions) in
     * addition the permission (who can use) will be also returned.
     *
     * @param httpServletRequest HttpServletRequest
     * @param schemeIdOrVariable String (scheme id or variable)
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeIdOrVariable}/export")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getExportScheme", summary = "Export a workflow scheme",
            description = "Export a [workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).\n\n" +
                    "Returns the full workflow scheme, along with steps, actions, permissions, etc., on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Exported workflow scheme successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class),
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": {\n" +
                                            "    \"permissions\": [\n" +
                                            "      {\n" +
                                            "        \"bitPermission\": false,\n" +
                                            "        \"id\": 0,\n" +
                                            "        \"individualPermission\": true,\n" +
                                            "        \"inode\": \"string\",\n" +
                                            "        \"permission\": 0,\n" +
                                            "        \"roleId\": \"string\",\n" +
                                            "        \"type\": \"string\"\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"workflowObject\": {\n" +
                                            "      \"actionClassParams\": [\n" +
                                            "        {\n" +
                                            "          \"actionClassId\": \"string\",\n" +
                                            "          \"id\": null,\n" +
                                            "          \"key\": \"string\",\n" +
                                            "          \"value\": null\n" +
                                            "        }\n" +
                                            "      ],\n" +
                                            "      \"actionClasses\": [\n" +
                                            "        {\n" +
                                            "          \"actionId\": \"string\",\n" +
                                            "          \"actionlet\": {\n" +
                                            "            \"actionClass\": \"string\",\n" +
                                            "            \"howTo\": \"string\",\n" +
                                            "            \"localizedHowto\": \"string\",\n" +
                                            "            \"localizedName\": \"string\",\n" +
                                            "            \"name\": \"string\",\n" +
                                            "            \"nextStep\": null,\n" +
                                            "            \"parameters\": [\n" +
                                            "              {\n" +
                                            "                \"defaultValue\": \"\",\n" +
                                            "                \"displayName\": \"string\",\n" +
                                            "                \"key\": \"string\",\n" +
                                            "                \"required\": false\n" +
                                            "              }\n" +
                                            "            ]\n" +
                                            "          },\n" +
                                            "          \"clazz\": \"string\",\n" +
                                            "          \"id\": \"string\",\n" +
                                            "          \"name\": \"string\",\n" +
                                            "          \"order\": 0\n" +
                                            "        }\n" +
                                            "      ],\n" +
                                            "      \"actionSteps\": [\n" +
                                            "        {\n" +
                                            "          \"actionId\": \"string\",\n" +
                                            "          \"actionOrder\": \"0\",\n" +
                                            "          \"stepId\": \"string\"\n" +
                                            "        }\n" +
                                            "      ],\n" +
                                            "      \"actions\": [\n" +
                                            "        {\n" +
                                            "          \"assignable\": false,\n" +
                                            "          \"commentable\": false,\n" +
                                            "          \"condition\": \"\",\n" +
                                            "          \"icon\": \"string\",\n" +
                                            "          \"id\": \"string\",\n" +
                                            "          \"metadata\": null,\n" +
                                            "          \"name\": \"string\",\n" +
                                            "          \"nextAssign\": \"string\",\n" +
                                            "          \"nextStep\": \"string\",\n" +
                                            "          \"nextStepCurrentStep\": true,\n" +
                                            "          \"order\": 0,\n" +
                                            "          \"owner\": null,\n" +
                                            "          \"roleHierarchyForAssign\": false,\n" +
                                            "          \"schemeId\": \"string\",\n" +
                                            "          \"showOn\": []\n" +
                                            "        }\n" +
                                            "      ],\n" +
                                            "      \"schemeSystemActionWorkflowActionMappings\": [\n" +
                                            "        {\n" +
                                            "          \"identifier\": \"string\",\n" +
                                            "          \"owner\": {\n" +
                                            "            \"archived\": false,\n" +
                                            "            \"creationDate\": 1723806880187,\n" +
                                            "            \"defaultScheme\": false,\n" +
                                            "            \"description\": \"string\",\n" +
                                            "            \"entryActionId\": null,\n" +
                                            "            \"id\": \"string\",\n" +
                                            "            \"mandatory\": false,\n" +
                                            "            \"modDate\": 1723796816309,\n" +
                                            "            \"name\": \"string\",\n" +
                                            "            \"system\": false,\n" +
                                            "            \"variableName\": \"string\"\n" +
                                            "          },\n" +
                                            "          \"systemAction\": \"string\",\n" +
                                            "          \"workflowAction\": {\n" +
                                            "            \"assignable\": false,\n" +
                                            "            \"commentable\": false,\n" +
                                            "            \"condition\": \"\",\n" +
                                            "            \"icon\": \"string\",\n" +
                                            "            \"id\": \"string\",\n" +
                                            "            \"metadata\": null,\n" +
                                            "            \"name\": \"string\",\n" +
                                            "            \"nextAssign\": \"string\",\n" +
                                            "            \"nextStep\": \"string\",\n" +
                                            "            \"nextStepCurrentStep\": true,\n" +
                                            "            \"order\": 0,\n" +
                                            "            \"owner\": null,\n" +
                                            "            \"roleHierarchyForAssign\": false,\n" +
                                            "            \"schemeId\": \"string\",\n" +
                                            "            \"showOn\": []\n" +
                                            "          },\n" +
                                            "          \"ownerContentType\": false,\n" +
                                            "          \"ownerScheme\": true\n" +
                                            "        }\n" +
                                            "      ],\n" +
                                            "      \"schemes\": [\n" +
                                            "        {\n" +
                                            "          \"archived\": false,\n" +
                                            "          \"creationDate\": 1723806880187,\n" +
                                            "          \"defaultScheme\": false,\n" +
                                            "          \"description\": \"string\",\n" +
                                            "          \"entryActionId\": null,\n" +
                                            "          \"id\": \"string\",\n" +
                                            "          \"mandatory\": false,\n" +
                                            "          \"modDate\": 1723796816309,\n" +
                                            "          \"name\": \"string\",\n" +
                                            "          \"system\": false,\n" +
                                            "          \"variableName\": \"string\"\n" +
                                            "        }\n" +
                                            "      ],\n" +
                                            "      \"steps\": [\n" +
                                            "        {\n" +
                                            "          \"creationDate\": 1723806894533,\n" +
                                            "          \"enableEscalation\": false,\n" +
                                            "          \"escalationAction\": null,\n" +
                                            "          \"escalationTime\": 0,\n" +
                                            "          \"id\": \"string\",\n" +
                                            "          \"myOrder\": 0,\n" +
                                            "          \"name\": \"string\",\n" +
                                            "          \"resolved\": false,\n" +
                                            "          \"schemeId\": \"string\"\n" +
                                            "        }\n" +
                                            "      ],\n" +
                                            "      \"version\": \"string\"\n" +
                                            "    }\n" +
                                            "  },\n" +
                                            "  \"errors\": [],\n" +
                                            "  \"i18nMessagesMap\": {},\n" +
                                            "  \"messages\": [],\n" +
                                            "  \"pagination\": null,\n" +
                                            "  \"permissions\": []\n" +
                                            "}")
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Workflow scheme not found")
            }
    )
    public final Response exportScheme(@Context final HttpServletRequest  httpServletRequest,
                                       @Context final HttpServletResponse httpServletResponse,
            @PathParam("schemeIdOrVariable") @Parameter(
                    required = true,
                    description = "Identifier or variable name of the workflow scheme to export.",
                    schema = @Schema(type = "string")
            ) final String schemeIdOrVariable) {

        final InitDataObject initDataObject = this.webResource.init
                (null, httpServletRequest, httpServletResponse,true, null);
        Response response;
        WorkflowSchemeImportExportObject exportObject;
        List<Permission>                 permissions;
        WorkflowScheme                   scheme;

        try {

            Logger.debug(this, "Exporting the workflow scheme: " + schemeIdOrVariable);
            this.workflowAPI.isUserAllowToModifiedWorkflow(initDataObject.getUser());

            scheme = this.workflowAPI.findScheme(schemeIdOrVariable);
            exportObject = this.workflowImportExportUtil.buildExportObject(Arrays.asList(scheme));
            permissions  = this.workflowHelper.getActionsPermissions(exportObject.getActions());
            response     = Response.ok(new ResponseEntityView(
                    Map.of("workflowObject", new WorkflowSchemeImportExportObjectView(VERSION, exportObject),
                            "permissions", permissions))).build(); // 200
        } catch (Exception e){
            Logger.error(this.getClass(),
                    "Exception on exportScheme, Error exporting the schemes", e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    } // exportScheme.

    /**
     * Do a deep copy of the scheme including steps, action, permissions and so on.
     * You can include a query string name, to include the scheme name
     * @param httpServletRequest  HttpServletRequest
     * @param schemeId String
     * @param name String
     * @param workflowCopyForm (Optional param. use it to set any specifics on the new scheme)
     * @return Response
     */
    @POST
    @Path("/schemes/{schemeId}/copy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postCopyScheme", summary = "Copy a workflow scheme",
            description = "Copy a [workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).\n\n " +
                    "A name for the new scheme may be provided either by parameter or by POST body property; if no name " +
                    "is supplied, the name will be that of the copied workflow scheme with the current Unix epoch " +
                    "timestamp integer appended.\n\n" +
                    "Returns copied workflow scheme on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Copied workflow scheme successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowSchemeView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "404", description = "Workflow scheme not found"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response copyScheme(@Context final HttpServletRequest httpServletRequest,
                                     @Context final HttpServletResponse httpServletResponse,
                                     @PathParam("schemeId") @Parameter(
                                             required = true,
                                             description = "Identifier of workflow scheme.\n\n" +
                                                     "Example value: `d61a59e1-a49c-46f2-a929-db2b4bfa88b2` " +
                                                     "(Default system workflow)",
                                             schema = @Schema(type = "string")
                                     ) final String schemeId,
                                     @QueryParam("name") @Parameter(
                                             description = "Name of new scheme from copy.\n\nNote: A name with a length " +
                                                     "less than 2 characters or greater than 100 may require renaming before " +
                                                     "certain actions, such as archiving, can be taken on it.",
                                             schema = @Schema(type = "string")
                                     ) final String name,
                                     @RequestBody(
                                             description = "Body consists of a `name` property; an alternate way to supply " +
                                                     "the name of the new scheme, instead of parameter.\n\n Name supplied " +
                                                     "this way must be at minimum 2 and at maximum 100 characters in length.",
                                             content = @Content(
                                                     schema = @Schema(implementation = WorkflowCopyForm.class)
                                             )
                                     ) final WorkflowCopyForm workflowCopyForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, httpServletRequest, httpServletResponse,true, null);
        Response response;

        try {
            final Optional<String> workflowName = (
                    UtilMethods.isSet(name) ? Optional.of(name) :
                            (
                                    UtilMethods.isSet(workflowCopyForm) ? Optional
                                            .of(workflowCopyForm.getName()) :
                                            Optional.empty()
                            )
            );

            Logger.debug(this, "Copying the workflow scheme: " + schemeId);
            response     = Response.ok(new ResponseEntityView(
                    this.workflowAPI.deepCopyWorkflowScheme(
                            this.workflowAPI.findScheme(schemeId),
                            initDataObject.getUser(), workflowName))
            ).build(); // 200
        } catch (Exception e){
            Logger.error(this.getClass(),
                    "Exception on exportScheme, Error exporting the schemes", e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    } // exportScheme.

    /**
     * Returns all the possible default actions associated to the content type workflow schemes.
     * 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/defaultactions/contenttype/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getDefaultActionsByContentTypeId", summary = "Find possible default actions by content type",
            description = "Returns a list of actions that may be used as a [default action]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) for a " +
                    "specified [content type](https://www.dotcms.com/docs/latest/content-types), along with their " +
                    "associated [workflow schemes](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Default action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityDefaultWorkflowActionsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Content type not found")
            }
    )
    public final ResponseEntityDefaultWorkflowActionsView findAvailableDefaultActionsByContentType(@Context final HttpServletRequest request,
                                                                   @Context final HttpServletResponse response,
                                                                   @PathParam("contentTypeId") @Parameter(
                                                                           required = true,
                                                                           description = "Identifier or variable of content type to examine for actions.\n\n" +
                                                                                   "Example ID: `c541abb1-69b3-4bc5-8430-5e09e5239cc8` (Default page content type)\n\n" +
                                                                                   "Example Variable: `htmlpageasset` (Default page content type)",
                                                                           schema = @Schema(type = "string")
                                                                   ) final String contentTypeId) throws NotFoundInDbException {
        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
            Logger.debug(this,
                    () -> "Getting the available workflow schemes default action for the ContentType: "
                            + contentTypeId );
            final List<WorkflowDefaultActionView> actions = this.workflowHelper.findAvailableDefaultActionsByContentType(contentTypeId, initDataObject.getUser());
            return new ResponseEntityDefaultWorkflowActionsView(actions);
    } // findAvailableDefaultActionsByContentType.

    /**
     *
     * Returns all the possible default actions associated to the workflow schemes.
     * 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/defaultactions/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getDefaultActionsBySchemeIds", summary = "Find possible default actions by scheme(s)",
            description = "Returns a list of actions that are eligible to be used as a [default action]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#DefaultActions) for one or " +
                    "more [workflow schemes](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityDefaultWorkflowActionsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow action not found")
            }
    )
    public final Response findAvailableDefaultActionsBySchemes(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("ids") @Parameter(
                    required = true,
                    description = "Comma-separated list of workflow scheme identifiers.",
                    schema = @Schema(type = "string")
            ) String schemeIds) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this,
                    ()->"Getting the available workflow schemes default action for the schemes: "
                            + schemeIds);
            final List<WorkflowDefaultActionView> actions = this.workflowHelper
                    .findAvailableDefaultActionsBySchemes(schemeIds, initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on find Available Default Actions exception message: " + e
                            .getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findAvailableDefaultActionsBySchemes.

    /**
     * Finds the available actions of the initial/first step(s) of the workflow scheme(s) associated
     * with a content type Id.
     * @param request HttpServletRequest
     * @param contentTypeId String
     * @return Response
     */
    @GET
        @Path("/initialactions/contenttype/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getInitialActionsByContentTypeId", summary = "Find initial actions by content type",
            description = "Returns a list of available actions of the initial/first step(s) of the workflow scheme(s) " +
                    "associated with a [content type](https://www.dotcms.com/docs/latest/content-types).",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Initial action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityDefaultWorkflowActionsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Content type not found")
            }
    )
    public final Response findInitialAvailableActionsByContentType(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("contentTypeId") @Parameter(
                    required = true,
                    description = "Identifier or variable of content type to examine for initial actions.\n\n" +
                            "Example ID: `c541abb1-69b3-4bc5-8430-5e09e5239cc8` (Default page content type)\n\n" +
                            "Example Variable: `htmlpageasset` (Default page content type)",
                    schema = @Schema(type = "string")
            ) final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this,
                    ()->"Getting the available actions for the contentlet inode: " + contentTypeId);
            final boolean includeSeparator = ConversionUtils.toBoolean(request.getParameter(INCLUDE_SEPARATOR), false) ;
            final List<WorkflowDefaultActionView> actions = includeSeparator?
                    this.workflowHelper.findInitialAvailableActionsByContentType(contentTypeId,
                            initDataObject.getUser()):
                    this.workflowHelper.findInitialAvailableActionsByContentTypeSkippingSeparators(contentTypeId,
                            initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findInitialAvailableActionsByContentType, content type id: "
                            + contentTypeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findInitialAvailableActionsByContentType.

    /**
     * Creates a new scheme
     *
     * @param request HttpServletRequest
     * @param workflowSchemeForm WorkflowSchemeForm
     * @return Response
     */
    @POST
    @Path("/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postSaveScheme", summary = "Create a workflow scheme",
            description = "Create a [workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).\n\n " +
                    "Returns created workflow scheme on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Copied workflow scheme successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowSchemeView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response saveScheme(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @RequestBody(
                                             description = "The request body consists of the following three properties:\n\n" +
                                                     "| Property | Type | Description |\n" +
                                                     "|-|-|-|\n" +
                                                     "| `schemeName` | String | The workflow scheme's name. |\n" +
                                                     "| `schemeDescription` | String | A description of the scheme. |\n" +
                                                     "| `schemeArchived` | Boolean | If `true`, the scheme will be created " +
                                                                                    "in an archived state. |\n",
                                             content = @Content(
                                                     schema = @Schema(implementation = WorkflowSchemeForm.class)
                                             )
                                     ) final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        try {
            DotPreconditions.notNull(workflowSchemeForm,"Expected Request body was empty.");
            Logger.debug(this, ()->"Saving scheme named: " + workflowSchemeForm.getSchemeName());
            final WorkflowScheme scheme = this.workflowHelper.saveOrUpdate(null, workflowSchemeForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(scheme)).build(); // 200
        } catch (Exception e) {
            final String schemeName = workflowSchemeForm == null ? "" : workflowSchemeForm.getSchemeName();
            Logger.error(this.getClass(), "Exception on save, schema named: " + schemeName + ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }


    /**
     * Updates an existing scheme
     * @param request HttpServletRequest
     * @param workflowSchemeForm WorkflowSchemeForm
     * @return Response
     */
    @PUT
    @Path("/schemes/{schemeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putUpdateWorkflowScheme", summary = "Update a workflow scheme",
            description = "Updates a [workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes).\n\n" +
                    "Returns updated scheme on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated workflow scheme successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "404", description = "Workflow scheme not found."),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final Response updateScheme(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("schemeId") @Parameter(
                                               required = true,
                                               description = "Identifier of workflow scheme.\n\n" +
                                                       "Example value: `d61a59e1-a49c-46f2-a929-db2b4bfa88b2` (Default system workflow)",
                                               schema = @Schema(type = "string")
                                       ) final String schemeId,
                                       @RequestBody(
                                               description = "The request body consists of the following three properties:\n\n" +
                                                       "| Property | Type | Description |\n" +
                                                       "|-|-|-|\n" +
                                                       "| `schemeName` | String | The workflow scheme's name. |\n" +
                                                       "| `schemeDescription` | String | A description of the scheme. |\n" +
                                                       "| `schemeArchived` | Boolean | If `true`, the scheme will be be placed " +
                                                       "in an archived state. |\n",
                                               content = @Content(
                                                       schema = @Schema(implementation = WorkflowSchemeForm.class)
                                               )
                                       ) final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        Logger.debug(this, "Updating scheme with id: " + schemeId);
        try {
            DotPreconditions.notNull(workflowSchemeForm,"Expected Request body was empty.");
            final User           user   = initDataObject.getUser();
            final WorkflowScheme scheme = this.workflowHelper.saveOrUpdate(schemeId, workflowSchemeForm, user);
            return Response.ok(new ResponseEntityView(scheme)).build(); // 200
        }  catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to update schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Deletes an existing scheme (the response is async)
     * @param request HttpServletRequest
     * @return Response
     */
    @DELETE
    @Path("/schemes/{schemeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "deleteWorkflowSchemeById", summary = "Delete a workflow scheme",
            description = "Deletes a [workflow scheme](https://www.dotcms.com/docs/latest/managing-workflows#Schemes)\n\n" +
                    "Scheme must already be in an archived state.\n\n" +
                    "Returns deleted workflow scheme on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow scheme deleted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowSchemeView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Workflow scheme not found")
            }
    )
    public final void deleteScheme(@Context final HttpServletRequest request,
                                   @Suspended final AsyncResponse asyncResponse,
                                   @PathParam("schemeId") @Parameter(
                                           required = true,
                                           description = "Identifier of workflow scheme to delete.",
                                           schema = @Schema(type = "string")
                                   ) final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init(null, request,new EmptyHttpResponse(), true, null);
        Logger.debug(this, ()-> "Deleting scheme with id: " + schemeId);
        try {

            ResponseUtil.handleAsyncResponse(
                    this.workflowHelper.delete(schemeId, initDataObject.getUser()), asyncResponse);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to delete schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    } // deleteScheme.

    /**
     * Returns the status of a specific piece of Content in the Workflow it is assigned to. In
     * summary:
     * <ul>
     *     <li>The Workflow Scheme that the Contentlet is in.</li>
     *     <li>The Step that the Contentlet is in.</li>
     *     <li>The User assigned to such a Step.</li>
     * </ul>
     * Here's an example of how to use this endpoint:
     * <pre>
     *     http://localhost:8080/api/v1/workflow/status/{contentletInode}
     * </pre>
     *
     * @param request         The current instance of the {@link HttpServletRequest}.
     * @param response        The current instance of the {@link HttpServletResponse}.
     * @param contentletInode The inode of the Contentlet whose status will be checked.
     *
     * @return The status information of the Contentlet in the Workflow it is assigned to.
     *
     * @throws DotDataException          The specified Contentlet Inode was not found.
     * @throws DotSecurityException      The User calling this endpoint does not have required
     *                                   permissions to do so.
     * @throws InvocationTargetException Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     * @throws IllegalAccessException    Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     */
    @GET
    @Path("/status/{contentletInode}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getContentWorkflowStatusByInode", summary = "Find workflow status of content",
            description = "Checks the current workflow status of a contentlet by its [inode]" +
                    "(https://www.dotcms.com/docs/latest/content-versions#IdentifiersInodes).\n\n" +
                    "Returns an object containing the associated [workflow scheme]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#Schemes), [workflow step]" +
                    "(https://www.dotcms.com/docs/latest/managing-workflows#Steps), and [workflow task]" +
                    "(https://www.dotcms.com/docs/latest/workflow-tasks) associated with the contentlet.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseContentletWorkflowStatusView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request")
            }
    )
    public final ResponseContentletWorkflowStatusView getStatusForContentlet(@Context final HttpServletRequest request,
                                                                             @Context final HttpServletResponse response,
                                                                             @PathParam("contentletInode") @Parameter(
                                                                                     required = true,
                                                                                     description = "Inode of content version to inspect for workflow status.\n\n",
                                                                                     schema = @Schema(type = "string")
                                                                             ) final String contentletInode)
            throws DotDataException, DotSecurityException, InvocationTargetException, IllegalAccessException {
        Logger.debug(this, String.format("Retrieving Workflow status for Contentlet with Inode " +
                "'%s'", contentletInode));
        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        final User user = initDataObject.getUser();
        WorkflowStep wfStep = null;
        WorkflowScheme scheme = null;
        WorkflowTask wfTask = new WorkflowTask();
        final Contentlet contentlet = this.contentletAPI.find(contentletInode, user, false);
        final Optional<WorkflowStep> stepOpt = this.workflowAPI.findCurrentStep(contentlet);
        if (stepOpt.isPresent()) {
            wfStep = stepOpt.get();
            scheme = this.workflowAPI.findScheme(wfStep.getSchemeId());
        }
        final WorkflowTask originalTask = this.workflowAPI.findTaskByContentlet(contentlet);
        if (null != originalTask) {
            BeanUtils.copyProperties(wfTask, originalTask);
            wfTask = this.workflowHelper.handleWorkflowTaskData(wfTask);
        }
        return new ResponseContentletWorkflowStatusView(new ContentletWorkflowStatusView(scheme,
                wfStep, wfTask));
    }

    /**
     * Returns the status of a specific piece of Content in the Workflow it is assigned to. In
     * summary:
     * <ul>
     *     <li>The Workflow Scheme that the Contentlet is in.</li>
     *     <li>The Step that the Contentlet is in.</li>
     *     <li>The User assigned to such a Step.</li>
     * </ul>
     * Here's an example of how to use this endpoint:
     * <pre>
     *     http://localhost:8080/api/v1/workflow/status/{contentletInode}
     * </pre>
     *
     * @param request         The current instance of the {@link HttpServletRequest}.
     * @param response        The current instance of the {@link HttpServletResponse}.
     * @param contentletIdentifier The inode of the Contentlet whose status will be checked.
     *
     * @return The status information of the Contentlet in the Workflow it is assigned to.
     *
     * @throws DotDataException          The specified Contentlet Inode was not found.
     * @throws DotSecurityException      The User calling this endpoint does not have required
     *                                   permissions to do so.
     * @throws InvocationTargetException Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     * @throws IllegalAccessException    Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     */
    @GET
    @Path("/tasks/history/comments/{contentletIdentifier}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowTasksHistoryComments", summary = "Find workflow tasks history and comments of content",
            description = "Retrieve the workflow tasks comments of a contentlet by its [id]" +
                    "(https://www.dotcms.com/docs/latest/content-versions#IdentifiersInodes).\n\n" +
                    "Returns an object containing the associated [workflow history or comments]" +
                    "https://www2.dotcms.com/docs/latest/workflow-tasks, [workflow task]",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowHistoryCommentsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request")
            }
    )
    public final ResponseEntityWorkflowHistoryCommentsView getWorkflowTasksHistoryComments(@Context final HttpServletRequest request,
                                                                                         @Context final HttpServletResponse response,
                                                                                         @PathParam("contentletIdentifier") @Parameter(
                                                                                     required = true,
                                                                                     description = "Id of content  to inspect for workflow tasks.\n\n",
                                                                                     schema = @Schema(type = "string")
                                                                             ) final String contentletIdentifier,
                                                                                         @DefaultValue("-1") @QueryParam("language") @Parameter(
                                                                               description = "Language version of target content.",
                                                                               schema = @Schema(type = "string")) final String language
                                                                       )
            throws DotDataException, DotSecurityException, InvocationTargetException, IllegalAccessException {

        Logger.debug(this, String.format("Retrieving Workflow tasks for Contentlet with identifier " +
                "'%s'", contentletIdentifier));
        final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true).requiredFrontendUser(false).init();

        final User user = initDataObject.getUser();
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode = PageMode.get(request);

        final Optional<Contentlet> currentContentlet =  languageId <= 0?
                this.workflowHelper.getContentletByIdentifier(contentletIdentifier, mode, initDataObject.getUser(),
                        ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId()):
                this.contentletAPI.findContentletByIdentifierOrFallback
                        (contentletIdentifier, mode.showLive, languageId, initDataObject.getUser(), mode.respectAnonPerms);

        if (currentContentlet.isPresent()) {

            final WorkflowTask currentWorkflowTask = this.workflowAPI.findTaskByContentlet(currentContentlet.get());
            final List<WorkflowTimelineItem> workflowComments = this.workflowAPI.getCommentsAndChangeHistory(currentWorkflowTask);
            final List<WorkflowTimelineItemView> workflowTimelineItemViews = workflowComments.stream()
                    .map(this::toWorkflowTimelineItemView)
                    .collect(Collectors.toList());
            return new ResponseEntityWorkflowHistoryCommentsView(workflowTimelineItemViews);
        }

        throw new DoesNotExistException("Contentlet with identifier " + contentletIdentifier + " does not exist.");
    }

    private WorkflowTimelineItemView toWorkflowTimelineItemView(final WorkflowTimelineItem wfTimeLine) {

        final WorkflowStep step = wfTimeLine instanceof WorkflowHistory && UtilMethods.isSet(wfTimeLine.stepId())?
                Try.of(()->this.workflowHelper.findStepById(wfTimeLine.stepId())).getOrNull():null;
        final WorkflowAction action = wfTimeLine instanceof WorkflowHistory &&  UtilMethods.isSet(wfTimeLine.actionId())?
                Try.of(()->this.workflowHelper.findAction(wfTimeLine.actionId(), APILocator.systemUser())).getOrNull():null;

        final Map<String, String> minimalStepViewMap = new HashMap<>();
        if (Objects.nonNull(step)) {

            minimalStepViewMap.put("id",step.getId());
            minimalStepViewMap.put("name",step.getName());
            minimalStepViewMap.put("schemeId",step.getSchemeId());
        }

        final Map<String, String> minimalActionViewMap = new HashMap<>();
        if (Objects.nonNull(action)) {

            minimalActionViewMap.put("id",action.getId());
            minimalActionViewMap.put("name",action.getName());
            minimalActionViewMap.put("schemeId",action.getSchemeId());
        }

        final String postedBy = this.workflowHelper.getPostedBy(wfTimeLine.roleId());
        return new WorkflowTimelineItemView(wfTimeLine.createdDate(), wfTimeLine.roleId(), postedBy,
                wfTimeLine.commentDescription(), wfTimeLine.taskId(), wfTimeLine.type(), minimalStepViewMap, minimalActionViewMap);
    }

    /**
     * Creates a new workflow comment
     *
     * @param request HttpServletRequest
     * @param workflowSchemeForm WorkflowSchemeForm
     * @return Response
     */
    @POST
    @Path("/{contentletId}/comments")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postSaveScheme", summary = "Create a workflow comment",
            description = "Create a [workflow comment].\n\n " +
                    "Returns created workflow comment on success.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Copied workflow comment successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowCommentView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type")
            }
    )
    public final ResponseEntityWorkflowCommentView saveComment(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @PathParam("contentletId") @Parameter(
                                             required = true,
                                             description = "Identifier of contentlet to add comment.",
                                             schema = @Schema(type = "string")
                                     ) final String contentletId,
                                     @DefaultValue("-1") @QueryParam("language") @Parameter(
                                           description = "Language version of target content.",
                                           schema = @Schema(type = "string")) final String language,
                                     @RequestBody(
                                             description = "The request body consists of the following three properties:\n\n" +
                                                     "| Property | Type | Description |\n" +
                                                     "|-|-|-|\n" +
                                                     "| `comment` | String | The workflow comment. |\n",
                                             content = @Content(
                                                     schema = @Schema(implementation = WorkflowCommentForm.class)
                                             )
                                     ) final WorkflowCommentForm workflowCommentForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true).requiredFrontendUser(false).init();

        DotPreconditions.notNull(workflowCommentForm,"Expected Request body was empty.");
        Logger.debug(this, ()->"Saving a workflow comment for the contentletId: " + contentletId);

        final User user = initDataObject.getUser();
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode = PageMode.get(request);

        final Optional<Contentlet> currentContentlet =  languageId <= 0?
                this.workflowHelper.getContentletByIdentifier(contentletId, mode, initDataObject.getUser(),
                        ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId()):
                this.contentletAPI.findContentletByIdentifierOrFallback
                        (contentletId, mode.showLive, languageId, initDataObject.getUser(), mode.respectAnonPerms);
        if (currentContentlet.isPresent()) {

            final WorkflowTask task = this.workflowAPI.findTaskByContentlet(currentContentlet.get());
            final WorkflowComment taskComment = new WorkflowComment();
            taskComment.setComment(workflowCommentForm.getComment());
            taskComment.setCreationDate(new Date());
            taskComment.setPostedBy(user.getUserId());
            taskComment.setWorkflowtaskId(task.getId());
            this.workflowAPI.saveComment(taskComment);
            return new ResponseEntityWorkflowCommentView(
                    toWorkflowTimelineItemView(taskComment));
        }

        throw new DoesNotExistException("Contentlet with identifier " + contentletId + " does not exist.");
    }
} // E:O:F:WorkflowResource.
