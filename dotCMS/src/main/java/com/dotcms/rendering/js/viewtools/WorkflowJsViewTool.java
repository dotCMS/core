package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsHttpServletRequestAware;
import com.dotcms.rendering.js.JsHttpServletResponseAware;
import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsContentMap;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
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

        try {

            final User user = WebAPILocator.getUserWebAPI().getUser(this.request);
            final PageMode pageMode = PageMode.get(this.request);
            final WorkflowAPI.SystemAction systemAction = WorkflowAPI.SystemAction.NEW;
            final Contentlet contentlet = this.contentHelper.populateContentletFromMap(new Contentlet(), contentletMap);
            final Optional<WorkflowAction> optWorkflowAction =
                    this.workflowAPI.findActionMappedBySystemActionContentlet(contentlet, systemAction, user);
            if (optWorkflowAction.isPresent()) {

                final WorkflowAction workflowAction = optWorkflowAction.get();
                final String actionId = workflowAction.getId();

                Logger.info(this, "Using the default action: " + workflowAction +
                        ", for the system action: " + systemAction);

                final ContentletDependencies.Builder formBuilder = new ContentletDependencies.Builder();
                formBuilder.workflowActionId(actionId).modUser(user);
                final MutableBoolean respectAnonPermissions = new MutableBoolean(false);

                if(workflowOptions != null) {

                    Optional.ofNullable(workflowOptions.get("respectAnonPerms")).ifPresent(respectAnonPerms -> {
                                respectAnonPermissions.setValue(ConversionUtils.toBoolean(workflowOptions.get("respectAnonPerms").toString(), false));
                                formBuilder.respectAnonymousPermissions(respectAnonPermissions.booleanValue());
                            }
                            );

                    processWorkflowOptions(workflowOptions, formBuilder);

                    // todo; implement this thing
                    //this.processPermissions(fireActionForm, formBuilder);
                }

                if (contentlet.getMap().containsKey(Contentlet.RELATIONSHIP_KEY)) {
                    formBuilder.relationships((ContentletRelationships) contentlet.getMap().get(Contentlet.RELATIONSHIP_KEY));
                }

                final Optional<List<Category>>categories = MapToContentletPopulator.
                        INSTANCE.fetchCategories(contentlet, user, respectAnonPermissions.booleanValue());

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
