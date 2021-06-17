package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This Actionlet allows to the user to move a contentlet to another place
 * @author jsanca
 */
public class MoveContentActionlet extends WorkFlowActionlet {


    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter("path", "Optional path to move, for example: //demo.dotcms.com/application", "", false));

        return params;
    }

    @Override
    public String getName() {
        return "Move";
    }

    @Override
    public String getHowTo() {
        return "If the path is not set on this actionlet, dotCMS will allow a user to select a destination";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final User user             = processor.getUser();
        final String pathParam      = params.get("path").getValue();
        final String path           = findFolderIdByPath(pathParam, contentlet);

        APILocator.getContentletAPI().move(contentlet, user, path, respectFrontendRoles);
    }


    private String findFolderIdByPath (final String actionletPathParameter, final Contentlet contentlet)  {

        return  UtilMethods.isSet(actionletPathParameter)?
                actionletPathParameter: contentlet.getStringProperty("_path_to_move");
    }
}
