
package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Deletes the approvers of the workflow task
 * @author jsanca
 */
public class ResetApproversActionlet extends WorkFlowActionlet {

	@Override
	public String getName() {
		return "Reset Approvers";
	}

	@Override
	public String getHowTo() {

		return "This actionlet will reset workflow history approvers";
	}

	@Override
	public void executeAction(final WorkflowProcessor processor,
							  final Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {

		try {

			final List<WorkflowHistory> histories = processor.getHistory();
			for (final WorkflowHistory history : histories) {
				final Map<String, Object> changeMap = history.getChangeMap();
				final Object type = changeMap.get("type");
				if (WorkflowHistoryType.APPROVAL.name().equals(type)) {

					APILocator.getWorkflowAPI().deleteWorkflowHistory(history);
				}
			}
		} catch (DotDataException e) {
			Logger.error(ResetApproversActionlet.class,e.getMessage(),e);
		}
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
