package com.dotmarketing.portlets.workflows.action;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.struts.WorkflowTaskForm;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
/**
 * @author David
 */

public class EditWorkflowTaskAction extends DotPortletAction {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private WorkflowAPI wfAPI = APILocator.getWorkflowAPI();
	
	public void processAction(ActionMapping mapping, ActionForm form,
			PortletConfig config, ActionRequest req, ActionResponse res)
	throws Exception {
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");
		HttpServletRequest request =  ((ActionRequestImpl)req).getHttpServletRequest();
		Logger.debug(this, "EditWorkflowTaskAction cmd=" + cmd);

		HibernateUtil.startTransaction();

		User user = _getUser(req);

		try {
			Logger.debug(this, "Calling Retrieve method");
			_retrieveWorkflowTask(req, WebKeys.WORKFLOW_TASK_EDIT);
		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}

	
		/*
		 * We are viewing the workflow task
		 */
		if ((cmd != null) && cmd.equals(Constants.VIEW)) {
			try {
				Logger.debug(this, "Calling View Method");
				_viewWorkflowTask(req, res, config, form, user);
				setForward(req, "portlet.ext.workflows.view_workflow_task");
				return;
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			} 
		}

		if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.ADD_COMMENT)) {
			try {
				Logger.debug(this, "Calling Add Comment Method");
				_addWorkflowComment(req, res, config, form, user);
				_sendToReferral(req, res, referer);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		} else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.ADD_FILE)) {
			try {
				Logger.debug(this, "Calling Add File Method");
				_addFileToWorkflow(req, res, config, form, user);
				_sendToReferral(req, res, referer);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		} else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.REMOVE_FILE)) {
			try {
				Logger.debug(this, "Calling Remove File Method");
				_removeFileToWorkflow(req, res, config, form, user);
				_sendToReferral(req, res, referer);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		} 
	

		Logger.debug(this, "Unspecified Action");

		HibernateUtil.commitTransaction();

		setForward(req, "portlet.ext.workflows.edit_workflow_task");
	}

	///// ************** ALL METHODS HERE *************************** ////////



	



	private void _viewWorkflowTask(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		WorkflowTask task = (WorkflowTask) req.getAttribute(WebKeys.WORKFLOW_TASK_EDIT);
		WorkflowTaskForm taskform = (WorkflowTaskForm) form;
		BeanUtils.copyProperties(taskform, task);
		if (task.getDueDate() != null) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(task.getDueDate());
			taskform.setDueDateDay(String.valueOf(cal.get(Calendar.DATE)));
			taskform.setDueDateMonth(String.valueOf(cal.get(Calendar.MONTH)));
			taskform.setDueDateYear(String.valueOf(cal.get(Calendar.YEAR)));
		}
	}



	private void _addWorkflowComment(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		WorkflowTask task = (WorkflowTask) req.getAttribute(WebKeys.WORKFLOW_TASK_EDIT);
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		ActionResponseImpl resImpl  = (ActionResponseImpl) res;
		HttpServletRequest httpReq= reqImpl.getHttpServletRequest();
		HttpServletResponse httpRes= resImpl.getHttpServletResponse();
		String comment = req.getParameter ("comment");
		WorkflowComment taskComment = new WorkflowComment ();
		taskComment.setComment(comment);
		taskComment.setCreationDate(new Date());
		taskComment.setPostedBy(user.getUserId());
		taskComment.setWorkflowtaskId(task.getId());
		wfAPI.saveComment(taskComment);

		String changeDesc = LanguageUtil.get(user, "edit_worflow.history.comment.added") + comment;
		_logWorkflowTaskHistory(task, user, changeDesc);


	}

	private void _addFileToWorkflow(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		WorkflowTask task = (WorkflowTask) req.getAttribute(WebKeys.WORKFLOW_TASK_EDIT);
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		ActionResponseImpl resImpl  = (ActionResponseImpl) res;
		HttpServletRequest httpReq= reqImpl.getHttpServletRequest();
		HttpServletResponse httpRes= resImpl.getHttpServletResponse();
		String fileToAttachInode = req.getParameter ("file_inode");
		String title = null;
		try{
			title = APILocator.getContentletAPI().find(fileToAttachInode, user, false).getTitle();
			
		}
		catch(Exception e){
			try{
				title = APILocator.getFileAPI().find(fileToAttachInode,user, false).getTitle();
			}
			catch(Exception es){
				
			}
			
		}
		
		if(title!=null){

			wfAPI.attachFileToTask(task, fileToAttachInode);
			String changeDesc = LanguageUtil.get(user, "edit_worflow.history.file.added")+": " + title;
			_logWorkflowTaskHistory(task, user, changeDesc);

		}

	

	}

	private void _removeFileToWorkflow(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		WorkflowTask task = (WorkflowTask) req.getAttribute(WebKeys.WORKFLOW_TASK_EDIT);
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		ActionResponseImpl resImpl  = (ActionResponseImpl) res;
		HttpServletRequest httpReq= reqImpl.getHttpServletRequest();
		HttpServletResponse httpRes= resImpl.getHttpServletResponse();
		String fileToAttachInode = req.getParameter ("file_inode");
		String title = null;
		try{
			title = APILocator.getContentletAPI().find(fileToAttachInode, user, false).getTitle();
			
		}
		catch(Exception e){
			try{
				title = APILocator.getFileAPI().find(fileToAttachInode,user, false).getTitle();
			}
			catch(Exception es){
				
			}
			
		}
		
		if(title!=null){
			wfAPI.removeAttachedFile(task, fileToAttachInode);
			String changeDesc = LanguageUtil.get(user, "edit_worflow.history.file.removed")+": " + title;
			_logWorkflowTaskHistory(task, user, changeDesc);

		}

	}



	private void _retrieveWorkflowTask(ActionRequest req, String webkey) throws Exception {
		WorkflowTask webAsset = (WorkflowTask) wfAPI.findTaskById(req.getParameter("inode"));
		req.setAttribute(webkey, webAsset);
	}

	private void _logWorkflowTaskHistory (WorkflowTask task, User user, String history) throws DotDataException {
		WorkflowHistory hist = new WorkflowHistory ();
		hist.setChangeDescription(history);
		hist.setCreationDate(new Date ());
		hist.setMadeBy(user.getUserId());
	    hist.setWorkflowtaskId(task.getId());
		wfAPI.saveWorkflowHistory(hist);
	}
	

}