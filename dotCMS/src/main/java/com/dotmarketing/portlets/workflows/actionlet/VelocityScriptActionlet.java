package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotcms.rendering.util.ActionletUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
            final HttpServletRequest request   = ActionletUtil.getRequest(currentUser);
            final HttpServletResponse response = ActionletUtil.getResponse();
            final WorkflowActionClassParameter scriptParameter = params.get("script");
            final WorkflowActionClassParameter keyParameter    = params.get("resultKey");
            final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ENGINE);
            final String script       = scriptParameter.getValue();
            final String resultKey    = keyParameter.getValue();
            final Reader reader       = new StringReader(script);
            final Object result       = engine.eval(request, response, reader,
                    new HashMap<>(Map.of("workflow", processor,
                            "user", processor.getUser(),
                            "contentlet", processor.getContentlet(),
                            "content", processor.getContentlet())));

            this.stop = processor.abort();
            if (null != result && null != resultKey) {
                processor.getContentlet().setProperty(resultKey, result);
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }
    }

    @Override
    public boolean stopProcessing() {
        return stop;
    }
}
