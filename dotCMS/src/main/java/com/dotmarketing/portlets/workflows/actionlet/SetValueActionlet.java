package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SetValueActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();

        params.add(new WorkflowActionletParameter("field", "velocityVar Name of the field to set", "", true));
        params.add(new WorkflowActionletParameter("value", "The value you want to set it to - can be velocity", "", true));

        return params;
    }

    @Override
    public String getName() {
        return "Set Value";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will set the value of a field.  The value param can be a string, or it can be velocity, where whatever is set as $value will be applied";
    }

    public void executePreAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final String field = params.get("field").getValue();
        final String value = params.get("value").getValue();
        final Field finalField = contentlet.getStructure().getFieldVar(field);
        Object finalValue = value;

        if (null == finalField) {

            throw new WorkflowActionFailureException("Set Value ActionLet: The field: " + field
                    + " does not exists on: " + contentlet.getContentType().variable());
        }

        if (UtilMethods.isSet(value) && (value.contains("$"))) {

            try {
                // get the host of the content
                Host host = APILocator.getHostAPI().find(processor.getContentlet().getHost(),
                        APILocator.getUserAPI().getSystemUser(), false);
                if (host.isSystemHost()) {
                    host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
                }

                HttpServletRequest requestProxy = new MockHttpRequest(host.getHostname(), null).request();
                HttpServletResponse responseProxy = new BaseResponse().response();

                final String workflowTaskTitle;
                final Date modDate;
                if (null != processor.getTask()) {
                    workflowTaskTitle =
                            UtilMethods.isSet(processor.getTask().getTitle()) ? processor.getTask()
                                    .getTitle() : processor.getContentlet().getTitle();

                    modDate = processor.getTask().getModDate();
                } else {
                    workflowTaskTitle = processor.getContentlet().getTitle();
                    modDate = processor.getContentlet().getModDate();
                }

                org.apache.velocity.context.Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
                ctx.put("host", host);
                ctx.put("host_id", host.getIdentifier());
                ctx.put("user", processor.getUser());
                ctx.put("workflow", processor);
                ctx.put("stepName", processor.getStep().getName());
                ctx.put("stepId", processor.getStep().getId());
                ctx.put("nextAssign", processor.getNextAssign().getName());
                ctx.put("workflowMessage", processor.getWorkflowMessage());
                ctx.put("nextStepResolved", processor.getNextStep().isResolved());
                ctx.put("nextStepId", processor.getNextStep().getId());
                ctx.put("nextStepName", processor.getNextStep().getName());
                ctx.put("workflowTaskTitle", workflowTaskTitle);
                ctx.put("modDate", modDate);
                ctx.put("structureName", processor.getContentlet().getStructure().getName());

                ctx.put("contentlet", contentlet);
                ctx.put("content", contentlet);

                VelocityUtil.eval(value, ctx);
                finalValue = ctx.get("value");

            } catch (Exception e) {
                throw new WorkflowActionFailureException(e.getMessage(),e);
            }

        }

        APILocator.getContentletAPI().setContentletProperty(contentlet, finalField, finalValue);
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {


    }

}