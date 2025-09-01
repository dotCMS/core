
package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryState;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryType;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;

/**
 * Deletes the approvers of the workflow task
 * @author jsanca
 */
public class ResetApproversActionlet extends WorkFlowActionlet {

	private static final String ID_DELIMITER = ",";
	private static final String PARAM_CONTENT_ACTIONS = "action";

	private static ArrayList<WorkflowActionletParameter> ACTIONLET_PARAMETERS = null;

	@Override
	public synchronized List<WorkflowActionletParameter> getParameters() {
		if (null == ACTIONLET_PARAMETERS) {
			ACTIONLET_PARAMETERS = new ArrayList<>();
			ACTIONLET_PARAMETERS
					.add(new WorkflowActionletParameter(PARAM_CONTENT_ACTIONS,
							"Optional Action ID, or Name", null,
							false));
		}
		return ACTIONLET_PARAMETERS;
	}

	@Override
	public String getName() {
		return "Reset Approvals";
	}

	@Override
	public String getHowTo() {

		return "This actionlet will reset workflow history approvals";
	}

	private Optional<String> getWorkflowIdFromParameter (final WorkflowProcessor processor,
														 final WorkflowActionClassParameter parameter)  {

		final String value = getParameterValue(parameter);
		if (null != value) {

			if (!UUIDUtil.isUUID(value)) {

				final List<WorkflowAction> workflowActions;
				try {
					workflowActions = APILocator.getWorkflowAPI()
							.findActions(processor.getScheme(), processor.getUser());
				} catch (DotDataException | DotSecurityException e) {
					throw new WorkflowActionFailureException(e.getMessage(), e);
				}

				if (UtilMethods.isSet(workflowActions)) {

					final Optional<WorkflowAction> workflowAction =
							workflowActions.stream().filter(action -> action.getName().equalsIgnoreCase(value)).findFirst();
					if (workflowAction.isPresent()) {

						return Optional.ofNullable(workflowAction.get().getId());
					}
				}
			}
		}

		return Optional.ofNullable(value);
	}

	@Override
	public void executeAction(final WorkflowProcessor processor,
							  final Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {

		try {

			final Optional<String> actionIdOpt = this.getWorkflowIdFromParameter(processor, params.get(PARAM_CONTENT_ACTIONS));
			final List<WorkflowHistory> newHistories = new ArrayList<>();
			final List<WorkflowHistory> histories    = processor.getHistory();
			for (final WorkflowHistory history : histories) {

				final Map<String, Object> changeMap = history.getChangeMap();
				final Object type = changeMap.get("type");
				if (WorkflowHistoryType.APPROVAL.name().equals(type)) {

					// if wants to filter by action id
					if (actionIdOpt.isPresent() && !actionIdOpt.get().equals(history.actionId())) {

						newHistories.add(history);
						continue;
					}

					final String description = "{\"description\":\""+ changeMap.get("description") +
							"\", \"type\":\"" + WorkflowHistoryType.APPROVAL.name() +
							"\", \"state\":\""+  WorkflowHistoryState.RESET.name() +"\" }";
					history.setChangeDescription(description);
					APILocator.getWorkflowAPI().saveWorkflowHistory(history);
				}

				newHistories.add(history);
			}

			processor.setHistory(newHistories);
		} catch (DotDataException e) {
			Logger.error(ResetApproversActionlet.class,e.getMessage(),e);
		}
	}


}
