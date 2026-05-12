package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotcms.rendering.util.ActionletUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Velocity Script Actionlet allows to execute custom script in a workflow action
 * @author jsanca
 */
public class VelocityScriptActionlet extends WorkFlowActionlet {

    private final static String ENGINE = "Velocity";
    private static List<WorkflowActionletParameter> parameterList = createParamList();
    private boolean stop = false;

    private static List<WorkflowActionletParameter> createParamList () {

        final ImmutableList.Builder<WorkflowActionletParameter> paramList = new ImmutableList.Builder<>();

        paramList.add(new WorkflowActionletParameter
                ("script", "Script Code", null, false));
        paramList.add(new WorkflowActionletParameter
                ("resultKey", "Contentlet Result Property Name", "result", false));

        return paramList.build();
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        return parameterList;
    }

    @Override
    public String getName() {
        return "Velocity Script Actionlet";
    }

    @Override
    public String getHowTo() {

        return "This actionlet give the ability to run a velocity script as part of the workflow action." +
                " The Script Code allows to add the velocity, can include a vtl by #dotParse directive."  +
                " The Result Property Name is the name to store the result of the velocity execution in the contentlet, will store the dotJson and the output (if empty won't add any result to the contentlet).";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        try {
            final User  currentUser            = processor.getUser();
            final HttpServletResponse response = ActionletUtil.getResponse();
            final WorkflowActionClassParameter scriptParameter = params.get("script");
            final WorkflowActionClassParameter keyParameter    = params.get("resultKey");
            final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ENGINE);
            final String script       = scriptParameter.getValue();
            final String resultKey    = keyParameter.getValue();
            final Reader reader       = new StringReader(script);
            final Contentlet contentlet = processor.getContentlet();

            // Resolve the contentlet's site first so the mock request (background/Quartz threads)
            // carries the correct hostname. SecretTool snapshots this.request during init() —
            // before any VTL executes — making it immune to #set($host = ...) overrides.
            Host contentletHost = null;
            if (UtilMethods.isSet(contentlet.getHost())) {
                final Host found = APILocator.getHostAPI().find(
                        contentlet.getHost(), APILocator.systemUser(), false);
                if (null != found && UtilMethods.isSet(found.getIdentifier()) && !found.isArchived()) {
                    contentletHost = found;
                }
            }

            // Always build a mock request scoped to the contentlet's own site so that
            // SecretTool.this.request (snapshotted at init-time, before VTL executes) resolves
            // secrets for the contentlet's site — not the caller's browser host. This is correct
            // for both background jobs (no thread-local request) and HTTP-triggered workflows
            // (where the thread-local carries the browser's site, not the contentlet's site).
            final HttpServletRequest request = new MockAttributeRequest(new MockSessionRequest(
                    new FakeHttpRequest(
                            null != contentletHost
                                    ? contentletHost.getHostname()
                                    : APILocator.systemHost().getHostname(),
                            StringPool.FORWARD_SLASH).request()
              ).request()).request();

            // Propagate the workflow processor's user onto the mock request so viewtools that
            // resolve the user via PortalUtil.getUser(req) — e.g. WorkflowTool.init() — see the
            // actual triggering user instead of falling back to anonymous (issue #35347).
            if (null != currentUser) {
                request.setAttribute(WebKeys.USER, currentUser);
                final HttpSession session = request.getSession(false);
                if (null != session) {
                    session.setAttribute(WebKeys.USER, currentUser);
                }
            }

            final Map<String, Object> contextParams = new HashMap<>(Map.of("workflow", processor,
                    "user", currentUser,
                    "contentlet", contentlet,
                    "content", contentlet));
            if (null != contentletHost) {
                contextParams.put("host", contentletHost);
            }

            final Object result = engine.eval(request, response, reader, contextParams);

            this.stop = processor.abort();
            if (null != result && null != resultKey) {
                processor.getContentlet().setProperty(resultKey, result);
            }
        } catch (Exception e) {

            Logger.error(this, ExceptionUtil.getErrorMessage(e), e);
            throw new WorkflowActionFailureException(ExceptionUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public boolean stopProcessing() {
        return stop;
    }
}
