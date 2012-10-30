package com.dotmarketing.portlets.checkurl.util;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseImpl;

/**
 * The abstract class for DispatcherService. It's implements the render and the processAction and delegate respectively to _render and to _processAction.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 13, 2012
 */
public abstract class AbstractDispatcherService extends DotPortletAction {
	
	protected final static String ACTION_METHOD_PARAM = "dot_action_method";
	protected final static String RENDER_METHOD_PARAM = "dot_render_method";
	protected static int databaseType = 0;
	
	static {
		_setDatabaseType();
	}
	
	@Override
	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {
		RenderRequestImpl reqImpl = (RenderRequestImpl)req;
		RenderResponseImpl resImpl = (RenderResponseImpl)res;		
		return _render(mapping, form, config, reqImpl, resImpl);
	}
		
	@Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
		ActionResponseImpl resImpl = (ActionResponseImpl)res;
		_processAction(mapping, form, config, reqImpl, resImpl);
	}

	private static void _setDatabaseType(){
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			databaseType = 1;
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			databaseType = 2;
		}		
		// TODO: must extends these IF statements for other database
	}
	
	protected boolean _isForPagination(RenderRequest req){
		if(req.getParameter("s")!= null && req.getParameter("e")!= null)
			return true;
		else
			return false;
	}
	
	protected void _setOutcome(ActionResponseImpl res, String outcome){
		if(null!=outcome){
			if(!res.getRenderParameters().containsKey(RENDER_METHOD_PARAM))
				res.setRenderParameter(RENDER_METHOD_PARAM, outcome);
		}
	}
	
	public abstract ActionForward _render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequestImpl reqImpl, RenderResponseImpl resImpl) throws Exception;
	public abstract void _processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequestImpl req, ActionResponseImpl res) throws Exception;
	
}
