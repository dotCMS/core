package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return "This actionlet will move the content to a new path.  If the path is not set on the actionlet, will pop up a message to select the new destiny";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final User user             = processor.getUser();
        final String pathParam      = params.get("path").getValue();
        final String path           = findFolderIdByPath(pathParam, contentlet);

        APILocator.getContentletAPI().move(contentlet, user, path);
    }


    private String findFolderIdByPath (final String actionletPathParameter, final Contentlet contentlet)  {

        return  UtilMethods.isSet(actionletPathParameter)?
                actionletPathParameter: contentlet.getStringProperty("_path_to_move");
    }
}
