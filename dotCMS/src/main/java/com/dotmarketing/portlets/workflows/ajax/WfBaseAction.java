package com.dotmarketing.portlets.workflows.ajax;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;

import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

@Deprecated
abstract class WfBaseAction extends AjaxAction {

	protected static final String ACTION_ID_PARAM = "actionId";
	protected static final String ACTION_NAME_PARAM = "actionName";
	protected static final String SCHEME_ID_PARAM = "schemeId";
	protected static final String STEP_ID_PARAM = "stepId";
	protected static final String ACTION_ICON_SELECT_PARAM = "actionIconSelect";
	protected static final String ACTION_ASSIGNABLE_PARAM = "actionAssignable";
	protected static final String ACTION_COMMENTABLE_PARAM = "actionCommentable";
	protected static final String ACTION_ROLE_HIERARCHY_FOR_ASSIGN_PARAM = "actionRoleHierarchyForAssign";
	protected static final String ACTION_NEXT_STEP_PARAM = "actionNextStep";
	protected static final String ACTION_ASSIGN_TO_SELECT_PARAM = "actionAssignToSelect";
	protected static final String ACTION_CONDITION_PARAM = "actionCondition";
	protected static final String SHOW_ON_PARAM = "showOn";
	protected static final String WHO_CAN_USE_PARAM = "whoCanUse";
	protected static final String ORDER_PARAM = "order";
	
	protected abstract Set<String> getAllowedCommands();

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String cmd = request.getParameter("cmd");
		if (cmd == null) cmd = "action";
		java.lang.reflect.Method meth = null;
		Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };
		try {
            if (getUser() == null) {
                response.sendError(401);
                return;
			}

		  if(getAllowedCommands().contains(cmd)) {
			  meth = getMethod(cmd, partypes);
		  }	else  if (UtilMethods.isSet(cmd)){
			  Logger.error(this.getClass(), String.format("Attempt to run Invalid command %s :",cmd));
			  return;
		  }

		} catch (Exception e) {

			try {
				cmd = "action";
				meth = getMethod(cmd, partypes);
			} catch (Exception ex) {
				Logger.error(this.getClass(), "Trying to run method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				writeError(response, e.getCause().getMessage());
				return;
			}
		}
		try {
			meth.invoke(this, arglist);
		} catch (Exception e) {
			Logger.error(WfBaseAction.class, "Trying to run method:" + cmd);
			Logger.error(WfBaseAction.class, e.getMessage(), e.getCause());
			var causeMessage = Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage());
			Logger.error(WfBaseAction.class, causeMessage, e);
			writeError(response, causeMessage);
		}

	}

	/**
	 * extracted as a separated method, so it can be verified when called
	 * @param method
	 * @param parameterTypes
	 * @return
	 * @throws NoSuchMethodException
	 */
	@VisibleForTesting
	Method getMethod(String method,  Class<?>... parameterTypes ) throws NoSuchMethodException {
		Logger.debug(this, "Trying to run method:" + method + " with parameters:" + parameterTypes);
		return this.getClass().getMethod(method, parameterTypes);
	}

	public void writeError(HttpServletResponse response, String error) throws IOException {
		String ret = null;

		try {
			ret = LanguageUtil.get(getUser(), error);
		} catch (Exception e) {

		}
		if (ret == null) {
			try {
				ret = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(),
						error);
			} catch (Exception e) {

			}
		}
		if (ret == null) {
			ret = error;
		}
        response.setContentType("text/plain");
		response.getWriter().println("FAILURE: " + ret);
	}
	
	public void writeSuccess(HttpServletResponse response, String success) throws IOException {
		String ret = null;

		try {
			ret = LanguageUtil.get(getUser(), success);
		} catch (Exception e) {

		}
		if (ret == null) {
			try {
				ret = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(),
						success);
			} catch (Exception e) {

			}
		}
		if (ret == null) {
			ret = success;
		}
		response.setContentType("text/plain");
		response.getWriter().println("SUCCESS:" + success );
	}
}
