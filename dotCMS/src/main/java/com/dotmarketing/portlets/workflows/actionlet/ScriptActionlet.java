package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.google.common.collect.ImmutableList;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * Script actionlet allows to execute custom script in a workflow action
 * @author jsanca
 */
public class ScriptActionlet extends WorkFlowActionlet {

    private static List<WorkflowActionletParameter> parameterList = createParamList();

    private static List<WorkflowActionletParameter> createParamList () {

        final ImmutableList.Builder<WorkflowActionletParameter> paramList = new ImmutableList.Builder<>();

        paramList.add(new WorkflowActionletParameter
                ("type", "Engine Type", "Velocity", true));
        paramList.add(new WorkflowActionletParameter
                ("file", "Script Path", null, false));
        paramList.add(new WorkflowActionletParameter
                ("script", "Script Code", null, false));
        paramList.add(new WorkflowActionletParameter
                ("resultKey", "Result key", null, true));

        return paramList.build();
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        return parameterList;
    }

    @Override
    public String getName() {
        return "Script Actionlet";
    }

    @Override
    public String getHowTo() {

        return "This actionlet give the ability to run a script as part of the workflow action.";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final WorkflowActionClassParameter typeParameter   = params.get("type");
        final String engineType   = typeParameter.getValue();
        final WorkflowActionClassParameter fileParameter   = params.get("file");
        final String file         = fileParameter.getValue();
        final WorkflowActionClassParameter scriptParameter = params.get("script");
        final String script       = scriptParameter.getValue();
        final WorkflowActionClassParameter keyParameter    = params.get("resultKey");
        final String resultKey    = keyParameter.getValue();
        final Reader reader       = this.createReader (file, script);
        final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(engineType);

        final Object result = engine.eval(null, null, reader,
                CollectionsUtils.map("workflow", processor,
                "user", processor.getUser(),
                "contentlet", processor.getContentlet(),
                "content", processor.getContentlet()));

        processor.getContentlet().setProperty(resultKey, result);
    }

    private Reader createReader(final String file, final String script) {

        // todo: implement file
        return new StringReader(script);
    }

}
