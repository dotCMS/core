package com.dotcms.rendering.js;

import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dotcms.rendering.util.ActionletUtil.getRequest;
import static com.dotcms.rendering.util.ActionletUtil.getResponse;

/**
 * Js Script Actionlet allows to execute custom script in a workflow action
 * @author jsanca
 */
public class JsScriptActionlet  extends WorkFlowActionlet {

    private static final List<WorkflowActionletParameter> PARAMETER_LIST = createParamList();
    private boolean stop = false;



    private static List<WorkflowActionletParameter> createParamList () {

        final ImmutableList.Builder<WorkflowActionletParameter> paramList = new ImmutableList.Builder<>();

        paramList.add(new WorkflowActionletParameter
                ("javascriptCode", "JavaScript Code", null, false));
        paramList.add(new WorkflowActionletParameter
                ("resultKey", "Contentlet Result Property Name", "result", false));

        return paramList.build();
    }
    @Override
    public List<WorkflowActionletParameter> getParameters() {

        return PARAMETER_LIST;
    }

    @Override
    public String getName() {
        return "JavaScript Actionlet";
    }

    @Override
    public String getHowTo() {
        return "This actionlet give the ability to run a javascript as part of the workflow action." +
                " The Result Property Name is the name to store the result of the javascript execution in the contentlet, " +
                "will store the dotJson and the output (if empty won't add any result to the contentlet).";
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        try {
            final User currentUser              = processor.getUser();
            final HttpServletRequest  request   = getRequest(currentUser);
            final HttpServletResponse response  = getResponse();
            final WorkflowActionClassParameter javascriptCodeParameter = params.get("javascriptCode");
            final WorkflowActionClassParameter keyParameter    = params.get("resultKey");
            final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ScriptEngineFactory.JAVASCRIPT_ENGINE);
            final String javascriptCode = javascriptCodeParameter.getValue();
            final String resultKey      = keyParameter.getValue();
            final Object result         = engine.eval(request, response, new StringReader(javascriptCode),
                    new HashMap<>(Map.of("workflow", processor, "user", processor.getUser(),
                            "contentlet", processor.getContentlet(), "content", processor.getContentlet())));

            this.stop = processor.abort();
            if (Objects.nonNull(result) && Objects.nonNull(resultKey)) {
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
