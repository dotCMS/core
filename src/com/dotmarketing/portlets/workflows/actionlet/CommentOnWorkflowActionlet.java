package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

public class CommentOnWorkflowActionlet extends WorkFlowActionlet {


	private static List<WorkflowActionletParameter> paramList = null; 

	@Override
	public synchronized List<WorkflowActionletParameter> getParameters() {
		if(paramList ==null){
			paramList = new ArrayList<WorkflowActionletParameter>();
			paramList.add(new WorkflowActionletParameter("comment", "Workflow Comment", null, true));
		}
		return paramList;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return "Comment on Workflow";
	}

	public String getHowTo() {

		return "This actionlet allows you to add a comment on the workflow task.";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		WorkflowActionClassParameter commentParam =  params.get("comment");
		WorkflowComment comment = new WorkflowComment();
		comment.setPostedBy(processor.getUser().getUserId());
		comment.setComment(commentParam.getValue());
		comment.setWorkflowtaskId(processor.getTask().getId());
		try {
			APILocator.getWorkflowAPI().saveComment(comment);
		} catch (DotDataException e) {
			Logger.error(CommentOnWorkflowActionlet.class,e.getMessage(),e);
		}

	}

	public WorkflowStep getNextStep() {
		// TODO Auto-generated method stub
		return null;
	}

}
