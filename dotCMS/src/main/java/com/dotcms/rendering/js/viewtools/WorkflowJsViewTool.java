package com.dotcms.rendering.js.viewtools;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.js.JsHttpServletRequestAware;
import com.dotcms.rendering.js.JsHttpServletResponseAware;
import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsContentMap;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements the interface with javascript context to interact with the workflow API
 */
public class WorkflowJsViewTool implements JsViewTool, JsHttpServletRequestAware, JsHttpServletResponseAware, JsViewContextAware {

    private final ContentHelper contentHelper = ContentHelper.getInstance();
    private final WorkflowAPI  workflowAPI    = APILocator.getWorkflowAPI();

    private ViewContext viewContext = null;
    private HttpServletRequest request = null;
    private HttpServletResponse response = null;

    @Override
    public String getName() {
        return "workflows";
    }

    @Override
    public SCOPE getScope() {
        return SCOPE.REQUEST;
    }

    @Override
    public void setRequest(final HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void setResponse(final HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void setViewContext(ViewContext viewContext) {

        this.viewContext = viewContext;
    }

    @HostAccess.Export
    public JsContentMap fireNew(final Map contentletMap,
                                final Map workflowOptions) {

        if (null == contentletMap || contentletMap.isEmpty() || !contentletMap.containsKey("contentType")) {

            throw new IllegalArgumentException("contentType attribute is required on the contentMap");
        }

        final WorkflowAPI.SystemAction systemAction = WorkflowAPI.SystemAction.NEW;
        final User user = WebAPILocator.getUserWebAPI().getUser(this.request);

        try {


            final String contentTypeVarName = contentletMap.get("contentType").toString();
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(contentTypeVarName);
            final WorkflowAction workflowAction = this.findWorkflowActionMapped(systemAction, contentType, user);

            if (null == workflowAction) {

                throw new IllegalArgumentException("Could not find a NEW system action for the Contentlet with type: "
                        + contentTypeVarName);
            }

            return this.fireInternal(contentletMap, workflowAction, workflowOptions, new Contentlet());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @HostAccess.Export
    public JsContentMap fireEdit(final Map contentletMap,
                                final Map workflowOptions) {

        if (null == contentletMap || contentletMap.isEmpty() || !contentletMap.containsKey("identifier")) {

            throw new IllegalArgumentException("Contentlet map is required or identifier");
        }

        if (null == contentletMap || contentletMap.isEmpty() || !contentletMap.containsKey("contentType")) {

            throw new IllegalArgumentException("contentType attribute is required on the contentMap");
        }

        final WorkflowAPI.SystemAction systemAction = WorkflowAPI.SystemAction.EDIT;
        final User user = WebAPILocator.getUserWebAPI().getUser(this.request);

        try {

            final String contentTypeVarName = contentletMap.get("contentType").toString();
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(contentTypeVarName);
            final WorkflowAction workflowAction = this.findWorkflowActionMapped(systemAction, contentType, user);

            if (null == workflowAction) {

                throw new IllegalArgumentException("Could not find a EDIT system action for the Contentlet with type: "
                        + contentTypeVarName);
            }

            final Contentlet existingContentlet = this.findById(contentletMap.get("identifier").toString());

            return this.fireInternal(contentletMap, workflowAction, workflowOptions, existingContentlet);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private WorkflowAction findWorkflowActionMapped (final WorkflowAPI.SystemAction systemAction,
                                                     final ContentType contentType,
                                                     final User user) throws DotDataException, DotSecurityException {

        WorkflowAction workflowAction = null;
        Optional<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappingOpt =
                this.workflowAPI.findSystemActionByContentType(systemAction, contentType, user);
        if (systemActionWorkflowActionMappingOpt.isPresent()) {
            workflowAction = systemActionWorkflowActionMappingOpt.get().getWorkflowAction();
        } else {
            final List<WorkflowScheme> workflowSchemes = workflowAPI.findSchemesForContentType(contentType);
            if (null != workflowSchemes) {
                for (final WorkflowScheme workflowScheme : workflowSchemes) {
                    systemActionWorkflowActionMappingOpt =
                            this.workflowAPI.findSystemActionByScheme(systemAction, workflowScheme, user);
                    if (systemActionWorkflowActionMappingOpt.isPresent()) {
                        workflowAction = systemActionWorkflowActionMappingOpt.get().getWorkflowAction();
                        break;
                    }
                }
            }
        }

        return workflowAction;
    }

    private Contentlet findById(final String identifier) {

        Logger.debug(this, ()-> "Fire Action, looking for content by identifier: " + identifier);

        final Contentlet currentContentlet =
                Try.of(()->APILocator.getContentletAPI().findContentletByIdentifierAnyLanguageAnyVariant(identifier)).getOrNull();

        DotPreconditions.notNull(currentContentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);

        return currentContentlet;
    }

    protected JsContentMap fireInternal(final Map contentletMap,
                                        final WorkflowAction workflowAction,
                                        final Map workflowOptions,
                                        final Contentlet contentletIn) {

        try {

            final User user = WebAPILocator.getUserWebAPI().getUser(this.request);
            final PageMode pageMode = PageMode.get(this.request);
            final Contentlet contentlet = this.contentHelper.populateContentletFromMap(contentletIn, contentletMap);
            if (null != workflowAction) {

                final String actionId = workflowAction.getId();

                Logger.info(this, "Using the default action: " + workflowAction);

                final boolean respectAnonPermissions = pageMode.respectAnonPerms;
                final ContentletDependencies.Builder formBuilder = new ContentletDependencies.Builder();
                formBuilder.workflowActionId(actionId).modUser(user);
                formBuilder.respectAnonymousPermissions(respectAnonPermissions);
                if(workflowOptions != null) {

                    processWorkflowOptions(workflowOptions, formBuilder);

                    // todo; implement this thing
                    //this.processPermissions(fireActionForm, formBuilder);
                }

                if (contentlet.getMap().containsKey(Contentlet.RELATIONSHIP_KEY)) {
                    formBuilder.relationships((ContentletRelationships) contentlet.getMap().get(Contentlet.RELATIONSHIP_KEY));
                }

                final Optional<List<Category>>categories = MapToContentletPopulator.
                        INSTANCE.fetchCategories(contentlet, user, respectAnonPermissions);

                //Empty collection implies removal, so only when a value is present we must pass the collection
                categories.ifPresent(formBuilder::categories);

                final Contentlet contentletOut = this.workflowAPI.fireContentWorkflow(contentlet, formBuilder.build());
                final Host host =  Try
                        .of(() -> Host.SYSTEM_HOST.equals(contentletOut.getHost()) || null == contentletOut.getHost()
                                ? APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)
                                : APILocator.getHostAPI().find(contentletOut.getHost(), APILocator.systemUser(), false))
                        .getOrElse(APILocator.systemHost());

                final ContentMap contentMap = new ContentMap(contentletOut, user, pageMode, host, viewContext.getVelocityContext());
                return new JsContentMap(contentMap);
            }

            throw new DotRuntimeException("No NEW System Workflow Action configurated for contentlet, type: "
                    + contentletMap.getOrDefault("contentType", "unknown"));
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static void processWorkflowOptions(Map workflowOptions, ContentletDependencies.Builder formBuilder) {
        Optional.ofNullable(workflowOptions.get("comments")).ifPresent(comments ->
                formBuilder.workflowActionComments(comments.toString()));

        Optional.ofNullable(workflowOptions.get("assignKey")).ifPresent(assignKey ->
                formBuilder.workflowAssignKey(assignKey.toString()));

        Optional.ofNullable(workflowOptions.get("publishDate")).ifPresent(publishDate ->
                formBuilder.workflowPublishDate(publishDate.toString()));

        Optional.ofNullable(workflowOptions.get("publishTime")).ifPresent(publishTime ->
                formBuilder.workflowPublishTime(publishTime.toString()));

        Optional.ofNullable(workflowOptions.get("timezoneId")).ifPresent(timezoneId ->
                formBuilder.workflowTimezoneId(timezoneId.toString()));

        Optional.ofNullable(workflowOptions.get("expireDate")).ifPresent(expireDate ->
                formBuilder.workflowExpireDate(expireDate.toString()));

        Optional.ofNullable(workflowOptions.get("expireTime")).ifPresent(expireTime ->
                formBuilder.workflowExpireTime(expireTime.toString()));

        Optional.ofNullable(workflowOptions.get("neverExpire")).ifPresent(neverExpire ->
                formBuilder.workflowNeverExpire(neverExpire.toString()));

        Optional.ofNullable(workflowOptions.get("whereToSend")).ifPresent(whereToSend ->
                formBuilder.workflowWhereToSend(whereToSend.toString()));

        Optional.ofNullable(workflowOptions.get("iWantTo")).ifPresent(iWantTo ->
                formBuilder.workflowIWantTo(iWantTo.toString()));

        Optional.ofNullable(workflowOptions.get("pathToMove")).ifPresent(pathToMove ->
                formBuilder.workflowPathToMove(pathToMove.toString()));
    }
}
