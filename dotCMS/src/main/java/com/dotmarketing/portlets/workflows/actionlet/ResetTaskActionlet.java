
package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Actionlet(reset = true)
public class ResetTaskActionlet extends WorkFlowActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = -3399186955215452961L;


	@Override
	public String getName() {
		return "Reset Workflow";
	}

	@Override
	public String getHowTo() {

		return "This actionlet will reset workflow task state, but keeping the comments and history.  It will also STOP all further subaction processing";
	}

	@Override
	public void executeAction(final WorkflowProcessor processor,
							  final Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {

		final WorkflowTask task = processor.getTask();
		
		try {

			if (null != task) {
				task.setStatus(null);
				APILocator.getWorkflowAPI().saveWorkflowTask(task);
				processor.setTask(null);
			}

			final Contentlet contentlet = processor.getContentlet();
			if(null != contentlet && null != processor.getUser()){
				// this will mark  the contentlet that is being reset as recently updated. Product of this Reset Action
				final Set<String> inodes = Stream.of(contentlet).map(Contentlet::getInode).collect(Collectors.toSet());
				final int rows = APILocator.getContentletAPI().updateModDate(inodes, processor.getUser());
				Logger.debug(getClass(),()->String.format("%s rows updated by updateModDate. ", rows));
			} else {
				Logger.error(getClass(), "Unable to set modification date on the reset workflow.");
			}

			processor.setContentlet(null);
			processor.abortProcessor();
		} catch (DotDataException e) {
			Logger.error(ResetTaskActionlet.class,e.getMessage(),e);
		}
	}

	@Override
	public boolean stopProcessing() {

		return true;
	}

	
	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
