/*
 * Created on Jun 25, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.dotmarketing.portal.struts;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;

import com.liferay.portal.model.User;

/**
 * @author Maria
 *
 */
public interface DotPortletActionInterface {
	
	//the get version back web asset method is different for each asset so it needs to be implemented
	public abstract void _getVersionBackWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

	//the copy web asset method is different for each asset so it needs to be implemented
	public abstract void _copyWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

	//the save web asset method is different for each asset so it needs to be implemented
	public abstract void _saveWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

	//This method is implemented in part on the abstract class, still needs to be finished on each asset class, so it needs to stay on the interface
	public abstract void _editWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;


	/*
	*  All these methods are implemented in the Abstract class DotPortletAction. These are all common to the all WebAssets
	*/
	
//	public abstract void _moveWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

//	public abstract void _publishWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

//	public abstract void _getVersionsWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

//	public abstract void _unLockWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

//	public abstract void _deleteWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

//	public abstract void _undeleteWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;
	
//	public abstract void _deleteVersionWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;

//	public abstract void _unPublishWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;
		
//  public abstract void _retrieveWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user) throws Exception;
}
