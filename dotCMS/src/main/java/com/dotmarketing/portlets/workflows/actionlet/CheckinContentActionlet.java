package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;

public class CheckinContentActionlet extends WorkFlowActionlet {

  /** */
  private static final long serialVersionUID = 1L;

  public String getName() {
    return "Unlock content";
  }

  public String getHowTo() {

    return "This actionlet will checkin and unlock the content.";
  }

  public void executeAction(
      WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
      throws WorkflowActionFailureException {
    try {

      APILocator.getContentletAPI()
          .unlock(
              processor.getContentlet(),
              processor.getUser(),
              processor.getContentletDependencies() != null
                  && processor.getContentletDependencies().isRespectAnonymousPermissions());
    } catch (Exception e) {
      Logger.error(this.getClass(), e.getMessage(), e);
      throw new WorkflowActionFailureException(e.getMessage(), e);
    }
  }

  public WorkflowStep getNextStep() {

    return null;
  }

  @Override
  public List<WorkflowActionletParameter> getParameters() {

    return null;
  }
}
