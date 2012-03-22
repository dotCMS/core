package com.dotmarketing.portlets.templates.action;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portal.struts.DotPortletActionInterface;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.factories.TemplateFactory;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.struts.TemplateForm;
import com.dotmarketing.services.TemplateServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.StringUtil;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author Maria
 */

public class EditTemplateAction extends DotPortletAction implements
	DotPortletActionInterface {

	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	private static ThreadLocal<Perl5Matcher> localP5Matcher = new ThreadLocal<Perl5Matcher>(){
		protected Perl5Matcher initialValue() {
			return new Perl5Matcher();
		}
	};

	private static org.apache.oro.text.regex.Pattern parseContainerPattern;
	private static org.apache.oro.text.regex.Pattern oldContainerPattern;

	public EditTemplateAction() {
		Perl5Compiler c = new Perl5Compiler();
    	try{
	    	parseContainerPattern = c.compile("#parse\\( \\$container.* \\)",Perl5Compiler.READ_ONLY_MASK);
	    	oldContainerPattern = c.compile("[0-9]+",Perl5Compiler.READ_ONLY_MASK);
    	}catch (MalformedPatternException mfe) {
    		Logger.fatal(this,"Unable to instaniate dotCMS Velocity Cache",mfe);
			Logger.error(this,mfe.getMessage(),mfe);
		}
	}

	public void processAction(ActionMapping mapping, ActionForm form,
			PortletConfig config, ActionRequest req, ActionResponse res)
	throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		if ((referer != null) && (referer.length() != 0)) {
			referer = URLDecoder.decode(referer, "UTF-8");
		}

		Logger.debug(this, "EditTemplateAction cmd=" + cmd);

		if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.TEMPLATE_ADD_CONTAINER)) {
			Logger.debug(this, "I'm popping up the Template selector");
			setForward(req, "portlet.ext.templates.container_selector");
			return;
		}

		HibernateUtil.startTransaction();

		User user = _getUser(req);

		try {
			Logger.debug(this, "Calling Retrieve method");
			_retrieveWebAsset(req, res, config, form, user, Template.class,
					WebKeys.TEMPLATE_EDIT);

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}

		/*
		 * We are editing the Template
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			try {
				Logger.debug(this, "Calling Edit method");
				_editWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				if ((referer != null) && (referer.length() != 0)) {
					if (ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {
						//The web asset edit threw an exception because it's
						// locked so it should redirect back with message
						java.util.Map<String,String[]> params = new java.util.HashMap<String,String[]>();
						params.put("struts_action",new String[] { "/ext/director/direct" });
						params.put("cmd", new String[] { "editTemplate" });
						params.put("template", new String[] { req.getParameter("inode") });
						params.put("referer", new String[] { URLEncoder.encode(referer, "UTF-8") });

						String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq, WindowState.MAXIMIZED.toString(), params);

						_sendToReferral(req, res, directorURL);
						return;
					}
				}
				_handleException(ae, req);
			}
		}

		/*
		 * If we are updating the Template, copy the information
		 * from the struts bean to the hbm inode and run the
		 * update action and return to the list
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {

				if (Validator.validate(req, form, mapping)) {

					Logger.debug(this, "Calling Save method");
					_saveWebAsset(req, res, config, form, user);

					String subcmd = req.getParameter("subcmd");

					if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
						Logger.debug(this, "Calling Publish method");
						_publishWebAsset(req, res, config, form, user,
								WebKeys.TEMPLATE_FORM_EDIT);



					if(!UtilMethods.isSet(referer)) {
						java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
						params.put("struts_action",new String[] {"/ext/templates/view_templates"});
						referer = PortletURLUtil.getActionURL(req,WindowState.MAXIMIZED.toString(),params);
					}



				}

                    try{


                        _sendToReferral(req, res, referer);
                        return;
                    }
                    catch(Exception e){
                        java.util.Map<String,String[]> params = new java.util.HashMap<String,String[]>();
                        params.put("struts_action",new String[] { "/ext/templates/view_templates" });
                        String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq, WindowState.MAXIMIZED.toString(), params);
                        _sendToReferral(req, res, directorURL);
                        return;

                    }


				}
			} catch (Exception ae) {
				_handleException(ae, req);
			}

		}
		/*
		 * If we are deleteing the Template,
		 * run the delete action and return to the list
		 *
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				Logger.debug(this, "Calling Delete method");
				_deleteWebAsset(req, res, config, form, user,
						WebKeys.TEMPLATE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_DELETE))
		{
			try
			{
				Logger.debug(this,"Calling Full Delete Method");
				WebAsset webAsset = (WebAsset) req.getAttribute(WebKeys.TEMPLATE_EDIT);
				if(WebAssetFactory.deleteAsset(webAsset,user)) {
					SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".full_delete");
				} else {
					SessionMessages.add(httpReq, "error", "message." + webAsset.getType() + ".full_delete.error");
				}
			}
			catch(Exception ae)
			{
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_DELETE_LIST))
		{
			try
			{
				Logger.debug(this,"Calling Full Delete Method");
				String [] inodes = req.getParameterValues("publishInode");
				StringBuilder dependencies = new StringBuilder();
//				boolean returnValue = true;

				for(String inode  : inodes)	{
					String result = APILocator.getTemplateAPI().checkDependencies(inode, user, false);

					WebAsset webAsset = (WebAsset) InodeFactory.getInode(inode,Template.class);

					if(UtilMethods.isSet(result)) {
						dependencies.append(LanguageUtil.get(user, "template-name")).append(": ").append(webAsset.getFriendlyName()).append("\n");
						dependencies.append(LanguageUtil.get(user, "Pages-URLs")).append(": ").append(result);
					} else {
//						returnValue &= WebAssetFactory.deleteAsset(webAsset,user);
						WebAssetFactory.deleteAsset(webAsset,user);
					}

					dependencies.append("\n");
				}

				SessionMessages.add(httpReq,"error","message.template.full_delete.error");
//				if(returnValue)
//				{
//					SessionMessages.add(httpReq,"message","message.template.full_delete");
//				}
//				else
//				{
//					SessionMessages.add(httpReq,"error","message.template.full_delete.error");
//					Logger.debug(this," Template cannot be deleted if it has existing relationships");
//				}
			}
			catch(Exception ae)
			{
				SessionMessages.add(httpReq,"error","message.template.full_delete.error");
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are undeleting the Template,
		 * run the undelete action and return to the list
		 *
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNDELETE)) {
			try {
				Logger.debug(this, "Calling UnDelete method");
				_undeleteWebAsset(req, res, config, form, user,
						WebKeys.TEMPLATE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are deleting the Template version,
		 * run the deeleteversion action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.DELETEVERSION)) {
			try {
				Logger.debug(this, "Calling Delete Version Method");
				_deleteVersionWebAsset(req, res, config, form, user,
						WebKeys.TEMPLATE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are unpublishing the Template,
		 * run the unpublish action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNPUBLISH)) {
			try {
				Logger.debug(this, "Calling Unpublish Method");
				_unPublishWebAsset(req, res, config, form, user,
						WebKeys.TEMPLATE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are getting the Template version back,
		 * run the getversionback action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.GETVERSIONBACK)) {
			try {
				Logger.debug(this, "Calling Get Version Back Method");
				_getVersionBackWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are getting the Template versions,
		 * run the assetversions action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.ASSETVERSIONS)) {
			try {
				Logger.debug(this, "Calling Get Versions Method");
				_getVersionsWebAsset(req, res, config, form, user,
						WebKeys.TEMPLATE_EDIT, WebKeys.TEMPLATE_VERSIONS);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		}
		/*
		 * If we are unlocking the Template,
		 * run the unlock action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNLOCK)) {
			try {
				Logger.debug(this, "Calling Unlock Method");
				_unLockWebAsset(req, res, config, form, user,
						WebKeys.TEMPLATE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are copying the Template,
		 * run the copy action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.COPY)) {
			try {
				Logger.debug(this, "Calling Copy Method");
				_copyWebAsset(req, res, config, form, user);
			} catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
		} else
			Logger.debug(this, "Unspecified Action");

		HibernateUtil.commitTransaction();

		_setupEditTemplatePage(reqImpl, res, config, form, user);
		setForward(req, "portlet.ext.templates.edit_template");

	}

	///// ************** ALL METHODS HERE *************************** ////////

	private void _setupEditTemplatePage(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception {

		//Getting the host that can be assigned to the container
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		Template template = (Template) req.getAttribute(WebKeys.TEMPLATE_EDIT);
        Host templateHost = hostAPI.findParentHost(template, user, false);

		//Getting the host that can be assigned to the template
		List<Host> hosts = APILocator.getHostAPI().findAll(user, false);
		hosts.remove(APILocator.getHostAPI().findSystemHost(user, false));
		hosts = perAPI.filterCollection(hosts, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, false, user);
		if(templateHost != null && !hosts.contains(templateHost)) {
			hosts.add(templateHost);
		}
		req.setAttribute(WebKeys.TEMPLATE_HOSTS, hosts);

	}

	public void _editWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//calls edit method from super class that returns parent folder
		super._editWebAsset(req, res, config, form, user,
				WebKeys.TEMPLATE_EDIT);

		//This can't be done on the WebAsset so it needs to be done here.
		Template template = (Template) req.getAttribute(WebKeys.TEMPLATE_EDIT);

		if(InodeUtils.isSet(template.getInode())) {
			_checkReadPermissions(template, user, httpReq);
		}

		//gets image file --- on the image field on the template we store the image's identifier
		//Identifier imageIdentifier = (Identifier) InodeFactory.getInode(template.getImage(), Identifier.class);
		File imageFile = new File();
		Boolean fileAsContent = false;
		Contentlet imageContentlet = new Contentlet();

		if(InodeUtils.isSet(template.getImage())){
			Identifier imageIdentifier = APILocator.getIdentifierAPI().find(template.getImage());
			if(fileAsContent = imageIdentifier.getAssetType().equals("contentlet")) {
				imageContentlet = APILocator.getContentletAPI().findContentletByIdentifier(imageIdentifier.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
			} else {
				imageFile = (File) APILocator.getVersionableAPI().findWorkingVersion(imageIdentifier,APILocator.getUserAPI().getSystemUser(),false);
			}
		}

		TemplateForm cf = (TemplateForm) form;

		//gets the template host
		HttpSession session = httpReq.getSession();

		TemplateAPI templateAPI = APILocator.getTemplateAPI();
		Host templateHost = templateAPI.getTemplateHost(template);

		if(templateHost == null) {
	        String hostId= (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	        if(!hostId.equals("allHosts")) {
	    		//Setting the default host = the selected crumbtrail host if it is a new container
            	Host crumbHost = hostAPI.find(hostId, user, false);
            	if(crumbHost != null && permissionAPI.doesUserHavePermission(crumbHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false))
            		cf.setHostId(hostId);
	        }
		} else {
			cf.setHostId(templateHost.getIdentifier());
		}
		cf.setImage(fileAsContent?imageContentlet.getIdentifier():imageFile.getIdentifier());
	}

	@SuppressWarnings("unchecked")
	public void _saveWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {
		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//gets TemplateForm struts bean
		TemplateForm cf = (TemplateForm) form;
		Template newTemplate = new Template();
		//gets the new information for the container from the request object

		BeanUtils.copyProperties(newTemplate,form);
		req.setAttribute(WebKeys.TEMPLATE_FORM_EDIT, newTemplate);


		//gets the current template being edited from the request object
		Template currentTemplate = (Template) req.getAttribute(WebKeys.TEMPLATE_EDIT);

		//Retrieves the host were the template will be assigned to
		Host host = hostAPI.find(cf.getHostId(), user, false);

		boolean isNew = !InodeUtils.isSet(currentTemplate.getInode());

		//Checking permissions
		if (!isNew) {
			_checkWritePermissions(currentTemplate, user, httpReq);
			newTemplate.setIdentifier(currentTemplate.getIdentifier());
		} else {
			//If the asset is new checking that the user has permission to add children to the parent host
			if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
				throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
			}

		}

		//gets user id from request for mod user
		String userId = user.getUserId();
		//gets file object for the thumbnail
		if (InodeUtils.isSet(cf.getImage())) {
			newTemplate.setImage(cf.getImage());
		}


		APILocator.getTemplateAPI().saveTemplate(newTemplate,host , user, false);
		APILocator.getVersionableAPI().setLocked(newTemplate, false, user);

		SessionMessages.add(httpReq, "message", "message.template.save");

		//copies the information back into the form bean
		BeanUtils.copyProperties(form, req.getAttribute(WebKeys.TEMPLATE_FORM_EDIT));
		BeanUtils.copyProperties(currentTemplate, req.getAttribute(WebKeys.TEMPLATE_FORM_EDIT));





	}

	public void _copyWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		Logger.debug(this, "I'm copying the Template");

		//gets the current template being edited from the request object
		Template currentTemplate = (Template) req
		.getAttribute(WebKeys.TEMPLATE_EDIT);

		//Checking permissions
		_checkCopyAndMovePermissions(currentTemplate, user, httpReq,"copy");

		//Calling the copy method from the factory
		TemplateFactory.copyTemplate(currentTemplate);

		SessionMessages.add(httpReq, "message", "message.template.copy");
	}

	@SuppressWarnings("unchecked")
	public void _getVersionBackWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		Template versionTemplate = (Template) InodeFactory.getInode(req
				.getParameter("inode_version"), Template.class);

		Identifier id = (Identifier)APILocator.getIdentifierAPI().find(versionTemplate);

		//Template workingTemplate = (Template)APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(),false);

		//gets containers identifiers children from current template

		APILocator.getVersionableAPI().setWorking(versionTemplate);

		//Template newWorkingTemplate = (Template) super._getVersionBackWebAsset(req, res, config, form, user, Template.class, WebKeys.TEMPLATE_EDIT);
		TemplateServices.invalidate(versionTemplate, true);
	}

	private void updateParseContainerSyntax(Template template){
		String tb = template.getBody();
		Perl5Matcher matcher = (Perl5Matcher) localP5Matcher.get();
		String oldParse;
		String newParse;
    	while(matcher.contains(tb, parseContainerPattern)){
     		MatchResult match = matcher.getMatch();
    		int groups = match.groups();
     		for(int g=0;g<groups;g++){
     			oldParse = match.group(g);
     			if(matcher.contains(oldParse, oldContainerPattern)){
     				MatchResult matchOld = matcher.getMatch();
     				newParse = matchOld.group(0).trim();
     				newParse = "#parseContainer('" + newParse + "')";
     				tb = StringUtil.replace(tb,oldParse,newParse);
     			}
     		}
     		template.setBody(tb);
    	}
	}

}