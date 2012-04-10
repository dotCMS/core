package com.dotmarketing.portlets.contentlet.action;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portal.struts.DotPortletActionInterface;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjax;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.servlet.SessionMessages;

public class EditContentletAction extends DotPortletAction implements DotPortletActionInterface {

	private CategoryAPI catAPI;
	private PermissionAPI perAPI;
	private ContentletAPI conAPI;
	private FieldAPI fAPI;
	private RelationshipAPI relAPI;
	private HostWebAPI hostWebAPI;
	
	private String currentHost;

	public EditContentletAction() {
		catAPI = APILocator.getCategoryAPI();
		perAPI = APILocator.getPermissionAPI();
		conAPI = APILocator.getContentletAPI();
		fAPI = APILocator.getFieldAPI();
		relAPI= APILocator.getRelationshipAPI();
		hostWebAPI = WebAPILocator.getHostWebAPI();
	}
	private Contentlet contentletToEdit;

	@SuppressWarnings("unchecked")
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
			ActionResponse res) throws Exception {
		List<Contentlet> contentToIndexAfterCommit  = new ArrayList<Contentlet>();
		// wraps request to get session object
		boolean validate = true;
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		
		HttpSession ses = httpReq.getSession();

		Logger.debug(this, "############################# Contentlet");

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");
		
		Logger.debug(this, "EditContentletAction cmd=" + cmd);

		HibernateUtil.startTransaction();

		User user = _getUser(req);

		// retrieve current host
		currentHost = HostUtil.hostNameUtil(req, user);
		//http://jira.dotmarketing.net/browse/DOTCMS-2273
		//To transport PortletConfig, Layout objects using session.
		//Needed for sendContentletPublishNotification of ContentletWebAPIImpl.java 		
		ses.setAttribute(com.dotmarketing.util.WebKeys.JAVAX_PORTLET_CONFIG, config);
		Layout layout = (Layout)req.getAttribute(com.liferay.portal.util.WebKeys.LAYOUT);
		ses.setAttribute(com.dotmarketing.util.WebKeys.LAYOUT, layout);


		int structureType = req.getParameter("contype") == null ? 0:Integer.valueOf(req.getParameter("contype"));
		if(structureType==Structure.STRUCTURE_TYPE_FORM){
			if(InodeUtils.isSet(req.getParameter("structure_id"))){
				referer=referer+"&structure_id="+req.getParameter("structure_id");
			}
			else{
			referer=referer+"&structure_id="+req.getParameter("structureInode");
			}
		}
		try {
			Logger.debug(this, "Calling Retrieve method");

			_retrieveWebAsset(req, res, config, form, user);


		} catch (Exception ae) {
			if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.DELETEVERSION)) {
				return;
			}
			_handleException(ae, req);
			return;
		}

		/*
		 * We are editing the contentlet
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			try {
				Logger.debug(this, "Calling Edit Method");

				_editWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				if ((referer != null) && (referer.length() != 0)) {
					if (ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {
						// The web asset edit threw an exception because it's
						// locked so it should redirect back with message
						java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
						params.put("struts_action", new String[] { "/ext/director/direct" });
						params.put("cmd", new String[] { "editContentlet" });
						params.put("contentlet", new String[] { req.getParameter("inode") });
						params.put("container",
								new String[] { (req.getParameter("contentcontainer_inode") != null) ? req
										.getParameter("contentcontainer_inode") : "0" });
						params.put("htmlPage", new String[] { (req.getParameter("htmlpage_inode") != null) ? req
								.getParameter("htmlpage_inode") : "0" });
						params.put("referer", new String[] { java.net.URLEncoder.encode(referer, "UTF-8") });

						String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,
								WindowState.MAXIMIZED.toString(), params);

						_sendToReferral(req, res, directorURL);
					} else {
						_handleException(ae, req);
					}
				} else
					_handleException(ae, req);
				return;
			}
		}

		/*
		 * We are editing the contentlet
		 */
		if ((cmd != null)
				&& (cmd.equals(com.dotmarketing.util.Constants.NEW) || cmd
						.equals(com.dotmarketing.util.Constants.NEW_EDIT))) {
			try {
				Logger.debug(this, "Calling Edit Method");
				httpReq.getSession().removeAttribute("ContentletForm_lastLanguage");
				httpReq.getSession().removeAttribute("ContentletForm_lastLanguage_permissions");
				_newContent(req, res, config, form, user);

			} catch (Exception ae) {
				if ((referer != null) && (referer.length() != 0)) {
					if (ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {
						// The web asset edit threw an exception because it's
						// locked so it should redirect back with message
						java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
						params.put("struts_action", new String[] { "/ext/director/direct" });
						params.put("cmd", new String[] { "editContentlet" });
						params.put("contentlet", new String[] { req.getParameter("inode") });
						params.put("container",
								new String[] { (req.getParameter("contentcontainer_inode") != null) ? req
										.getParameter("contentcontainer_inode") : "0" });
						params.put("htmlPage", new String[] { (req.getParameter("htmlpage_inode") != null) ? req
								.getParameter("htmlpage_inode") : "0" });
						params.put("referer", new String[] { java.net.URLEncoder.encode(referer, "UTF-8") });

						String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,
								WindowState.MAXIMIZED.toString(), params);

						_sendToReferral(req, res, directorURL);
					} else if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
						_sendToReferral(req, res, referer);
					} else {
						_handleException(ae, req);
					}
				} else
					_handleException(ae, req);
				return;
			}
		}

		
		/*
		 * If we are updating the contentlet, copy the information from the
		 * struts bean to the hbm inode and run the update action and return to
		 * the list
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				Logger.debug(this, "Calling Save Method");
				try{
					_saveWebAsset(req, res, config, form, user);
				}catch (DotContentletValidationException ce) {
					((ContentletForm)form).setHasvalidationerrors(true);
					SessionMessages.add(req, "message.contentlet.save.error");
					HibernateUtil.commitTransaction();
					reindexContentlets(contentToIndexAfterCommit,cmd);
					//This is called to preserve the values submitted in the form
					//in case of a validation error
					_loadForm(req, res, config, form, user, false);					
					setForward(req, "portlet.ext.contentlet.edit_contentlet");
					return;
				}catch (Exception ce) {
					SessionMessages.add(req, "message.contentlet.save.error");
					_loadForm(req, res, config, form, user, false);
					HibernateUtil.commitTransaction();
					reindexContentlets(contentToIndexAfterCommit,cmd);
					setForward(req, "portlet.ext.contentlet.edit_contentlet");
					return;
				}
				Logger.debug(this, "HTMLPage inode=" + req.getParameter("htmlpage_inode"));
				Logger.debug(this, "Container inode=" + req.getParameter("contentcontainer_inode"));
				if (req.getParameter("htmlpage_inode") != null
						&& req.getParameter("contentcontainer_inode") != null) {
					try {
						Logger.debug(this, "I'm setting my contentlet parents");
						_addToParents(req, res, config, form, user);
					} catch (Exception ae) {
						_handleException(ae, req);
						return;
					}
				}
				((ContentletForm)form).setMap(((Contentlet)req.getAttribute(WebKeys.CONTENTLET_FORM_EDIT)).getMap());
				try {
					String subcmd = req.getParameter("subcmd");
					String language = req.getParameter("languageId");
					Logger.debug(this, "AFTER PUBLISH LANGUAGE=" + language);
					if (UtilMethods.isSet(language) && referer.indexOf("language") > -1) {
						Logger.debug(this, "Replacing referer language=" + referer);
						referer = referer.replaceAll("language=([0-9])*", com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE+"=" + language);
						Logger.debug(this, "Referer after being replaced=" + referer);
					}
				}catch(Exception e){
					SessionMessages.add(req, "error","message.saved.but.not.publish");
				}

				_sendToReferral(req, res, referer); 
			} catch (Exception ae) {
				_handleException(ae, req);
			}
		}
		/*
		 * If we are deleting the contentlet, run the delete action and return
		 * to the list
		 * 
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				Logger.debug(this, "Calling Delete Method");

				List<Contentlet> contentlets = new ArrayList<Contentlet>();
				contentlets.add(contentletToEdit);
				try{
					conAPI.archive(contentlets, user, false);
					SessionMessages.add(httpReq, "message", "message.contentlet.delete");
				}catch(DotContentletStateException dcse){
					Logger.error(this,"Something happened while trying to archive content" , dcse);
					SessionMessages.add(httpReq, "message", "message.contentlet.delete.locked");
				}

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
				Contentlet contentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);			
				try{
					conAPI.delete(contentlet, user, false);
					SessionMessages.add(httpReq, "message", "message.contentlet.full_delete");
				}catch (Exception e) {
					SessionMessages.add(httpReq, "error", "message.contentlet.full_delete.error");
				}	
			}
			catch(Exception ae) 
			{
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are undeleting the container, run the undelete action and
		 * return to the list
		 * 
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.UNDELETE)) {
			try {
				Logger.debug(this, "Calling UnDelete Method");
				conAPI.unarchive(contentletToEdit, user, false);
				SessionMessages.add(httpReq, "message", "message.contentlet.undelete");	
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are deleting the container version, run the delete version
		 * action and return to the list
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.DELETEVERSION)) {
			try {
				Logger.debug(this, "Calling Delete Version Method");

				_deleteVersion(req, res, config, form, user, WebKeys.CONTENTLET_EDIT);
				return;
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			
		}
		/*
		 * If we are unpublishing the container, run the unpublish action and
		 * return to the list
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.UNPUBLISH)) {
			try {
				Logger.debug(this, "Calling Unpublish Method");

				// calls the asset factory edit
				try{
					conAPI.unpublish(contentletToEdit, user, false);
					ActivityLogger.logInfo(this.getClass(), "Unpublishing Contentlet "," User "+user.getFirstName()+" Unpublished content titled '"+contentletToEdit.getTitle()+"'", HostUtil.hostNameUtil(req, user));
					SessionMessages.add(httpReq, "message", "message.contentlet.unpublished");
				}catch(DotLockException dlock){
					SessionMessages.add(httpReq, "error", "message.contentlet.cannot.be.unlocked");
				}catch(DotContentletStateException dcse){
					SessionMessages.add(httpReq, "message", "message.contentlet.unpublish.notlive_or_locked");
				}
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			if(UtilMethods.isSet(req.getParameter("selected_lang"))){
				referer=referer+"&selected_lang="+req.getParameter("selected_lang");
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are getting the container version back, run the getversionback
		 * action and return to the list
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.GETVERSIONBACK)) {
			try {
				Logger.debug(this, "Calling Get Version Back Method");

				_getVersionBackWebAsset(req, res, config, form, user);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}

			Contentlet workingContentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);

			/* 
			 href =  "<portlet:actionURL>";
			 href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
			 href += "<portlet:param name='cmd' value='edit' />";
			 href += "<portlet:param name='referer' value='<%=referer%>' />";
			 href += "</portlet:actionURL>";
			 */ 

			HashMap params = new HashMap ();
			params.put("struts_action", new String [] { "/ext/contentlet/edit_contentlet" });
			params.put("inode", new String [] { String.valueOf(workingContentlet.getInode()) });
			params.put("cmd", new String [] { "edit" });
			params.put("referer", new String [] { referer });
			referer = PortletURLUtil.getActionURL(req, WindowState.MAXIMIZED.toString(), params);

			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are getting the container versions, run the assetversions
		 * action and return to the list
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.ASSETVERSIONS)) {
			try {
				Logger.debug(this, "Calling Get Versions Method");

				_getVersionsWebAsset(req, res, config, form, user, WebKeys.CONTENTLET_EDIT, WebKeys.CONTENTLET_VERSIONS);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		}
		/*
		 * If we are unlocking the container, run the unlock action and return
		 * to the list
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.UNLOCK)) {
			try {
				Logger.debug(this, "Calling Unlock Method");


				// http://jira.dotmarketing.net/browse/DOTCMS-1073
				// deleting uploaded files 
				/*Logger.debug(this, "Deleting uploaded files");
				java.io.File tempUserFolder = new java.io.File(Config.CONTEXT
						.getRealPath(com.dotmarketing.util.Constants.TEMP_BINARY_PATH)
						+ java.io.File.separator + user.getUserId());

				FileUtil.deltree(tempUserFolder);*/				

				if(perAPI.doesUserHavePermission(contentletToEdit, PermissionAPI.PERMISSION_WRITE, user)) {
					try{
						conAPI.unlock(contentletToEdit, user, false);
					}
					catch(Exception e){
						SessionMessages.add(httpReq, "error", "message.contentlet.cannot.be.unlocked");
						_sendToReferral(req, res, referer);
						return;
					}
					SessionMessages.add(httpReq, "message", "message.contentlet.unlocked");
				}

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}else if(cmd != null && cmd.equals("export")){

			try {
				boolean showDeleted = Boolean.parseBoolean( req.getParameter("showDeleted"));
				boolean filterSystemHost = Boolean.parseBoolean( req.getParameter("filterSystemHost"));
				boolean filterLocked = Boolean.parseBoolean( req.getParameter("filterLocked"));
				String categories = req.getParameter("expCategoriesValues");
				String fields = req.getParameter("expFieldsValues");
				String structureInode = req.getParameter("expStructureInode");

				ActionResponseImpl resImpl = (ActionResponseImpl) res;
				HttpServletResponse response = resImpl.getHttpServletResponse();

				downloadToExcel(response, user,searchContentlets(req,res,config,form,user,"Excel"));

			} catch (Exception ae) {
				_handleException(ae, req);
			}

			if(UtilMethods.isSet(referer)){
				_sendToReferral(req, res, referer);
			}else{
				setForward(req,"portlet.ext.contentlet.view_contentlets");
			}

		}
		/**
		 * If whe are going to unpublish a list of contentlets
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_UNPUBLISH_LIST)) 
		{
			try {	
				_batchUnpublish(req, res, config, form, user,contentToIndexAfterCommit);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			if(UtilMethods.isSet(req.getParameter("selected_lang"))){
				referer=referer+"&selected_lang="+req.getParameter("selected_lang");
			}
			_sendToReferral(req, res, referer);

		}

		/**
		 * If whe are going to publish a list of contentlets
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_PUBLISH_LIST)) 
		{
			try {	
				_batchPublish(req, res, config, form, user,contentToIndexAfterCommit);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			if(UtilMethods.isSet(req.getParameter("selected_lang"))){
				referer=referer+"&selected_lang="+req.getParameter("selected_lang");
			}
			 _sendToReferral(req, res, referer);
		}
		/**
		 * If whe are going to archive a list of contentlets
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_ARCHIVE_LIST)) 
		{	

			try {	
				_batchArchive(req, res, config, form, user,contentToIndexAfterCommit);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}		
			_sendToReferral(req, res, referer);

		}
		/**
		 * If whe are going to un-archive a list of contentlets
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_UNARCHIVE_LIST)) 
		{
			try {	
				_batchUnArchive(req, res, config, form, user,contentToIndexAfterCommit);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}		
			_sendToReferral(req, res, referer);


		}

		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_REINDEX_LIST)) 
		{
			try {	
				_batchReindex(req, res, config, form, user);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}		
			_sendToReferral(req, res, referer);


		}

		/**
		 * If whe are going to un-archive a list of contentlets
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_DELETE_LIST)) 
		{
			try {	
				_batchDelete(req, res, config, form, user,contentToIndexAfterCommit);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		
		/**
		 * If whe are going to un-lock a list of contentlets
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_UNLOCK_LIST)) 
		{
			try {	
				_batchUnlock(req, res, config, form, user,contentToIndexAfterCommit);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			if(UtilMethods.isSet(req.getParameter("selected_lang"))){
				referer=referer+"&selected_lang="+req.getParameter("selected_lang");
			}
			 _sendToReferral(req, res, referer);
		}
		/*
		 * If we are copying the container, run the copy action and return to
		 * the list
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.COPY)) {
			try {
				Logger.debug(this, "Calling Copy Method");				
				_copyWebAsset(req, res, config, form, user);
			} 
			catch(DotContentletValidationException ve) {
				SessionMessages.add(httpReq, "error", "message.contentlet.copy.relation.not_valid");
				_handleException(ve, req);			
			}		
			catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
		} else
			Logger.debug(this, "Unspecified Action");
		_loadForm(req, res, config, form, user, validate);


		reindexContentlets(contentToIndexAfterCommit,cmd);
		HibernateUtil.commitTransaction();
		
		if(UtilMethods.isSet(req.getAttribute("inodeToWaitOn"))){
			if(!conAPI.isInodeIndexed(req.getAttribute("inodeToWaitOn").toString())){
				Logger.error(this, "Timedout waiting on index to return");
			}
		}
		req.setAttribute("cache_control", "0");
		setForward(req, "portlet.ext.contentlet.edit_contentlet");
	}

	// /// ************** ALL METHODS HERE *************************** ////////

	protected void _retrieveWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form,User user) throws Exception {
		String inode = req.getParameter("inode");				
		String inodeStr = (InodeUtils.isSet(inode) ? inode : "");
		Contentlet contentlet = new Contentlet();

		if(InodeUtils.isSet(inodeStr))
		{			
			Boolean makeEditable = Boolean.valueOf(req.getParameter("makeEditable"));
			Boolean populateaccept = Boolean.valueOf(req.getParameter("populateaccept"));
			if(makeEditable && !populateaccept){
			   contentlet = conAPI.checkout(inodeStr, user, false);
			   contentlet.setInode(inodeStr);
			}else{
			   contentlet = conAPI.find(inodeStr, user, false);
			}
		}else {
			/*In case of multi-language first ocurrence new contentlet*/
			String sibblingInode = req.getParameter("sibbling");
			if(InodeUtils.isSet(sibblingInode) && !sibblingInode.equals("0")){
				Contentlet sibblingContentlet = conAPI.find(sibblingInode,APILocator.getUserAPI().getSystemUser(), false);
				Logger.debug(UtilHTML.class, "getLanguagesIcons :: Sibbling Contentlet = "
						+ sibblingContentlet.getInode());
				Identifier identifier = APILocator.getIdentifierAPI().find(sibblingContentlet);
				contentlet.setIdentifier(identifier.getInode());
				contentlet.setStructureInode(sibblingContentlet.getStructureInode());
			}
			String langId = req.getParameter("lang");
			if(UtilMethods.isSet(langId)){
				contentlet.setLanguageId(Long.parseLong(langId));
			}
		}

		if(perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false));
		req.setAttribute(WebKeys.CONTENTLET_EDIT, contentlet);
		contentletToEdit = contentlet;

		// Contententlets Relationships
		Structure st = contentlet.getStructure();
		if (st == null || !InodeUtils.isSet(st.getInode())) {
			String selectedStructure = "";
			if (InodeUtils.isSet(req.getParameter("selectedStructure"))) {
				selectedStructure = req.getParameter("selectedStructure");
				st = (Structure) InodeFactory.getInode(selectedStructure, Structure.class);
			} else if (InodeUtils.isSet(req.getParameter("contentcontainer_inode"))) {
				String containerInode = req.getParameter("contentcontainer_inode");
				Container container = (Container) InodeFactory.getInode(containerInode, Container.class);
				st = (Structure) InodeFactory.getInode(container.getStructureInode(), Structure.class);
			}else if (InodeUtils.isSet(req.getParameter("sibblingStructure"))) {
				selectedStructure = req.getParameter("sibblingStructure");
				st = (Structure) InodeFactory.getInode(selectedStructure, Structure.class);
			}else{
				st = StructureFactory.getDefaultStructure();
				((ContentletForm)form).setAllowChange(true);
			}
		}
		((ContentletForm)form).setStructureInode(st.getInode());

		_loadContentletRelationshipsInRequest(req, contentlet, st, user);

		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//This parameter is used to determine if the structure was selected from Add/Edit Content link in subnav.jsp, from
		//the Content Search Manager
		if(httpReq.getParameter("selected") != null){
			httpReq.getSession().setAttribute("selectedStructure", st.getInode());
		}

		// Asset Versions to list in the versions tab
		req.setAttribute(WebKeys.VERSIONS_INODE_EDIT, contentlet);

	}

	private void _loadContentletRelationshipsInRequest(ActionRequest request, Contentlet contentlet, Structure structure,User user) throws DotDataException {
		ContentletAPI contentletService = APILocator.getContentletAPI();
		contentlet.setStructureInode(structure.getInode());
		ContentletRelationships cRelationships = contentletService.getAllRelationships(contentlet); 		
		
		//DOTCMS-6097, if we don't have the related piece of content in the language the user is looking at, we show the flag of the language user is on but in gray.
		List<ContentletRelationships.ContentletRelationshipRecords> relationshipRecords = cRelationships.getRelationshipsRecords();
		for(ContentletRelationshipRecords contentletRelationshipRecords: relationshipRecords){
			List<Contentlet> contentletsList = contentletRelationshipRecords.getRecords();
			List<Contentlet> newContentletsList = new ArrayList<Contentlet>();
			for(Contentlet con: contentletsList){
				if(contentlet.getLanguageId() == con.getLanguageId()){
					newContentletsList.add(con);
				}else{
					try {
						List<Contentlet> allLangContentletsList = conAPI.getAllLanguages(con, null, user, false);
						boolean isAdded = false;
						for(Contentlet langCon: allLangContentletsList){
							if(langCon.getLanguageId() == contentlet.getLanguageId()){
								if(langCon.isLive() && !isAdded){
									isAdded = true;
									newContentletsList.add(langCon);
								}else if(langCon.isWorking() && !isAdded){
									isAdded = true;
									newContentletsList.add(langCon);
								}
							}
						}
						if(!isAdded){
							newContentletsList.add(con);
						}
					} catch (DotSecurityException e) {
						Logger.error(this, e.getMessage());						
					}
				}
			}			
			contentletRelationshipRecords.setRecords(newContentletsList);
		}
		
		request.setAttribute(WebKeys.CONTENTLET_RELATIONSHIPS_EDIT, cRelationships);

	}

	private void _addToParents(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		Logger.debug(this, "Inside AddContentletToParentsAction");

		Contentlet contentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_FORM_EDIT);
		// Contentlet currentContentlet = (Contentlet)
		// req.getAttribute(WebKeys.CONTENTLET_EDIT);
		Contentlet currentContentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);

		Logger.debug(this, "currentContentlet inode=" + currentContentlet.getInode());
		Logger.debug(this, "contentlet inode=" + contentlet.getInode());

		// it's a new contentlet. we should add to parents
		// if it's a version the parents get copied on save asset method
		if (currentContentlet.getInode().equalsIgnoreCase(contentlet.getInode())) {
			String htmlpage_inode = req.getParameter("htmlpage_inode");
			String contentcontainer_inode = req.getParameter("contentcontainer_inode");
			HTMLPage htmlParent = (HTMLPage) InodeFactory.getInode(htmlpage_inode, HTMLPage.class);
			Logger.debug(this, "Added Contentlet to parent=" + htmlParent.getInode());
			Container containerParent = (Container) InodeFactory.getInode(contentcontainer_inode, Container.class);
			Logger.debug(this, "Added Contentlet to parent=" + containerParent.getInode());

			Identifier iden = APILocator.getIdentifierAPI().find(contentlet);

			if (InodeUtils.isSet(htmlParent.getInode()) && InodeUtils.isSet(containerParent.getInode()) && InodeUtils.isSet(contentlet.getInode())) {
				Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find(htmlParent);
				Identifier containerIdentifier = APILocator.getIdentifierAPI().find(containerParent);
				Identifier contenletIdentifier = APILocator.getIdentifierAPI().find(contentlet);
				MultiTree multiTree = MultiTreeFactory.getMultiTree(htmlPageIdentifier, containerIdentifier,
						contenletIdentifier);
				Logger.debug(this, "Getting multitree for=" + htmlParent.getInode() + " ," + containerParent.getInode()
						+ " ," + contentlet.getIdentifier());
				Logger.debug(this, "Coming from multitree parent1=" + multiTree.getParent1() + " parent2="
						+ multiTree.getParent2());

				if (!InodeUtils.isSet(multiTree.getParent1()) && !InodeUtils.isSet(multiTree.getParent2()) && !InodeUtils.isSet(multiTree.getChild())) {

					Logger.debug(this, "MTree is null!!! Creating new one!");

					MultiTree mTree = new MultiTree(htmlPageIdentifier.getInode(), containerIdentifier.getInode(),
							contenletIdentifier.getInode(),null,containerParent.getMaxContentlets());
					MultiTreeFactory.saveMultiTree(mTree);
				}

				//Updating the last mod user and last mod date of the page
				htmlParent.setModDate(new Date());
				htmlParent.setModUser(user.getUserId());
				HibernateUtil.saveOrUpdate(htmlParent);

			}
			SessionMessages.add(httpReq, "message", "message.contentlet.add.parents");
		}
	}

	private void _newContent(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//Contentlet Form
		ContentletForm cf = (ContentletForm) form;

		String cmd = req.getParameter(Constants.CMD);
		String inode = req.getParameter("inode");
		String inodeStr = (InodeUtils.isSet(inode) ? inode : "");
		Contentlet contentlet = new Contentlet();

		if(InodeUtils.isSet(inodeStr))
			contentlet = conAPI.find(inodeStr, user, false);

		req.setAttribute(WebKeys.CONTENTLET_EDIT, contentlet);
		Structure structure = contentlet.getStructure();

		String selectedStructure = "";
		if (InodeUtils.isSet(req.getParameter("selectedStructure"))) {
			selectedStructure = req.getParameter("selectedStructure");
			structure = (Structure) InodeFactory.getInode(selectedStructure, Structure.class);
			contentlet.setStructureInode(structure.getInode());
		} else if (cmd.equals("newedit")) {
			String containerInode = req.getParameter("contentcontainer_inode");
			Container container = (Container) InodeFactory.getInode(containerInode, Container.class);
			structure = (Structure) InodeFactory.getInode(container.getStructureInode(), Structure.class);
			contentlet.setStructureInode(structure.getInode());
		}

		String langId = req.getParameter("lang");
		if(UtilMethods.isSet(langId)) {
			try {
				contentlet.setLanguageId(Integer.parseInt(langId));
			} catch (NumberFormatException e) { 
				contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
			}
		}else{
			contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		Map<String, Object> lastSearchMap = new HashMap<String, Object>();
		lastSearchMap.put("structure", structure);
		lastSearchMap.put("fieldsSearch",new HashMap<String,String>());
		lastSearchMap.put("categories",new ArrayList<String>());
		lastSearchMap.put("showDeleted",false);
		lastSearchMap.put("filterSystemHost",false);
		lastSearchMap.put("filterLocked",false);
		lastSearchMap.put("page",1);
		lastSearchMap.put("orderBy","modDate desc");
		httpReq.getSession().setAttribute(WebKeys.CONTENTLET_LAST_SEARCH, lastSearchMap);


		// Checking permissions to add new of structure selected
		_checkWritePermissions(structure, user, httpReq);

		List<Field> list = (List<Field>) FieldsCache.getFieldsByStructureInode(structure.getInode());
		for (Field field : list) {
			Object value = null;
			String defaultValue = field.getDefaultValue();
			if (UtilMethods.isSet(defaultValue)) {
				String typeField = field.getFieldContentlet();
				if (typeField.startsWith("bool")) {
					value = defaultValue;
				} else if (typeField.startsWith("date")) {
					SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					String date = defaultValue;
					value = dateFormatter.parse(date);
				} else if (typeField.startsWith("float")) {
					value = defaultValue;
				} else if (typeField.startsWith("integer")) {
					value = defaultValue;
				} else if (typeField.startsWith("text")) {
					value = defaultValue;
				}

				if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())
						|| field.getFieldType().equals(Field.FieldType.FILE.toString())) {
					try {
						//Identifier id = (Identifier) InodeFactory.getInode((String) value, Identifier.class);
						Identifier id = APILocator.getIdentifierAPI().find((String) value);
						if (InodeUtils.isSet(id.getInode())) {
							if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
								File inodeAux = (File) APILocator.getVersionableAPI().findWorkingVersion(id,  APILocator.getUserAPI().getSystemUser(), false);
								value = inodeAux.getInode();
							} else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
								File inodeAux = (File) APILocator.getVersionableAPI().findWorkingVersion(id,  APILocator.getUserAPI().getSystemUser(), false);
								value = inodeAux.getInode();
							}
						}
					} catch (Exception ex) {
						Logger.debug(this, ex.toString());
					}
				}
				BeanUtils.setProperty(contentlet, typeField, value);
			}
		}

		//Setting review intervals form properties
		if (structure.getReviewInterval() != null) {
			String interval = structure.getReviewInterval();
			Pattern p = Pattern.compile("(\\d+)([dmy])");
			Matcher m = p.matcher(interval);
			boolean b = m.matches();
			if (b) {
				cf.setReviewContent(true);
				String g1 = m.group(1);
				String g2 = m.group(2);
				cf.setReviewIntervalNum(g1);
				cf.setReviewIntervalSelect(g2);
			} 
		}

	}

	@SuppressWarnings("unchecked")
	public void _editWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user)
	throws Exception {

		ContentletForm cf = (ContentletForm) form;
		ContentletAPI contAPI = APILocator.getContentletAPI();

		Contentlet contentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);
		Contentlet workingContentlet = null;
		
		String sib= req.getParameter("sibbling");
		Boolean populateaccept = Boolean.valueOf(req.getParameter("populateaccept"));
		
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		httpReq.getSession().setAttribute("populateAccept", populateaccept);
		
		
		if(UtilMethods.isSet(sib))
		{
			Contentlet sibbling=conAPI.find(sib, user,false);
			conAPI.unlock(sibbling, user, false);
			if(populateaccept){
				contentlet = sibbling;
				contentlet.setInode("");
				//http://jira.dotmarketing.net/browse/DOTCMS-5802
				Structure structure = contentlet.getStructure();
				List<Field> list = (List<Field>) FieldsCache.getFieldsByStructureInode(structure.getInode());
				for (Field field : list) {
					if(field.getFieldContentlet().startsWith("binary")){
						httpReq.getSession().setAttribute(field.getFieldContentlet() + "-sibling", sib+","+field.getVelocityVarName());
					}
				}
			}
		}
		
		if(InodeUtils.isSet(contentlet.getInode())){
			workingContentlet = contAPI.findContentletByIdentifier(contentlet.getIdentifier(), false, contentlet.getLanguageId(), user, false);
		}else{
			workingContentlet = contentlet;
		}
		
		String langId = req.getParameter("lang");
		if(UtilMethods.isSet(langId)){
			contentlet.setLanguageId(Long.parseLong(langId));
		}

		GregorianCalendar cal = new GregorianCalendar();
		if (contentlet.getModDate() == null) {
			contentlet.setModDate(cal.getTime());
		}

		if(UtilMethods.isSet(sib)) {
		    req.setAttribute(WebKeys.CONTENT_EDITABLE, true);
		}
		else {
    		if(perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, user) && workingContentlet.isLocked()){
    			
    			String lockedUserId = APILocator.getVersionableAPI().getLockedBy(workingContentlet);
    			if(user.getUserId().equals(lockedUserId)){
    				req.setAttribute(WebKeys.CONTENT_EDITABLE, true);
    			}else{
    				req.setAttribute(WebKeys.CONTENT_EDITABLE, false);
    			}
    		}else{
    			req.setAttribute(WebKeys.CONTENT_EDITABLE, false);
    		}
    
    		if (contentlet.isArchived()) {
    			Company comp = PublicCompanyFactory.getDefaultCompany();
    			String message = LanguageUtil.get(comp.getCompanyId(), user.getLocale(), "message.contentlet.edit.deleted");
    			SessionMessages.add(req, "custommessage", message);
    		}
		}
		//	http://jira.dotmarketing.net/browse/DOTCMS-1073	
		//  retrieve file names while edit
		/*Logger.debug(this,"EditContentletAAction : retrieving binary field values.");
		Structure structure = contentlet.getStructure();
		List<Field> list = (List<Field>) FieldsCache.getFieldsByStructureInode(structure.getInode());
		for (Field field : list) {
			String value = "";
			if(field.getFieldContentlet().startsWith("binary")){
				//	http://jira.dotmarketing.net/browse/DOTCMS-2178
				java.io.File binaryFile = conAPI.getBinaryFile(contentlet.getInode(),field.getVelocityVarName(),user);
				//http://jira.dotmarketing.net/browse/DOTCMS-3463
				contentlet.setBinary(field.getVelocityVarName(),binaryFile);
			}
		}*/
		cf.setMap(new HashMap<String, Object>(contentlet.getMap()));

		Logger.debug(this, "EditContentletAction: contentletInode=" + contentlet.getInode());

		req.setAttribute(WebKeys.CONTENTLET_EDIT, contentlet);

		if (contentlet.getReviewInterval() != null) {
			String interval = contentlet.getReviewInterval();
			Pattern p = Pattern.compile("(\\d+)([dmy])");
			Matcher m = p.matcher(interval);
			boolean b = m.matches();
			if (b) {
				cf.setReviewContent(true);
				String g1 = m.group(1);
				String g2 = m.group(2);
				cf.setReviewIntervalNum(g1);
				cf.setReviewIntervalSelect(g2);
			} 
		}
		//DOTCMS-6097
		if(UtilMethods.isSet(req.getParameter("is_rel_tab")))
			req.setAttribute("is_rel_tab", req.getParameter("is_rel_tab"));
	}

	@SuppressWarnings("deprecation")
	private boolean _populateContent(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, Contentlet contentlet) throws Exception, DotContentletValidationException {
		ContentletForm contentletForm = (ContentletForm) form;
		if(InodeUtils.isSet(contentletForm.getIdentifier()) && !contentletForm.getIdentifier().equals(contentlet.getIdentifier())){
			throw new DotContentletValidationException("The content form submission data id different from the content which is trying to be edited");
		}
		try {
			String structureInode = contentlet.getStructureInode();
			if (!InodeUtils.isSet(structureInode)) {
				String selectedStructure = req.getParameter("selectedStructure");
				if (InodeUtils.isSet(selectedStructure)) {
					structureInode = selectedStructure;
				}
			}
			contentlet.setStructureInode(structureInode);

			contentlet.setIdentifier(contentletForm.getIdentifier());
			contentlet.setInode(contentletForm.getInode());
			contentlet.setLanguageId(contentletForm.getLanguageId());
			contentlet.setReviewInterval(contentletForm.getReviewInterval());
			List<String> disabled = new ArrayList<String>();
			CollectionUtils.addAll(disabled, contentletForm.getDisabledWysiwyg().split(","));
			contentlet.setDisabledWysiwyg(disabled);

			List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
			for (Field field : fields){
				if ((fAPI.isElementConstant(field))){
					continue;
				}
				String value = req.getParameter(field.getFieldContentlet());
				String typeField = field.getFieldType();
				/* Validate if the field is read only, if so then check to see if it's a new contentlet
				 * and set the structure field default value, otherwise do not set the new value.
				 */
				if (!typeField.equals(Field.FieldType.HIDDEN.toString()) && 
						!typeField.equals(Field.FieldType.IMAGE.toString()) && 
						!typeField.equals(Field.FieldType.FILE.toString())) 
				{
					if(field.isReadOnly() && !InodeUtils.isSet(contentlet.getInode()))
						value = field.getDefaultValue();
					if (field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
						//WYSIWYG workaround because the WYSIWYG includes a <br> even if the field was left blank by the user
						//we have to check the value to leave it blank in that case.
						if (value instanceof String && ((String)value).trim().toLowerCase().equals("<br>")) {
							value = "";
						}
					}
				}
				if(value != null && APILocator.getFieldAPI().valueSettable(field))
					try{
						conAPI.setContentletProperty(contentlet, field, value);
					}catch (Exception e) {
						Logger.info(this, "Unable to set field " + field.getFieldName() + " to value " + value);
						Logger.debug(this, "Unable to set field " + field.getFieldName() + " to value " + value, e);
					}
			}
			// this shoud be done elsewhere as the contentlet isn't yet saved
			/*String subcmd = req.getParameter("subcmd");
			if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
				Logger.debug(this, "Setting Publish to true");
				contentlet.setLive(true);
			}else{
				Logger.debug(this, "Setting live to false");
				contentlet.setLive(false);
			}*/
			return true;
		} catch (Exception ex) {
			return false;
		}
	}


	@SuppressWarnings({ "unchecked", "deprecation" })
	public void _saveWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception, DotContentletValidationException {
		ActionErrors ae = new ActionErrors();
		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		
		String subcmd = req.getParameter("subcmd");

		// Getting the contentlets variables to work
		Contentlet currentContentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);
		String currentContentident = currentContentlet.getIdentifier();
		boolean isNew = false;
		if(!(InodeUtils.isSet(currentContentlet.getInode()))){
			isNew = true;
		}
		if(!isNew){
			try{
				currentContentlet = conAPI.checkout(currentContentlet.getInode(), user, false);
			}catch (DotSecurityException dse) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
				throw new DotSecurityException("User cannot checkout contentlet : ", dse);
			}
		}
		req.setAttribute(WebKeys.CONTENTLET_FORM_EDIT, currentContentlet);
		req.setAttribute(WebKeys.CONTENTLET_EDIT, currentContentlet);

		ContentletForm contentletForm = (ContentletForm) form;
		String owner = contentletForm.getOwner();

		try{
			_populateContent(req, res, config, form, user, currentContentlet);
			//http://jira.dotmarketing.net/browse/DOTCMS-1450
			//The form doesn't have the identifier in it. so the populate content was setting it to 0 
			currentContentlet.setIdentifier(currentContentident);
		}catch (DotContentletValidationException ve) {
			ae.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.invalid.form"));
			req.setAttribute(Globals.ERROR_KEY, ae);
			throw new DotContentletValidationException(ve.getMessage());
		}		

		//Saving interval review properties
		if (contentletForm.isReviewContent()) {
			currentContentlet.setReviewInterval(contentletForm.getReviewIntervalNum() + contentletForm.getReviewIntervalSelect());
		} else {
			currentContentlet.setReviewInterval(null);
		}

		// saving the review dates
		currentContentlet.setLastReview(new Date ());
		if (currentContentlet.getReviewInterval() != null) {
			currentContentlet.setNextReview(conAPI.getNextReview(currentContentlet, user, false));
		}

		ArrayList<Category> cats = new ArrayList<Category>();
		// Getting categories that come from the entity
		String[] arr = req.getParameterValues("categories") == null?new String[0]:req.getParameterValues("categories");
		if (arr != null && arr.length > 0) {
			for (int i = 0; i < arr.length; i++) {
				Category category = catAPI.find(arr[i], user, false);
				if(!cats.contains(category))
				{
					cats.add(category);	
				}

			}
		}


		try{

			currentContentlet.setInode(null);
			ContentletRelationships contRel = retrieveRelationshipsData(currentContentlet,user, req );

			// http://jira.dotmarketing.net/browse/DOTCMS-65
			// Coming from other contentlet to relate it automatically
			String relateWith = req.getParameter("relwith");
			String relationType = req.getParameter("reltype");
			String relationHasParent = req.getParameter("relisparent");
			if(relateWith != null){
				try {

					List<ContentletRelationshipRecords> recordsList = contRel.getRelationshipsRecords();
					for(ContentletRelationshipRecords records : recordsList) {
						if(!records.getRelationship().getRelationTypeValue().equals(relationType))
							continue;
						if(RelationshipFactory.isSameStructureRelationship(records.getRelationship()) &&
								((!records.isHasParent() && relationHasParent.equals("no")) || 
										(records.isHasParent() && relationHasParent.equals("yes"))))
							continue;
						records.getRecords().add(conAPI.find(relateWith, user, false));

					}


				} catch (Exception e) {
					Logger.error(this,"Contentlet failed while creating new relationship",e);
				}

			}

			//Checkin in the content
			currentContentlet = conAPI.checkin(currentContentlet, contRel, cats, _getSelectedPermissions(req, currentContentlet), user, false);
			
	        if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
	            Logger.debug(this, "publishing after checkin");
	            ActivityLogger.logInfo(this.getClass(), "Publishing Contentlet "," User "+user.getFirstName()+" published content titled '"+currentContentlet.getTitle(), HostUtil.hostNameUtil(req, user));
	            APILocator.getVersionableAPI().setLive(currentContentlet);
	        }

		}catch(DotContentletValidationException ve) {
			if(ve.hasRequiredErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED);
				for (Field field : reqs) {
					ae.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.required", field.getFieldName()));
				}
			}
			if(ve.hasLengthErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_MAXLENGTH);
				for (Field field : reqs) {
					ae.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.maxlength", field.getFieldName(),"255"));
				}
			}
			if(ve.hasPatternErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_PATTERN);
				for (Field field : reqs) {
					ae.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.format", field.getFieldName()));
				}
			}
			if(ve.hasRelationshipErrors()){
				StringBuffer sb = new StringBuffer("<br>");
				Map<String,Map<Relationship,List<Contentlet>>> notValidRelationships = ve.getNotValidRelationship();
				Set<String> auxKeys = notValidRelationships.keySet();
				for(String key : auxKeys)
				{
					String errorMessage = "";
					if(key.equals(DotContentletValidationException.VALIDATION_FAILED_REQUIRED_REL))
					{
						errorMessage = "<b>Required Relationship</b>";
					}
					else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_INVALID_REL_CONTENT))
					{
						errorMessage = "<b>Invalid Relationship-Contentlet</b>";
					}
					else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_BAD_REL))
					{
						errorMessage = "<b>Bad Relationship</b>";
					}

					sb.append(errorMessage + ":<br>");
					Map<Relationship,List<Contentlet>> relationshipContentlets = notValidRelationships.get(key);			
					for(Entry<Relationship,List<Contentlet>> relationship : relationshipContentlets.entrySet())
					{			
						sb.append(relationship.getKey().getRelationTypeValue() + ", ");
					}					
					sb.append("<br>");			
				}
				sb.append("<br>");

				//need to update message to support multiple relationship validation errors
				ae.add(Globals.ERROR_KEY, new ActionMessage("message.relationship.required_ext",sb.toString()));
			}



			if(ve.hasUniqueErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_UNIQUE);
				for (Field field : reqs) {
					ae.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.unique", field.getFieldName()));
				}
			}

			req.setAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_RELATIONSHIPS_EDIT, getCurrentContentletRelationships(req, user));			

			throw ve;
		}catch (Exception e) {

			ae.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.save.error"));
			Logger.error(this,"Contentlet failed during checkin",e);
			throw e;
		}finally{
			if (ae != null && ae.size() > 0){ 
				req.setAttribute(Globals.ERROR_KEY, ae);
			}
		}
		req.setAttribute("inodeToWaitOn", currentContentlet.getInode());
		req.setAttribute(WebKeys.CONTENTLET_EDIT, currentContentlet);
		req.setAttribute(WebKeys.CONTENTLET_FORM_EDIT, currentContentlet);		
		if (Config.getBooleanProperty("CONTENT_CHANGE_NOTIFICATIONS") && !isNew)
			_sendContentletPublishNotification(currentContentlet, reqImpl.getHttpServletRequest());

		SessionMessages.add(httpReq, "message", "message.contentlet.save");
		if( subcmd != null && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH) ) {
			SessionMessages.add(httpReq, "message", "message.contentlet.published");
		}
	}

	private ArrayList<Permission> _getSelectedPermissions(ActionRequest req, Contentlet con){
		ArrayList<Permission> pers = new ArrayList<Permission>();
		String[] readPermissions = req.getParameterValues("read");
		if (readPermissions != null) {
			for (int k = 0; k < readPermissions.length; k++) {
				pers.add(new Permission(con.getInode(), readPermissions[k], PermissionAPI.PERMISSION_READ));
			}
		}

		String[] writePermissions = req.getParameterValues("write");
		if (writePermissions != null) {
			for (int k = 0; k < writePermissions.length; k++) {
				pers.add(new Permission(con.getInode(), writePermissions[k], PermissionAPI.PERMISSION_WRITE));

			}
		}

		String[] publishPermissions = req.getParameterValues("publish");
		if (publishPermissions != null) {
			for (int k = 0; k < publishPermissions.length; k++) {
				pers.add(new Permission(con.getInode(), publishPermissions[k], PermissionAPI.PERMISSION_PUBLISH));
			}
		}
		return pers;
	}

	private void _sendContentletPublishNotification (Contentlet contentlet, HttpServletRequest req) throws Exception,PortalException, SystemException {
		try
		{
			User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			Map<String, String[]> params = new HashMap<String, String[]> ();
			params.put("struts_action", new String [] {"/ext/contentlet/edit_contentlet"});
			params.put("cmd", new String [] {"edit"});
			params.put("inode", new String [] { String.valueOf(contentlet.getInode()) });
			String contentURL = PortletURLUtil.getActionURL(req, WindowState.MAXIMIZED.toString(), params);	
			List<Map<String, Object>> references = conAPI.getContentletReferences(contentlet, currentUser, false);
			List<Map<String, Object>> validReferences = new ArrayList<Map<String, Object>> ();


			//Avoinding to send the email to the same users
			for (Map<String, Object> reference : references) 
			{
				try
				{
					HTMLPage page = (HTMLPage)reference.get("page");					
					User pageUser = APILocator.getUserAPI().loadUserById(page.getModUser(),APILocator.getUserAPI().getSystemUser(),false);		
					if (!pageUser.getUserId().equals(currentUser.getUserId()))
					{
						reference.put("owner", pageUser);
						validReferences.add(reference);
					}
				}
				catch(Exception ex)
				{
					Logger.debug(this, "the reference has a null page");
				}
			}					
			if (validReferences.size() > 0) {
				ContentChangeNotificationThread notificationThread = 
					this.new ContentChangeNotificationThread (contentlet, validReferences, contentURL, hostWebAPI.getCurrentHost(req).getHostname());
				notificationThread.start();
			}			
		}
		catch(Exception ex)
		{					
			throw ex;
		}
	}

	//	Contentlet change notifications thread
	private class ContentChangeNotificationThread extends Thread {

		private String serverName;
		private String contentletEditURL;
		private Contentlet contentlet;
		private List<Map<String, Object>> references;
		private HostAPI hostAPI = APILocator.getHostAPI();
		private UserAPI userAPI = APILocator.getUserAPI();

		public ContentChangeNotificationThread (Contentlet cont, List<Map<String, Object>> references, String contentletEditURL, String serverName) {
			super ("ContentChangeNotificationThread");
			this.contentletEditURL = contentletEditURL;
			this.references = references;
			this.serverName = serverName;
			contentlet = cont;
		}

		@Override
		public void run() {
			try {
				User systemUser = userAPI.getSystemUser();
				String editorName = UtilMethods.getUserFullName(contentlet.getModUser());

				for (Map<String, Object> reference : references) {
					HTMLPage page = (HTMLPage)reference.get("page");
					Host host = hostAPI.findParentHost(page, systemUser, false);
					Company company = PublicCompanyFactory.getDefaultCompany(); 
					User pageUser = (User)reference.get("owner");

					HashMap<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("from", company.getEmailAddress());
					parameters.put("to", pageUser.getEmailAddress());
					parameters.put("subject", "dotCMS Notification");
					parameters.put("emailTemplate", Config.getStringProperty("CONTENT_CHANGE_NOTIFICATION_EMAIL_TEMPLATE"));
					parameters.put("contentletEditedURL", "http://" + serverName + contentletEditURL);
					parameters.put("contentletTitle", "Content");
					parameters.put("pageURL", "http://" + serverName + UtilMethods.encodeURIComponent(page.getURI()));
					parameters.put("pageTitle", page.getTitle());
					parameters.put("editorName", editorName);

					EmailFactory.sendParameterizedEmail(parameters, null, host, null);

				}
			} catch (Exception e) {
				Logger.error(this, "Error ocurring trying to send the content change notifications.", e);
			} finally {
				try {
					HibernateUtil.closeSession();
				} catch (DotHibernateException e) {
					Logger.error(this, "Error ocurring trying to send the content change notifications.", e);
				}
			}
		}
	}

	public void _copyWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		Contentlet currentContentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);

		//ContentletForm cf = (ContentletForm)form;
		String structureInode = currentContentlet.getStructureInode();
		if (!InodeUtils.isSet(structureInode)) {
			String selectedStructure = req.getParameter("selectedStructure");
			if (InodeUtils.isSet(selectedStructure)) {
				structureInode = selectedStructure;
			}
		}
		currentContentlet.setStructureInode(structureInode);
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
		StringBuffer uniqueFieldCopyErrors = new StringBuffer();
		boolean copyError = false;
		for (Field field : fields){
			if (field.isUnique()){
				if(!(field.getRegexCheck() == null || field.getRegexCheck().equals(""))){
					uniqueFieldCopyErrors.append(WebKeys.COPY_CONTENTLET_UNIQUE_HAS_VALIDATION + " ");
					SessionMessages.add(httpReq,"error", "message.contentlet.copy.unique.hasValidation");
					copyError = true;
				}
				if(!field.getFieldContentlet().startsWith("text")){
					uniqueFieldCopyErrors.append(WebKeys.COPY_CONTENTLET_UNIQUE_NOT_TEXT + " ");
					SessionMessages.add(httpReq,"error", "message.contentlet.copy.unique.not_text");
					copyError = true;
				}
				if(copyError){
					throw new DotContentletValidationException(uniqueFieldCopyErrors.toString());
				}
			}
		}

		Logger.debug(this, "currentContentlet Inode=" + currentContentlet.getInode());
		try {
			conAPI.unlock(currentContentlet, user, false);
			conAPI.copyContentlet(currentContentlet, user, false);
		} catch(DotSecurityException e) {
			SessionMessages.add(httpReq, "error", "message.contentlet.copy.permission.error");
			return;
		}

		// gets the session object for the messages
		SessionMessages.add(httpReq, "message", "message.contentlet.copy");
	}

	public void _getVersionBackWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form,
			User user) throws Exception {
		Contentlet conVersionToRestore = conAPI.find(req.getParameter("inode_version"), user, false);
		conAPI.restoreVersion(conVersionToRestore, user, false);
		req.setAttribute(WebKeys.CONTENTLET_EDIT , conVersionToRestore);
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void _loadForm(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,
			boolean validate) throws Exception {
		try {

			Contentlet contentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);
			ContentletForm contentletForm = (ContentletForm) form;
			contentletForm.setMap(contentlet.getMap());
			Structure structure = contentlet.getStructure();
			req.setAttribute("lang",contentlet.getLanguageId());
			if (!InodeUtils.isSet(structure.getInode())) {
				ActionRequestImpl reqImpl = (ActionRequestImpl) req;
				HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
				httpReq.getSession().setAttribute(WebKeys.Structure.STRUCTURE_TYPE, new Integer("1"));
				String selectedStructure = req.getParameter("selectedStructure");
				if (InodeUtils.isSet(selectedStructure)) {
					structure = StructureCache.getStructureByInode(selectedStructure);
				}
			}

			List structures = StructureFactory.getStructuresWithWritePermissions(user, false);
			if(!structures.contains(structure)) {
				structures.add(structure);
			}
			contentletForm.setAllStructures(structures);

			String cmd = req.getParameter(Constants.CMD);
			if ((cmd.equals("new") || !InodeUtils.isSet(contentletForm.getStructure().getInode())) && contentletForm.isAllowChange()) {
				contentletForm.setAllowChange(true);
			} else {
				contentletForm.setAllowChange(false);
			}

			if (cmd != null && cmd.equals(Constants.EDIT)) {
			    String sib= req.getParameter("sibbling");
			    Boolean populateaccept = Boolean.valueOf(req.getParameter("populateaccept"));
			    if(UtilMethods.isSet(sib) && populateaccept)
			        contentlet.setInode(sib);
			    
				//Setting categories in the contentlet form
				List<String> categoriesArr = new ArrayList<String> ();
				List<Category> cats = catAPI.getParents(contentlet, user, false);

				for (Category cat : cats) {
					categoriesArr.add(String.valueOf(cat.getInode()));
				}

				contentletForm.setCategories(categoriesArr.toArray(new String[0]));

				if(UtilMethods.isSet(sib) && populateaccept)
                    contentlet.setInode("");
			}

			if(cmd != null && (cmd.equals(Constants.ADD)))
			{
				if(structure != null)
				{
					ActionRequestImpl reqImpl = (ActionRequestImpl) req;
					HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
					HttpSession ses = httpReq.getSession();
					if(contentletForm.getStructure().getStructureType()!=Structure.STRUCTURE_TYPE_FORM){
						req.setAttribute("structure_id",String.valueOf(contentletForm.getStructureInode()));
					}
				}				
			}

		} catch (Exception ex) {
			Logger.debug(this, ex.toString());
			throw ex;
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public void downloadToExcel(HttpServletResponse response, User user, List contentletList) throws DotSecurityException{
		/*http://jira.dotmarketing.net/browse/DOTCMS-72*/
		PrintWriter pr = null;
		if(contentletList.size() > 0) {

			String[] inodes = new String[0];

			Structure st=null;
			List<Map<String, String>> contentlets = contentletList;

			List<String> contentletsInodes = new ArrayList<String>();
			Map<String, String> contentletMap;
			for (int i = 2; i < contentlets.size(); ++i) {
				Object map = contentlets.get(i);
				if(map!=null && map instanceof HashMap){
					contentletMap = contentlets.get(i);
					contentletsInodes.add(contentletMap.get("inode"));
				}
			}

			inodes = contentletsInodes.toArray(new String[0]);

			List<Contentlet> contentletsList2 = new ArrayList<Contentlet>();
			for(String inode  : inodes){

				Contentlet contentlet = new Contentlet();
				try{
					contentlet = conAPI.find(inode, user, false);
				}catch (DotDataException ex){
					Logger.error(this, "Unable to find contentlet with indoe " + inode);
				}
				contentletsList2.add(contentlet);
			}
			/*Structure, if contentletList.size() then contentletsList2 are not empty
			 * http://jira.dotmarketing.net/browse/DOTCMS-72*/
			st=(Structure)((contentletsList2.get(0)).getStructure());

			try {
				response.setContentType("application/octet-stream; charset=UTF-8");
				response.setHeader("Content-Disposition", "attachment; filename=\""+st.getName()+"_contents_" + UtilMethods.dateToHTMLDate(new java.util.Date(),"M_d_yyyy") +".csv\"");
				pr = response.getWriter();

				List<Field> stFields = FieldsCache.getFieldsByStructureInode(st.getInode());
				pr.print("Identifier");		
				pr.print(",languageCode");
				pr.print(",countryCode");
				for (Field f : stFields) {
					//we cannot export fields of these types
					if (f.getFieldType().equals(Field.FieldType.BUTTON.toString()) || 
							f.getFieldType().equals(Field.FieldType.FILE.toString()) ||
							f.getFieldType().equals(Field.FieldType.IMAGE.toString()) ||
							f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
							f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString()) ||
							f.getFieldType().equals(Field.FieldType.HIDDEN.toString()))
						continue;
					pr.print(","+(f.getFieldName().contains(",")?f.getFieldName().replaceAll(",", "&#44;"):f.getFieldName()));
					//http://jira.dotmarketing.net/browse/DOTCMS-3232
					/*if(f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
						pr.print(","+"Folder");
					}*/
				}



				pr.print("\r\n");
				for(Contentlet content :  contentletsList2 ){
					List<Category> catList = (List<Category>) catAPI.getParents(content, user, false);
					pr.print(""+content.getIdentifier()+"");
					Language lang =APILocator.getLanguageAPI().getLanguage(content.getLanguageId());
					pr.print("," +lang.getLanguageCode());
					pr.print(","+lang.getCountryCode());
					
					for (Field f : stFields) {
						try {
							//we cannot export fields of these types
							if (f.getFieldType().equals(Field.FieldType.BUTTON.toString()) || 
									f.getFieldType().equals(Field.FieldType.FILE.toString()) ||
									f.getFieldType().equals(Field.FieldType.IMAGE.toString()) ||
									f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
									f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString()) ||
									f.getFieldType().equals(Field.FieldType.HIDDEN.toString()))
								continue;

							Object value = "";
							if(conAPI.getFieldValue(content,f) != null)  
								value = conAPI.getFieldValue(content,f);
							String text = "";
							if(f.getFieldType().equals(Field.FieldType.CATEGORY.toString())){

								Category category = catAPI.find(f.getValues(), user, false);
								List<Category> children = catList;
								List<Category> allChildren= catAPI.getAllChildren(category, user, false);
								

								if (children.size() >= 1 && catAPI.canUseCategory(category, user, false)) {
									//children = (List<Category>)CollectionUtils.retainAll(catList, children);
									for(Category cat : children){
										if(allChildren.contains(cat)){
											if(UtilMethods.isSet(cat.getKey())){
												text = text+","+cat.getKey();
											}else{
												text = text+","+cat.getCategoryName();
											}
										}
									}
								}
								if(UtilMethods.isSet(text)){
									text=text.substring(1);
								}
							}else{

								if (value instanceof Date || value instanceof Timestamp) {
									if(f.getFieldType().equals(Field.FieldType.DATE.toString())) {
										SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_DATE);
										text = formatter.format(value);
									} else if(f.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
										SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_DATETIME);
										text = formatter.format(value);
									} else if(f.getFieldType().equals(Field.FieldType.TIME.toString())) {
										SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_TIME);
										text = formatter.format(value);
									}                                    
								} else {
									text = value.toString();
									if(text.endsWith(",")){
										text = text.substring (0, text.length()-1);
									}
								}

							}
							//Windows carriage return conversion
							text = text.replaceAll("\r","");

							//Cutting because an excel limitation of 31500 chars per cell
							//I put it commented it out because it drives to lose of data but if
							//a cell
							//if(text.length() > 31500)
							//	text = text.substring(0, 31500);

							if(text.contains(",") || text.contains("\n")) {
								//Double quotes replacing
								text = text.replaceAll("\"","\"\"");
								pr.print(",\""+text+"\"");
							} else
								pr.print(","+text);
						//http://jira.dotmarketing.net/browse/DOTCMS-3232	
						/*if(f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
							if(FolderAPI.SYSTEM_FOLDER.equals(content.getFolder()))
							  pr.print(content.getHost());
							else
							  pr.print(content.getFolder());
						}*/ //DOTCMS-4710

						}catch(Exception e){
							pr.print(",");
							Logger.error(this,e.getMessage(),e);
						}
					}


					pr.print("\r\n");
				}

				pr.flush();
				pr.close();
				HibernateUtil.closeSession();

			}catch(Exception p){
				Logger.error(this,p.getMessage(),p);
			}
		}
		else {
			try {pr.print("\r\n");} catch (Exception e) {	Logger.debug(this,"Error: download to excel "+e);	}
		}
	}


	

	/**
	 * Returns the relationships associated to the current contentlet
	 * 
	 * @param		req ActionRequest.
	 * @param		user User.
	 * @return		ContentletRelationships.
	 */
	private ContentletRelationships getCurrentContentletRelationships(ActionRequest req, User user) {
		List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationships.ContentletRelationshipRecords>();
		Set<String> keys = req.getParameterMap().keySet();
		ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords;
		boolean hasParent;
		String inodesSt;
		String[] inodes;
		Relationship relationship;
		String inode;
		Contentlet contentlet;
		ContentletAPI contentletAPI = APILocator.getContentletAPI();
		List<Contentlet> records = null; 

		for (String key : keys) {
			if (key.startsWith("rel_") && key.endsWith("_inodes")) {
				hasParent = key.indexOf("_P_") != -1;
				inodesSt = (String) req.getParameter(key);
				inodes = inodesSt.split(",");
				relationship = (Relationship) InodeFactory.getInode(inodes[0], Relationship.class);
				contentletRelationshipRecords = new ContentletRelationships(null).new ContentletRelationshipRecords(relationship, hasParent);
				records = new ArrayList<Contentlet>();

				for (int i = 1; i < inodes.length; i++) {
					try {
						inode = inodes[i];
						contentlet = contentletAPI.find(inode, user, false);
						if ((contentlet != null) && (InodeUtils.isSet(contentlet.getInode())))
							records.add(contentlet);
					} catch (Exception e) {
						Logger.warn(this, e.toString());
					}
				}

				contentletRelationshipRecords.setRecords(records);
				relationshipsRecords.add(contentletRelationshipRecords);
			}
		}

		ContentletRelationships result = new ContentletRelationships((Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT), relationshipsRecords);

		return result;
	}

	private ContentletRelationships retrieveRelationshipsData(Contentlet currentcontent, User user, ActionRequest req ){

		Set<String> keys = req.getParameterMap().keySet();

		ContentletRelationships relationshipsData = new ContentletRelationships(currentcontent);
		List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationshipRecords> ();
		relationshipsData.setRelationshipsRecords(relationshipsRecords);

		for (String key : keys) {
			if (key.startsWith("rel_") && key.endsWith("_inodes")) {
				boolean hasParent = key.contains("_P_");
				String inodesSt = (String) req.getParameter(key);

				String[] inodes = inodesSt.split(",");

				Relationship relationship = (Relationship) InodeFactory.getInode(inodes[0], Relationship.class);
				ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
				ArrayList<Contentlet> cons = new ArrayList<Contentlet>();
				for (String inode : inodes) {
					String i = "";
					try{
						i = inode;
					}catch (Exception e) {
						Logger.error(this, "Relationship not a number value : ",e);
					}
					if(relationship.getInode().equalsIgnoreCase(i)){
						continue;
					}
					try{
						cons.add(conAPI.find(inode, user, false));
					}catch(Exception e){
						Logger.debug(this,"Couldn't look up contentlet.  Assuming inode" + inode + "is not content");
					}
				}
				records.setRecords(cons);
				relationshipsRecords.add(records);
			}
		}

		return relationshipsData;

	}

	//Batch operations
	private void _batchUnpublish(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Unpublish Method");
			String [] inodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				inodes = getSelectedInodes(req,user);
			}



			class UnpublishThread extends Thread {
				private String[] inodes = new String[0];
				private User user;
				List<Contentlet> contentToIndexAfterCommit = new ArrayList<Contentlet>();

				public UnpublishThread(String[] inodes, User user,List<Contentlet> contentToIndexAfterCommit) {
					this.inodes = inodes;
					this.user = user;
					this.contentToIndexAfterCommit = contentToIndexAfterCommit;
				}

				public void run() {
					try {
						unpublish(contentToIndexAfterCommit);
					} catch (DotContentletStateException e) {
					} catch (DotSecurityException e) {
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
					}

				}

				public void unpublish(List<Contentlet> contentToIndexAfterCommit) throws DotContentletStateException, DotStateException, DotSecurityException, DotDataException {
					boolean hasNoPermissionOnAllContent = false;
					List<Contentlet> contentlets = new ArrayList<Contentlet>();
					for(String inode  : inodes){

						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
							contentToIndexAfterCommit.add(contentlet);
						
						}catch (DotSecurityException e) {
							hasNoPermissionOnAllContent = true;
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, user)) {
							contentlets.add(contentlet);
						} else
							hasNoPermissionOnAllContent = true;
					}
					try{
						boolean stateError = false;
						for (Contentlet contentlet : contentlets) {
							HibernateUtil.startTransaction();
							try{
								conAPI.unpublish(contentlet, user, false);
								HibernateUtil.commitTransaction();
								ActivityLogger.logInfo(this.getClass(), "Unublish contentlet action", " User " + user.getFirstName() + " Unpublished content titled '" + contentlet.getTitle()
										+ "' ", currentHost);
							}catch (DotContentletStateException e) {
								stateError = true;
							}catch(DotDataException de){
								HibernateUtil.rollbackTransaction();
								throw de;
							}finally{
								HibernateUtil.closeSession();
							}
						}
						if(stateError){
							throw new DotContentletStateException("Unable to unpublish one or more contentlets because it is locked");
						}
					}catch (DotSecurityException e) {
						hasNoPermissionOnAllContent = true;
					}
					if(hasNoPermissionOnAllContent)
						throw new DotSecurityException("Unable to unpublish some content due to lack of permissions");
				}
			}

			UnpublishThread thread = new UnpublishThread(inodes, user,contentToIndexAfterCommit);

			if (inodes.length > 50) {

				// Starting the thread
				thread.start();
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.unpublishing.background");

			} else {

				try {

					// Executing synchronous because there is not that many
					thread.unpublish(contentToIndexAfterCommit);
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unpublished");

				} catch (DotContentletStateException e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unpublish.notlive_or_locked");
				} catch (DotSecurityException dse) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unpublish.nopermissions");
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unpublish.error");
				}

			}

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}		
	}


	private void _batchPublish(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Publish Method");
			String [] inodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				inodes = getSelectedInodes(req,user);
			}



			class PublishThread extends Thread {
				private String[] inodes = new String[0];
				private User user;
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<Contentlet>();

				public PublishThread(String[] inodes, User user,List<Contentlet> contentToIndexAfterCommit) {
					this.inodes = inodes;
					this.user = user;
					this.contentToIndexAfterCommit = contentToIndexAfterCommit;
				}

				public void run() {
					try {
						publish(contentToIndexAfterCommit);
					} catch (DotContentletStateException e) {
					} catch (DotSecurityException e) {
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
					}

				}

				public void publish(List<Contentlet> contentToIndexAfterCommit) throws DotContentletStateException, DotStateException, DotSecurityException, DotDataException {
					boolean hasNoPermissionOnAllContent = false;
					List<Contentlet> contentlets = new ArrayList<Contentlet>();
					for(String inode  : inodes){

						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
							contentToIndexAfterCommit.add(contentlet);
							
							if(contentlet.isLive()){
								continue;
							}
						}catch (DotSecurityException e) {
							hasNoPermissionOnAllContent = true;
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, user) && !contentlet.isLive()) {
							contentlets.add(contentlet);
						} else
							hasNoPermissionOnAllContent = true;
					}
					try{
						boolean stateError = false;
						for (Contentlet contentlet : contentlets) {
							HibernateUtil.startTransaction();
							try{
								conAPI.publish(contentlet, user, false);
								ActivityLogger.logInfo(this.getClass(), "Publish contentlet action", " User " + user.getFirstName() + " Published content titled '" + contentlet.getTitle()
										+ "' ", currentHost);
								HibernateUtil.commitTransaction();
							}catch (DotContentletStateException e) {
								stateError = true;
							}catch(DotDataException de){
								HibernateUtil.rollbackTransaction();
								throw de;
							}finally{
								HibernateUtil.closeSession();
							}
						}
						if(stateError){
							throw new DotContentletStateException("Unable to publish one or more contentlets because it is locked");
						}
					}catch (DotSecurityException e) {
						hasNoPermissionOnAllContent = true;
					}
					if(hasNoPermissionOnAllContent)
						throw new DotSecurityException("Unable to publish some content due to lack of permissions");
				}
			}

			PublishThread thread = new PublishThread(inodes, user,contentToIndexAfterCommit);

			if (inodes.length > 50) {

				// Starting the thread
				thread.start();
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.publishing.background");

			} else {

				try {

					// Executing synchronous because there is not that many
					thread.publish(contentToIndexAfterCommit);
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.published");

				} catch (DotContentletStateException e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.publish.locked");
				} catch (DotSecurityException dse) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.publish.nopermissions");
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.publish.error");
				}

			}

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}		
	}




	private void _batchArchive(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Archive Method");
			String [] inodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				inodes = getSelectedInodes(req,user);
			}



			class ArchiveThread extends Thread {
				private String[] inodes = new String[0];
				private User user;
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<Contentlet>();

				public ArchiveThread(String[] inodes, User user,List<Contentlet> contentToIndexAfterCommit) {
					this.inodes = inodes;
					this.user = user;
					this.contentToIndexAfterCommit = contentToIndexAfterCommit;
				}

				public void run() {
					try {
						archive(contentToIndexAfterCommit);
					} catch (DotContentletStateException e) {
					} catch (DotSecurityException e) {
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
					}

				}

				public void archive(List<Contentlet> contentToIndexAfterCommit) throws DotContentletStateException, DotStateException, DotSecurityException, DotDataException {
					boolean hasNoPermissionOnAllContent = false;
					boolean someContentIsLive = false;
					List<Contentlet> contentlets = new ArrayList<Contentlet>();
					for(String inode  : inodes){

						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
							contentToIndexAfterCommit.add(contentlet);
						}catch (DotSecurityException e) {
							hasNoPermissionOnAllContent = true;
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user))
							if (!contentlet.isLive())
								contentlets.add(contentlet);
							else {
							    someContentIsLive=true;
							    contentToIndexAfterCommit.remove(contentlet);
							}
						else {
							hasNoPermissionOnAllContent = true;
							contentToIndexAfterCommit.remove(contentlet);
						}
					}
					try{
						boolean stateError = false;
						for (Contentlet contentlet : contentlets) {
							HibernateUtil.startTransaction();
							try{
								conAPI.archive(contentlet, user, false);
								ActivityLogger.logInfo(this.getClass(), "Archieve contentlet action", " User " + user.getFirstName() + " Archieved content titled '" + contentlet.getTitle()
										+ "' ", currentHost);
								HibernateUtil.commitTransaction();
							}catch (DotContentletStateException e) {
								stateError = true;
								contentToIndexAfterCommit.remove(contentlet);
							}catch(DotDataException de){
								HibernateUtil.rollbackTransaction();
								throw de;
							}finally{
								HibernateUtil.closeSession();
							}
						}
						if(stateError){
							throw new DotContentletStateException("Unable to archive one or more contentlets because it is locked");
						}
					}catch (DotSecurityException e) {
						hasNoPermissionOnAllContent = true;
					}
					if(hasNoPermissionOnAllContent)
						throw new DotSecurityException("Unable to archive some content due to lack of permissions");
					if(someContentIsLive)
					    throw new DotContentletStateException("Unable to archive some content because they are live");
				}
			}

			ArchiveThread thread = new ArchiveThread(inodes, user,contentToIndexAfterCommit);

			if (inodes.length > 50) {

				// Starting the thread
				thread.start();
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.archiving.background");

			} else {

				try {

					// Executing synchronous because there is not that many
					thread.archive(contentToIndexAfterCommit);
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.archived");

				} catch (DotContentletStateException e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.archived.live_or_locked");
				} catch (DotSecurityException dse) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.archive.nopermissions");
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.archive.error");
				}

			}

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}		
	}


	private void _batchDelete(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Delete Method");
			String [] inodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				inodes = getSelectedInodes(req,user);
			}



			class DeleteThread extends Thread {
				private String[] inodes = new String[0];
				private User user;
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<Contentlet>();

				public DeleteThread(String[] inodes, User user,List<Contentlet> contentToIndexAfterCommit) {
					this.inodes = inodes;
					this.user = user;
					this.contentToIndexAfterCommit = contentToIndexAfterCommit;
				}

				public void run() {
					try {
						delete(contentToIndexAfterCommit);
					} catch (DotContentletStateException e) {
					} catch (DotSecurityException e) {
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
					}

				}

				public void delete(List<Contentlet> contentToIndexAfterCommit) throws DotContentletStateException, DotStateException, DotSecurityException, DotDataException {
					boolean hasNoPermissionOnAllContent = false;
					List<Contentlet> contentlets = new ArrayList<Contentlet>();
					for(String inode  : inodes){
						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
							contentToIndexAfterCommit.add(contentlet);
						}catch (DotSecurityException e) {
							hasNoPermissionOnAllContent = true;
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user) && !contentlet.isLive()) {

							contentlets.add(contentlet);
						} else
							hasNoPermissionOnAllContent = true;
					}
					List<Contentlet> cons = new ArrayList<Contentlet>();
					for (Contentlet content : contentlets) {
						cons.clear();
						cons.add(content);
						try{
							conAPI.delete(cons, user, false);
						}catch (DotSecurityException e) {
							Logger.warn(this, "Unable to delete content because of a lack of permissions" + e.getMessage(), e);
							hasNoPermissionOnAllContent = true;
						}catch (Exception e) {
							Logger.warn(this, "Unable to delete content " + e.getMessage(), e);
						}
					}
				
				if(hasNoPermissionOnAllContent)
					throw new DotSecurityException("Unable to delete some content due to lack of permissions");
				}
			}

			DeleteThread thread = new DeleteThread(inodes, user,contentToIndexAfterCommit);

			if (inodes.length > 50) {

				// Starting the thread
				thread.start();
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.deleting.background");

			} else {

				try {

					// Executing synchronous because there is not that many
					thread.delete(contentToIndexAfterCommit);
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.deleted");

				} catch (DotContentletStateException e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.deleted.live_or_locked");
				} catch (DotSecurityException dse) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.deleted.nopermissions");
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.deleted.error");
				}

			}

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}		
	}

	private void _batchUnArchive(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Unarchive Method");
			String [] inodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				inodes = getSelectedInodes(req,user);
			}



			class UnarchiveThread extends Thread {
				private String[] inodes = new String[0];
				private User user;
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<Contentlet>();

				public UnarchiveThread(String[] inodes, User user,List<Contentlet> contentToIndexAfterCommit) {
					this.inodes = inodes;
					this.user = user;
					this.contentToIndexAfterCommit = contentToIndexAfterCommit;
				}

				public void run() {
					try {
						unarchive(contentToIndexAfterCommit);
					} catch (DotContentletStateException e) {
					} catch (DotSecurityException e) {
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
					}

				}

				public void unarchive(List<Contentlet> contentToIndexAfterCommit) throws DotContentletStateException, DotStateException, DotSecurityException, DotDataException {
					boolean hasNoPermissionOnAllContent = false;
					List<Contentlet> contentlets = new ArrayList<Contentlet>();
					for(String inode  : inodes){

						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
							contentToIndexAfterCommit.add(contentlet);
						}catch (DotSecurityException e) {
							hasNoPermissionOnAllContent = true;
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user)) {
							contentlets.add(contentlet);
						} else
							hasNoPermissionOnAllContent = true;
					}
					try{
						boolean stateError = false;
						for (Contentlet contentlet : contentlets) {
							HibernateUtil.startTransaction();
							try{
								conAPI.unarchive(contentlet, user, false);
								ActivityLogger.logInfo(this.getClass(), "Unarchieve contentlet action", " User " + user.getFirstName() + " Unarchieved content titled '" + contentlet.getTitle()
										+ "' ", currentHost);
								HibernateUtil.commitTransaction();
							}catch (DotContentletStateException e) {
								stateError = true;
							}catch(DotDataException de){
								HibernateUtil.rollbackTransaction();
								throw de;
							}finally{
								HibernateUtil.closeSession();
							}
						}
						if(stateError){
							throw new DotContentletStateException("Unable to unarchive one or more contentlets because it is locked");
						}
					}catch (DotSecurityException e) {
						hasNoPermissionOnAllContent = true;
					}
					if(hasNoPermissionOnAllContent)
						throw new DotSecurityException("Unable to unarchive some content due to lack of permissions");
				}
			}

			UnarchiveThread thread = new UnarchiveThread(inodes, user,contentToIndexAfterCommit);

			if (inodes.length > 50) {

				// Starting the thread
				thread.start();
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.unarchiving.background");

			} else {

				try {

					// Executing synchronous because there is not that many
					thread.unarchive(contentToIndexAfterCommit);
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unarchive");

				} catch (DotContentletStateException e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unarchive.locked");
				} catch (DotSecurityException dse) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unarchive.nopermissions");
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unarchive.error");
				}

			}

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}
	}

	private void _batchReindex(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		Logger.debug(this, "Calling Batch Reindex Method");
		String [] inodes = req.getParameterValues("publishInode");

		if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
			inodes = getSelectedInodes(req,user);
		}



		class ReindexThread extends Thread {
			private String[] inodes = new String[0];

			public ReindexThread(String[] inodes) {
				this.inodes = inodes;
			}

			public void run() {
				try {
					reindex();
				} catch (DotContentletStateException e) {
					Logger.error(this, e.getMessage(), e);
				} catch (DotSecurityException e) {
					Logger.error(this, e.getMessage(), e);
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}

			}

			public void reindex() throws DotContentletStateException, DotStateException, DotSecurityException, DotDataException {
				//DistributedJournalAPI jAPI = APILocator.getDistributedJournalAPI();
				for(String inode  : inodes){

					Contentlet contentlet = new Contentlet();
					try{
						contentlet = conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), false);
						if(contentlet != null && UtilMethods.isSet(contentlet.getInode())){
							contentlet.setLowIndexPriority(true);
							conAPI.reindex(contentlet);
						}else{
							Logger.error(this, "Unable to find contentlet with inode " + inode);
							//new ESIndexAPI().removeContentFromIndex(contentlet);
							// TODO: implement a way to clean the index in this case
							continue;
						}
					}catch (DotDataException ex){
						Logger.error(this, "Unable to find contentlet with inode " + inode);
						//jAPI.addContentIndexEntryToDelete(inode);
						continue;
					}
				}
			}
		}

		ReindexThread thread = new ReindexThread(inodes);

		if (inodes.length > 50) {
			// Starting the thread
			thread.start();
			SessionMessages.add(httpReq, "message", "message.contentlets.batch.reindexing.background");

		} else {
			try {
				// Executing synchronous because there is not that many
				thread.reindex();
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.reindexing.background");
			} catch (Exception e) {
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.reindexing.error");
			}	
		}
	}

	private void reindexContentlets(List<Contentlet> contentToIndexAfterCommit,String cmd){
	    ESIndexAPI indexAPI = new ESIndexAPI();
		for (Contentlet con : contentToIndexAfterCommit) {
			try {
				Identifier ident=APILocator.getIdentifierAPI().find(con);
				if(ident!=null && UtilMethods.isSet(ident.getId()))
				    indexAPI.addContentToIndex(con);
				else
				    indexAPI.removeContentFromIndex(con);
			} catch (DotDataException e) {
				Logger.error(this, e.getMessage(),e);
			}
		}
		
		//DOTCMS-4614
		if (contentToIndexAfterCommit.size() <= 50 
				&&	(cmd != null) 
				&&	(cmd.equals(com.dotmarketing.util.Constants.FULL_ARCHIVE_LIST)	
						||	cmd.equals(com.dotmarketing.util.Constants.FULL_UNARCHIVE_LIST) )){
			
			String addlQry = "";
			if(cmd.equals(com.dotmarketing.util.Constants.FULL_ARCHIVE_LIST))
				addlQry = " +deleted:true ";
			if(cmd.equals(com.dotmarketing.util.Constants.FULL_UNARCHIVE_LIST))
				addlQry = " +deleted:false ";
			
			for(Contentlet c : contentToIndexAfterCommit){
				conAPI.isInodeIndexed(c.getInode()+addlQry);
			}
		}
		
	}

	/* http://jira.dotmarketing.net/browse/DOTCMS-72*/
	private List searchContentlets(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String from){
		try {
			String structureInode;
			String fieldsValues;
			String categoriesValues;

			if(from.equals("Excel")){
				structureInode = req.getParameter("expStructureInode");
				fieldsValues= req.getParameter("expFieldsValues");
				categoriesValues= req.getParameter("expCategoriesValues");
			}

			else{
				structureInode = req.getParameter("structureInode");
				fieldsValues= req.getParameter("fieldsValues");
				categoriesValues= req.getParameter("categoriesValues");
			}

			String showDeleted = req.getParameter("showDeleted");
			String filterSystemHost = req.getParameter("filterSystemHost");
			String filterLocked = req.getParameter("filterLocked");
			String currentSortBy = req.getParameter("currentSortBy");
			String modDateFrom = req.getParameter("modDateFrom");
			String modDateTo = req.getParameter("modDateTo");

			List<String> listFieldsValues = new ArrayList<String>();
			if (UtilMethods.isSet(fieldsValues)) {
				String[] fieldsValuesArray = fieldsValues.split(",");
				for (String value: fieldsValuesArray) {
					listFieldsValues.add(value);
				}
			}

			List<String> listCategoriesValues = new ArrayList<String>();
			if (UtilMethods.isSet(categoriesValues)) {
				String[] categoriesValuesArray = categoriesValues.split(",");
				for (String value: categoriesValuesArray) {
					if(UtilMethods.isSet(value)) {
						listCategoriesValues.add(value);
					}
				}
			}

			ContentletAjax contentletAjax = new ContentletAjax();
			List<Map<String, String>> contentlets = contentletAjax.searchContentletsByUser(structureInode, listFieldsValues, listCategoriesValues, Boolean.parseBoolean(showDeleted), Boolean.parseBoolean(filterSystemHost), Boolean.parseBoolean(filterLocked), 0, currentSortBy, 100000, user, null, modDateFrom, modDateTo);
			return contentlets;
		} catch (Exception e) {
			Logger.debug(this, "Error: searchContentlets (EditContentletAction ): "+e);
			return null;
		}

	} 

	public void _deleteVersion(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKeyEdit)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//WebAsset webAsset = (WebAsset) req.getAttribute(webKeyEdit);
		com.dotmarketing.portlets.contentlet.model.Contentlet webAsset = (com.dotmarketing.portlets.contentlet.model.Contentlet)req.getAttribute(webKeyEdit);

		ContentletAPI conAPI;
		conAPI = APILocator.getContentletAPI();
		ActionResponseImpl resImpl = (ActionResponseImpl) res;
		// calls the Contentlet API delete the container version
		try{
			//conAPI.delete(webAsset, user, false, false);
			conAPI.deleteVersion(webAsset,user,false);
			

		}catch(Exception e){
			resImpl.getHttpServletResponse().getWriter().println("FAILURE:" + LanguageUtil.get(user, "message.contentlet.delete.live_or_working"));
		}
	}

	/* http://jira.dotmarketing.net/browse/DOTCMS-5986*/
	private String[] getSelectedInodes(ActionRequest req, User user){
	    String[] allInodes = new String[0];
        String[] uncheckedInodes = new String[0];
        String[] result;
        ArrayList<String> resultInodes = new ArrayList<String>();
	    
	    if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
	        String luceneQuery=req.getParameter("luceneQuery");
	        try {
                List<ContentletSearch> list=conAPI.searchIndex(luceneQuery, -1, -1, null, user, false);
                allInodes=new String[list.size()];
                int idx=0;
                for(ContentletSearch cs : list)
                    allInodes[idx++]=cs.getInode();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } 
	    }
		
		String allUncheckedContentInodes = req.getParameter("allUncheckedContentsInodes");
		 
		
		if(!allUncheckedContentInodes.equals(""))
			uncheckedInodes = allUncheckedContentInodes.split(",");		
		
		for (String str:allInodes) {
			boolean found = false;
			for (String str1:uncheckedInodes) {
				if(str.equals(str1))
					found = true;
			}
			if(!found)
				resultInodes.add(str);
		}
		
		result = new String[resultInodes.size()];
		result = resultInodes.toArray(result);
		
		return result;
	}
	
	
	private void _batchUnlock(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Unlock Method");
			String [] inodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				inodes = getSelectedInodes(req,user);
			}



			class UnlockThread extends Thread {
				private String[] inodes = new String[0];
				private User user;
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<Contentlet>();

				public UnlockThread(String[] inodes, User user,List<Contentlet> contentToIndexAfterCommit) {
					this.inodes = inodes;
					this.user = user;
					this.contentToIndexAfterCommit = contentToIndexAfterCommit;
				}

				public void run() {
					try {
						unlock(contentToIndexAfterCommit);
					} catch (DotContentletStateException e) {
					} catch (DotSecurityException e) {
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
					}

				}

				public void unlock(List<Contentlet> contentToIndexAfterCommit) throws DotContentletStateException, DotStateException, DotSecurityException, DotDataException {
					List<Contentlet> contentlets = new ArrayList<Contentlet>();
					for(String inode  : inodes){

						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
							contentToIndexAfterCommit.add(contentlet);
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user)) {
							contentlets.add(contentlet);
						} 
					}
					boolean securityError = false;
					boolean notLocked = false;
					for (Contentlet contentlet : contentlets) {
						HibernateUtil.startTransaction();
						try{
							conAPI.unlock(contentlet, user, false);
							HibernateUtil.commitTransaction();
						}catch (DotStateException e) {
							notLocked = true;
							HibernateUtil.rollbackTransaction();
						}catch (DotSecurityException e) {
							securityError = true;	
							HibernateUtil.rollbackTransaction();
						}catch(DotDataException de){
							HibernateUtil.rollbackTransaction();
							throw de;
						}finally{
							HibernateUtil.closeSession();
						}
					}
					if(securityError)
						throw new DotSecurityException("Unable to unlock some content due to lack of permissions");
					
					if(notLocked)
						throw new DotContentletStateException("Unable to unlock some content because they were not locked");

				}
			}

			UnlockThread thread = new UnlockThread(inodes, user,contentToIndexAfterCommit);

			if (inodes.length > 50) {

				// Starting the thread
				thread.start();
				SessionMessages.add(httpReq, "message", "message.contentlets.batch.unlock.background");

			} else {

				try {

					// Executing synchronous because there is not that many
					thread.unlock(contentToIndexAfterCommit);
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unlock");

				} catch (DotContentletStateException e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unlock.notlocked");
				} catch (DotSecurityException dse) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unlock.nopermissions");
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.unlock.error");
				}

			}

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}
	}
}
