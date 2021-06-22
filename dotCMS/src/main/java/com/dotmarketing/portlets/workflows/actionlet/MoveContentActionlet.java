package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This Actionlet allows to the user to move a contentlet to another place
 * @author jsanca
 */
public class MoveContentActionlet extends WorkFlowActionlet {


    /**
     * This is the parameter for the Actionlet
     */
    public static final String PATH_KEY = "path";

    /**
     * This is the parameter if want to override the path from the contentlet map properties
     */
    public static final String CONTENTLET_PATH_KEY = "_path_to_move";

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
        final String pathParam      = params.get(PATH_KEY).getValue();
        final String path           = findFolderIdByPath(pathParam, contentlet);

        Logger.debug(this, "Moving the contentlet to: " + path);

        processor.setContentlet(Try.of(()->APILocator.getContentletAPI().move(contentlet, user, path, false))
                .getOrElseThrow(e -> new WorkflowActionFailureException(e.getMessage(), (Exception) e)));
    }


    private String findFolderIdByPath (final String actionletPathParameter, final Contentlet contentlet)  {

        return  UtilMethods.isSet(actionletPathParameter)?
                actionletPathParameter: contentlet.getStringProperty("_path_to_move");
    }
}
