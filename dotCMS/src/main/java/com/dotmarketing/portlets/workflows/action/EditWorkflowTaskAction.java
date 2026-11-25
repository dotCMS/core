package com.dotmarketing.portlets.workflows.action;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.struts.WorkflowTaskForm;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.beanutils.BeanUtils;
/**
 * @author David
 */

public class EditWorkflowTaskAction extends DotPortletAction {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private WorkflowAPI wfAPI = APILocator.getWorkflowAPI();
	
	/**
	 * Handles the commands of the legacy workflow task screen: viewing a task, commenting on it,
	 * and attaching or removing a file.
	 *
	 * <p>The task is named by the {@code inode} request parameter, which any caller can set, so
	 * the commands that change the task are allowed only for a {@link User} who may read the
	 * contentlet that task belongs to — or edit it, for removing an attachment. A refused caller
	 * is rolled back and sent to the error page without the task being touched.</p>
	 *
	 * <p>{@code view} is checked the same way, before the screen is forwarded to. It names its
	 * task with {@code taskId} rather than {@code inode}, which is the parameter the task dialog
	 * sends, so the task it is checked against is read from there.</p>
	 *
	 * @param mapping the Struts mapping for this action
	 * @param form    the {@link WorkflowTaskForm} bound to the request
	 * @param config  the configuration of the portlet the request came through
	 * @param req     the Struts wrapper for the HTTP request
	 * @param res     the Struts wrapper for the HTTP response
	 * @throws Exception if the command cannot be carried out
	 */
	public void processAction(ActionMapping mapping, ActionForm form,
							  PortletConfig config, ActionRequest req, ActionResponse res)
	throws Exception {
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
				final WorkflowTask taskToView = _resolveTaskToView(req);
				if (!WorkflowTaskAccess.canAccess(taskToView, user)) {
					_handleException(new DotSecurityException(String.format(
							"User '%s' is not allowed to view this workflow task",
							null != user ? user.getUserId() : "<none>")), req, false);
					return;
				}
				req.setAttribute(WebKeys.WORKFLOW_TASK_EDIT, taskToView);
				Logger.debug(this, "Calling View Method");
				_viewWorkflowTask(req, res, config, form, user);
				setForward(req, "portlet.ext.workflows.view_workflow_task");
				return;
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		}

		final WorkflowTask requestedTask =
				(WorkflowTask) req.getAttribute(WebKeys.WORKFLOW_TASK_EDIT);
		if (isTaskWriteCommand(cmd) && !isAllowedOnTask(cmd, requestedTask, user)) {
			_handleException(new DotSecurityException(String.format(
					"User '%s' is not allowed to run '%s' on this workflow task",
					null != user ? user.getUserId() : "<none>", cmd)), req, false);
			return;
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

		HibernateUtil.closeAndCommitTransaction();

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



	}

	private void _addFileToWorkflow(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		WorkflowTask task = (WorkflowTask) req.getAttribute(WebKeys.WORKFLOW_TASK_EDIT);
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		ActionResponseImpl resImpl  = (ActionResponseImpl) res;
		String fileToAttachInode = req.getParameter ("file_inode");
		String title = null;
		try{
			title = APILocator.getContentletAPI().find(fileToAttachInode, user, false).getTitle();
			
		}
		catch(Exception e){
			
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
		String fileToAttachInode = req.getParameter ("file_inode");
		String title = null;
		try{
			title = APILocator.getContentletAPI().find(fileToAttachInode, user, false).getTitle();
			
		}
		catch(Exception e){
			
		}
		
		if(title!=null){
			wfAPI.removeAttachedFile(task, fileToAttachInode);
			String changeDesc = LanguageUtil.get(user, "edit_worflow.history.file.removed")+": " + title;
			_logWorkflowTaskHistory(task, user, changeDesc);

		}

	}



	/**
	 * Tells whether the given command changes the workflow task, as opposed to only rendering it.
	 * These are the commands that need the caller checked against the task's contentlet.
	 *
	 * @param cmd the value of the request's {@code cmd} parameter, possibly {@code null}
	 * @return {@code true} when the command writes to the task
	 */
	private static boolean isTaskWriteCommand(final String cmd) {
		return com.dotmarketing.util.Constants.ADD_COMMENT.equals(cmd)
				|| com.dotmarketing.util.Constants.ADD_FILE.equals(cmd)
				|| com.dotmarketing.util.Constants.REMOVE_FILE.equals(cmd);
	}

	/**
	 * Tells whether the caller may run the given write command on the given task.
	 *
	 * <p>Commenting and attaching ask for READ on the task's contentlet, which is what the screen
	 * offers today and what the REST comments endpoint requires. Removing an attachment asks for
	 * EDIT instead: it destroys work someone else put there, and no endpoint sets a lower bar for
	 * it.</p>
	 *
	 * @param cmd  the command being run, already known to be a write command
	 * @param task the {@link WorkflowTask} named by the request, possibly {@code null}
	 * @param user the {@link User} making the request, possibly {@code null}
	 * @return {@code true} when the command may go ahead
	 */
	private static boolean isAllowedOnTask(final String cmd, final WorkflowTask task,
			final User user) {
		return com.dotmarketing.util.Constants.REMOVE_FILE.equals(cmd)
				? WorkflowTaskAccess.canEdit(task, user)
				: WorkflowTaskAccess.canAccess(task, user);
	}

	/**
	 * Reads the task the screen is being asked for.
	 *
	 * <p>The task dialog of the back-end names the task with {@code taskId} and sends no
	 * {@code inode}, which is the parameter the commands that write to a task use. Both are read
	 * here, {@code taskId} first, so that the task authorized against is the one the screen will
	 * go on to render.</p>
	 *
	 * @param req the Struts wrapper for the HTTP request
	 * @return the {@link WorkflowTask} the request names. A request that names none, or names one
	 *         that does not exist, yields an empty task, which no user is allowed to open
	 * @throws DotDataException if the task cannot be read
	 */
	private WorkflowTask _resolveTaskToView(final ActionRequest req) throws DotDataException {
		final String taskId = UtilMethods.isSet(req.getParameter("taskId"))
				? req.getParameter("taskId")
				: req.getParameter("inode");
		return wfAPI.findTaskById(taskId);
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