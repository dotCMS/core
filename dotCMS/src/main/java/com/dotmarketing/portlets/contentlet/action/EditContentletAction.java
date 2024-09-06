package com.dotmarketing.portlets.contentlet.action;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.action.ActionMessage;
import com.dotcms.util.I18NMessage;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PublishStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portal.struts.DotPortletActionInterface;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjax;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.contentlet.struts.EventAwareContentletForm;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.LocaleUtil;
import com.liferay.util.StringPool;
import com.liferay.util.servlet.SessionMessages;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dotmarketing.portlets.calendar.action.EventFormUtils.editEvent;
import static com.dotmarketing.portlets.calendar.action.EventFormUtils.setEventDefaults;
import static com.dotmarketing.portlets.contentlet.util.ContentletUtil.isNewFieldTypeAllowedOnImportExport;

/**
 * This class processes all the interactions with contentlets that are
 * originated from the Content Search portlet.
 * 
 * @author root
 * @version 1.0
 * @since May 22, 2012
 *
 */
public class EditContentletAction extends DotPortletAction implements DotPortletActionInterface {

	private Contentlet contentletToEdit;
	
	private CategoryAPI catAPI;
	private PermissionAPI perAPI;
	private ContentletAPI conAPI;
	private FieldAPI fAPI;
	private HostWebAPI hostWebAPI;
	private TagAPI tagAPI;
	private String currentHost;
	private NotificationAPI notificationAPI;
	private UserAPI userAPI;
	private RoleAPI roleAPI;
	private EventAPI eventAPI;

	private final boolean DONT_RESPECT_FRONTEND_ROLES = Boolean.FALSE;

	/**
	 * Default constructor that initializes all the required dotCMS APIs.
	 */
	public EditContentletAction() {
		this( APILocator.getCategoryAPI(),
		APILocator.getPermissionAPI(),
		APILocator.getContentletAPI(),
		APILocator.getFieldAPI(),
		WebAPILocator.getHostWebAPI(),
		APILocator.getTagAPI(),
		APILocator.getNotificationAPI(),
		APILocator.getUserAPI(),
		APILocator.getRoleAPI(),
		APILocator.getEventAPI());
	}
	
	@VisibleForTesting
	public EditContentletAction(final CategoryAPI catAPI,
						 final PermissionAPI perAPI,
						 final ContentletAPI conAPI,
						 final FieldAPI fAPI,
						 final HostWebAPI hostWebAPI,
						 final TagAPI tagAPI,
						 final NotificationAPI notificationAPI,
                         final UserAPI userAPI,
						 final RoleAPI roleAPI,
						 final EventAPI eventAPI) {
		this.catAPI = catAPI;
		this.perAPI = perAPI;
		this.conAPI = conAPI;
		this.fAPI= fAPI;
		this.hostWebAPI = hostWebAPI;
		this.tagAPI = tagAPI;
		this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
		this.roleAPI = roleAPI;
		this.eventAPI = eventAPI;
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @throws IOException
	 * @throws LanguageException
	 */
	@CloseDBIfOpened
	public void buildFakeAjaxResponse(ActionRequest req, ActionResponse res) throws IOException, LanguageException{
	  try {
        HibernateUtil.closeAndCommitTransaction();
      } catch (DotHibernateException e) {
        try {
          HibernateUtil.rollbackTransaction();
        } catch (DotHibernateException e1) { }
    }
      HttpServletResponse response = ((ActionResponseImpl)res).getHttpServletResponse();
      User user = super._getUser((ActionRequest) req);
      
      Writer out = response.getWriter();

      if(SessionMessages.get(req, "message") !=null){
        String message =  (String) SessionMessages.get(req, "message");
        out.append("<script>");
        out.append("parent.showDotCMSSystemMessage(\"");
        out.append(UtilMethods.javaScriptify(LanguageUtil.get(user, message)));
        out.append("\");");
        out.append("</script>");
      }
      if(SessionMessages.get(req, "error") !=null){
        String error =  (String) SessionMessages.get(req, "error");
        out.append("<script>");
        out.append("parent.showDotCMSErrorMessage(\"");
        out.append(UtilMethods.javaScriptify(LanguageUtil.get(user, error)));
        out.append("\");");
        out.append("</script>");
      }
      
      out.append("<script>parent.fakeAjaxCallback();</script>");
      out.close();
      return;
	}

	/**
	 * Handles all the actions associated to contentlets.
	 *
	 * @param mapping
	 *            -
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @throws Exception
	 *             An error occurred when interacting with contentlets.
	 */
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
			ActionResponse res) throws Exception {
		List<Contentlet> contentToIndexAfterCommit  = new ArrayList<>();
		// wraps request to get session object
		boolean validate = true;
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		HttpSession ses = httpReq.getSession();

		Logger.debug(this, "############################# Contentlet");

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");
		
		if ((referer != null) && (referer.length() != 0)) {
			referer = URLDecoder.decode(referer, "UTF-8");
		}

		Logger.debug(this, "EditContentletAction cmd=" + cmd);
		
		User user = _getUser(req);

		if(user ==null || !user.isBackendUser()){
		  Logger.warn(this.getClass(), "User is not set or user does not have access to portlet:" + reqImpl.getPortletName());
		  _sendToReferral(req, res, "/api/v1/logout");
		  return;
		}
		
		HibernateUtil.startTransaction();

		// retrieve current host
		currentHost = HostUtil.hostNameUtil(req, user);


		int structureType = req.getParameter("contentStructureType") == null ? 0:Integer.valueOf(req.getParameter("contentStructureType"));
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
				handleEditAssetException(ae, req, res, referer, httpReq);
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
				handleEditAssetException(ae, req, res, referer, httpReq);
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
					HibernateUtil.closeAndCommitTransaction();
					reindexContentlets(contentToIndexAfterCommit,cmd);
					//This is called to preserve the values submitted in the form
					//in case of a validation error
					_loadForm(req, res, config, form, user, false);
					setForward(req, "portlet.ext.contentlet.edit_contentlet");
					return;
				}catch (Exception ce) {
					SessionMessages.add(req, "message.contentlet.save.error");
					_loadForm(req, res, config, form, user, false);
					HibernateUtil.closeAndCommitTransaction();
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
						_addToParents(req);
					} catch (Exception ae) {
						_handleException(ae, req);
						return;
					}
				}
				((ContentletForm)form).setMap(((Contentlet)req.getAttribute(WebKeys.CONTENTLET_FORM_EDIT)).getMap());
				try {
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

				List<Contentlet> contentlets = new ArrayList<>();
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
                    if (conAPI.delete(contentlet, user, false)) {
                        SessionMessages.add(httpReq, "message", "message.contentlet.full_delete");
                    } else {
                        SessionMessages.add(httpReq, "error",
                                "message.contentlet.delete.archived.error");
                    }
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
					contentletToEdit.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
					conAPI.unpublish(contentletToEdit, user, false);
					ActivityLogger.logInfo(this.getClass(), "Unpublishing Contentlet "," User "+user.getFirstName()+" Unpublished content titled '"+contentletToEdit.getTitle()+"'", HostUtil.hostNameUtil(req, user));
					SessionMessages.add(httpReq, "message", "message.contentlet.unpublished");
				}catch(DotLockException dlock){
					SessionMessages.add(httpReq, "error", "message.contentlet.cannot.be.unlocked");
				}catch(DotContentletStateException dcse){
					SessionMessages.add(httpReq, "message", "message.contentlet.unpublish.notlive_or_locked");
				}catch (PublishStateException e) {
					SessionMessages.add(httpReq, "message", e.getMessage());
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

			HashMap<String, String[]> params = new HashMap<>();
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
				final String contentTypeId = req.getParameter("expStructureInode");
				final ActionResponseImpl resImpl = (ActionResponseImpl) res;
				final HttpServletResponse response = resImpl.getHttpServletResponse();

                final Structure contentType = CacheLocator.getContentTypeCache().getStructureByInode(contentTypeId);
                final String startMsg = String.format("Exporting contents of type '%s' to CSV file. Please wait...",
                        contentType.getName());
				final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
				final SystemMessage systemMessage = systemMessageBuilder.setMessage(startMsg).setType(MessageType
						.SIMPLE_MESSAGE).setSeverity(MessageSeverity.INFO).setLife(10000).create();
				SystemMessageEventUtil.getInstance().pushMessage(systemMessage, ImmutableList.of(user.getUserId()));
                Logger.info(this, String.format("Exporting contents of type ID '%s' to CSV file. Retrieving results" +
                        " from ES index...", contentTypeId));
				downloadToExcel(response, user,searchContentlets(req,res,config,form,user,"Excel"), contentTypeId);
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
            buildFakeAjaxResponse(req, res);
            return;
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
			buildFakeAjaxResponse(req, res);
			return;
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
            buildFakeAjaxResponse(req, res);
            return;
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
            buildFakeAjaxResponse(req, res);
            return;
		}
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_REINDEX_LIST))
		{
			try {
				_batchReindex(req, res, config, form, user);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
            buildFakeAjaxResponse(req, res);
            return;
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
            buildFakeAjaxResponse(req, res);
            return;
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
            buildFakeAjaxResponse(req, res);
            return;
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
			catch(DotWorkflowException we) {
			    SessionMessages.add(httpReq, "error", "message.contentlet.copy.mandatory.workflow");
			    _handleException(we, req);
			}
			catch (Exception ae) {
				_handleException(ae, req);
			}
			if(UtilMethods.isSet(referer)){
				_sendToReferral(req, res, referer);
			}else{
				buildFakeAjaxResponse(req, res);
			}
	        return;
		} else
			Logger.debug(this, "Unspecified Action");
		_loadForm(req, res, config, form, user, validate);

		reindexContentlets(contentToIndexAfterCommit,cmd);
		HibernateUtil.closeAndCommitTransaction();

		if(UtilMethods.isSet(req.getAttribute("inodeToWaitOn"))){
			if(!conAPI.isInodeIndexed(req.getAttribute("inodeToWaitOn").toString())){ // todo: not sure about this one, might introduce a timeout
				Logger.error(this, "Timedout waiting on index to return");
			}
		}
		req.setAttribute("cache_control", "0");
		
		setForward(req, "portlet.ext.contentlet.edit_contentlet");
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws Exception
	 */
	protected void _retrieveWebAsset(final ActionRequest req,
									 final ActionResponse res,
									 final PortletConfig config,
									 final ActionForm form,
									 final User user) throws Exception {

		String inode = req.getParameter("inode");
		String inodeStr = (InodeUtils.isSet(inode) ? inode : StringUtils.EMPTY);
		Contentlet contentlet = new Contentlet();
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

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
			String langId = req.getParameter("lang");

			if(InodeUtils.isSet(sibblingInode) && !sibblingInode.equals("0")){
				Contentlet sibblingContentlet = conAPI.find(sibblingInode,APILocator.getUserAPI().getSystemUser(), false);

				if (UtilMethods.isSet(langId) && sibblingContentlet.getLanguageId() == Long.parseLong(langId)) {
					contentlet = sibblingContentlet;
					req.setAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT, contentlet);
					req.setAttribute("inode", sibblingInode);
				} else {
					Logger.debug(EditContentletAction.class, ()->"retrieveWebAsset :: Sibbling Contentlet = " + sibblingContentlet.getInode());
					Identifier identifier = APILocator.getIdentifierAPI().find(sibblingContentlet);
					contentlet.setIdentifier(identifier.getInode());
					contentlet.setStructureInode(sibblingContentlet.getStructureInode());

					// take host field values with it
					for (final Field field : FieldsCache.getFieldsByStructureInode(sibblingContentlet.getStructureInode())) {
						if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
							contentlet.setStringProperty(field.getVelocityVarName(), sibblingContentlet.getStringProperty(field.getVelocityVarName()));
							contentlet.setHost(sibblingContentlet.getHost());
							contentlet.setFolder(sibblingContentlet.getFolder());
						}
					}
				}
			}

			if(UtilMethods.isSet(langId)){
				contentlet.setLanguageId(Long.parseLong(langId));
			}
		}

		if(perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false));
		req.setAttribute(WebKeys.CONTENTLET_EDIT, contentlet);
		contentletToEdit = contentlet;

		// Contententlets Relationships
		Structure contentType = contentlet.getStructure();

		if (contentType == null || !InodeUtils.isSet(contentType.getInode())) {
			contentType = this.getSelectedStructure(req, (ContentletForm) form, contentTypeAPI);
		}

		contentlet.setContentTypeId(contentType.id());

		req.setAttribute(WebKeys.CONTENTLET_RELATIONSHIPS_EDIT,
				APILocator.getContentletAPI().getAllRelationships(contentlet));

		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		if(contentlet.getLanguageId() != 0){
			httpReq.getSession().setAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE, String.valueOf(contentlet.getLanguageId()));
		} else {
			httpReq.getSession().setAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE,
				String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId()));
		}

		req.setAttribute("identifier", contentlet.getIdentifier());
		// Asset Versions to list in the versions tab
		req.setAttribute(WebKeys.VERSIONS_INODE_EDIT, contentlet);

	}

	/**
	 * 
	 * 
	 * @param request
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param contentTypeAPI
	 *            - The API used to retrieve the selected Content Type.
	 * @throws Exception
	 */
	private Structure getSelectedStructure(final ActionRequest request,
										   final ContentletForm form,
										   final ContentTypeAPI contentTypeAPI) throws DotSecurityException, DotDataException {
		Structure contentType = null;
		String selectedStructure = StringUtils.EMPTY;

		if (InodeUtils.isSet(request.getParameter("selectedStructure"))) {
            selectedStructure = request.getParameter("selectedStructure");
            contentType = this.transform(contentTypeAPI.find(selectedStructure));
        } else if (InodeUtils.isSet(request.getParameter("sibblingStructure"))) {
            selectedStructure = request.getParameter("sibblingStructure");
            contentType = this.transform(contentTypeAPI.find(selectedStructure));
        } else{
            contentType = StructureFactory.getDefaultStructure();
            form.setAllowChange(true);
        }

		return contentType;
	}

	/**
	 * 
	 * @param contentType
	 * @return
	 */
	private Structure transform(final ContentType contentType) {
		return (null != contentType)?new StructureTransformer(contentType).asStructure():null;
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @throws Exception
	 */
	private void _addToParents(ActionRequest req) {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		Logger.debug(this, "Inside AddContentletToParentsAction");

		Contentlet contentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_FORM_EDIT);
		Contentlet currentContentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);

		Logger.debug(this, "currentContentlet inode=" + currentContentlet.getInode());
		Logger.debug(this, "contentlet inode=" + contentlet.getInode());

		// it's a new contentlet. we should add to parents
		// if it's a version the parents get copied on save asset method
		if (currentContentlet.getInode().equalsIgnoreCase(contentlet.getInode())) {
			String contentcontainer_inode = req.getParameter("contentcontainer_inode");
			Container containerParent = (Container) InodeFactory.getInode(contentcontainer_inode, Container.class);
			Logger.debug(this, "Added Contentlet to parent=" + containerParent.getInode());
			SessionMessages.add(httpReq, "message", "message.contentlet.add.parents");
		}
	}

	/**
	 * Displays the "Add New Content" page where users can add content based on
	 * a specific Content Type.
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for the Content Search portlet.
	 * @param form
	 *            - The form containing the information selected by the user,
	 *            e.g., the selected Content Type.
	 * @param user
	 *            - The {@link User} who wants to create a new content.
	 * @throws Exception
	 *             An error occurred when generating the content edit page.
	 */
	private void _newContent(final ActionRequest req,
							 final ActionResponse res,
							 final PortletConfig config,
							 final ActionForm form,
							 final User user) throws Exception {
		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

		//Contentlet Form
		ContentletForm cf = (ContentletForm) form;

		String cmd = req.getParameter(Constants.CMD);
		String inode = req.getParameter("inode");
		String inodeStr = (InodeUtils.isSet(inode) ? inode : "");
		Contentlet contentlet = new Contentlet();

		if(InodeUtils.isSet(inodeStr)) {
			contentlet = conAPI.find(inodeStr, user, false);
		}
		req.setAttribute(WebKeys.CONTENTLET_EDIT, contentlet);
		Structure contentType = contentlet.getStructure(); // todo
		// : this is null, but the dialog if does not have any content type for the current user shouldn't be showed.

		String selectedContentType = "";
		final String siblingStructure = req.getParameter("sibblingStructure");
		if (InodeUtils.isSet(req.getParameter("selectedStructure"))
				|| InodeUtils.isSet(cf.getStructureInode())
				|| InodeUtils.isSet(siblingStructure)) {

			if (InodeUtils.isSet(req.getParameter("selectedStructure"))) {
				selectedContentType = req.getParameter("selectedStructure");

			} else if (InodeUtils.isSet(siblingStructure)){
				selectedContentType = siblingStructure;

			}
			else {
				selectedContentType = cf.getStructureInode();
			}
			contentType = this.transform(contentTypeAPI.find(selectedContentType));
			contentlet.setStructureInode(contentType.getInode());
		} else if (cmd.equals("newedit")) {
			contentType = StructureFactory.getDefaultStructure();
			contentlet.setStructureInode(contentType.getInode());
		}

		String langId = req.getParameter("lang");
		if(UtilMethods.isSet(langId)) {
			try {
				contentlet.setLanguageId(Long.parseLong(langId));
			} catch (NumberFormatException e) {
			    Logger.error(getClass(),"Error parsing language Id from request", e);
				contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
			}
		}else{
			contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}
		// Add information regarding the last content search in the session
		if (httpReq.getSession().getAttribute(WebKeys.CONTENTLET_LAST_SEARCH) == null) {
			Map<String, Object> lastSearchMap = new HashMap<>();
			lastSearchMap.put("structure", contentType);
			lastSearchMap.put("fieldsSearch", new HashMap<String, String>());
			lastSearchMap.put("categories", new ArrayList<String>());
			lastSearchMap.put("showDeleted", false);
			lastSearchMap.put("filterSystemHost", false);
			lastSearchMap.put("filterLocked", false);
			lastSearchMap.put("page", 1);
			lastSearchMap.put("orderBy", "modDate desc");
			httpReq.getSession().setAttribute(WebKeys.CONTENTLET_LAST_SEARCH, lastSearchMap);
		}

        if(null != contentType) {
			//In case we failed to determine the structured out of the selectedStructure param
			if (!UtilMethods.isSet(contentType.getInode()) && UtilMethods.isSet(req.getParameter("identifier"))) {
				final String identifier = req.getParameter("identifier");
				final Contentlet auxContentlet = conAPI
						.findContentletByIdentifierAnyLanguage(identifier);
				contentType = auxContentlet.getStructure();
				contentlet.setStructureInode(contentType.getInode());
			}
		}else{
			this.handleContentTypeNull((ActionRequestImpl) req, inode);
		}
		// Checking permissions to add new content of selected content type
		_checkWritePermissions(contentType, user, httpReq);

		List<Field> list = (List<Field>) FieldsCache.getFieldsByStructureInode(contentType.getInode());
		for (Field field : list) {
			String defaultValue = field.getDefaultValue();
			if (UtilMethods.isSet(defaultValue)) {
				String typeField = field.getFieldContentlet();
				if (typeField.startsWith("bool")) {
					boolean defaultValueBoolean = false;
					if(defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("1") || defaultValue.equalsIgnoreCase("yes")
							|| defaultValue.equalsIgnoreCase("y") || defaultValue.equalsIgnoreCase("on"))
						defaultValueBoolean = true;
					contentlet.setBoolProperty(field.getVelocityVarName(), defaultValueBoolean);
				} else if (typeField.startsWith("date")) {
				    if(defaultValue.equals("now"))
				        contentlet.setDateProperty(field.getVelocityVarName(), new Date());
				    else {
				        DateFormat df=null;
				        final String ft=field.getFieldType();
			            if(ft.equals(Field.FieldType.DATE.toString()))
			                df=new SimpleDateFormat("yyyy-MM-dd");
			            else if(ft.equals(Field.FieldType.DATE_TIME.toString()))
			                df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			            else if(ft.equals(Field.FieldType.TIME.toString()))
			                df=new SimpleDateFormat("HH:mm:ss");
			            try {
			                contentlet.setDateProperty(field.getVelocityVarName(), df.parse(defaultValue));
			            }
			            catch(ParseException e) {
			                // pass it as null
			            }
				    }
				} else if (typeField.startsWith("float")) {
				    contentlet.setFloatProperty(field.getVelocityVarName(), Float.parseFloat(defaultValue));
				} else if (typeField.startsWith("integer")) {
				    contentlet.setLongProperty(field.getVelocityVarName(), Long.parseLong(defaultValue));
				} else if (typeField.startsWith("text")) {
				    contentlet.setStringProperty(field.getVelocityVarName(), defaultValue);
				}
			}
		}
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param inode
	 */
	private void handleContentTypeNull(final ActionRequestImpl req, final String inode) {
		Logger.info(this,
                "The content type is null on adding new content with the inode: " + inode +
                          ", throwing IllegalArgumentException");

		final Locale locale = LocaleUtil.getLocale(req.getHttpServletRequest());
		String message = null;

		try {

            message = LanguageUtil.format(locale, "edit-contentlet-content-type-null", (null == inode)?"null":inode);
        } catch (LanguageException e) {

            message = "The content type is null on adding new content with the inode: " + inode;
        }

		throw new IllegalArgumentException(message);
	}

	/**
	 * 
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws Exception
	 */
	public void _editWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user)
	throws Exception {

		final ContentletForm contentletForm = (ContentletForm) form;
		final ContentletAPI contAPI = APILocator.getContentletAPI();

		Contentlet contentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);
		Contentlet workingContentlet = null;

		String sib= req.getParameter("sibbling");
		Boolean populateaccept = Boolean.valueOf(req.getParameter("populateaccept"));

		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		httpReq.getSession().setAttribute("populateAccept", populateaccept);

		if(UtilMethods.isSet(sib))
		{
			final Contentlet sibbling = conAPI.find(sib, user,false);
			unLockIfNecessary(sibbling, user);

			if(populateaccept){
				contentlet = sibbling;
				contentlet.setInode("");
				Structure structure = contentlet.getStructure();
				List<Field> list = FieldsCache.getFieldsByStructureInode(structure.getInode());
				for (Field field : list) {
					if(Field.FieldType.BINARY.toString().equals(field.getFieldType())){
						httpReq.getSession().setAttribute(field.getFieldContentlet() + "-sibling", sib+","+field.getVelocityVarName());
						java.io.File inputFile = APILocator.getContentletAPI().getBinaryFile(sib, field.getVelocityVarName(), user);
						if(inputFile != null){
							java.io.File acopyFolder=new java.io.File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
									+ java.io.File.separator + user.getUserId() + java.io.File.separator + field.getFieldContentlet()
	                                + java.io.File.separator + UUIDGenerator.generateUuid());
							
							if(!acopyFolder.exists())
	                            acopyFolder.mkdir();
							
							String shortFileName = FileUtil.getShortFileName(inputFile.getAbsolutePath());
							
							java.io.File binaryFile = new java.io.File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
									+ java.io.File.separator + user.getUserId() + java.io.File.separator + field.getFieldContentlet()
									+ java.io.File.separator + shortFileName.trim());
							
							FileUtil.copyFile(inputFile, binaryFile);
						}
					}
				}
			}
		}

		if(InodeUtils.isSet(contentlet.getInode())){
			final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();

			workingContentlet = contAPI.findContentletByIdentifier(contentlet.getIdentifier(),
					false, contentlet.getLanguageId(), currentVariantId, user, false);

			if(workingContentlet == null) {
				workingContentlet = contAPI.findContentletByIdentifier(contentlet.getIdentifier(),
						false, contentlet.getLanguageId(), VariantAPI.DEFAULT_VARIANT.name(),
						user, false);

			}
		}else{
			workingContentlet = contentlet;
		}

		if(!InodeUtils.isSet(contentlet.getInode())) {
    		String langId = req.getParameter("lang");
    		if(UtilMethods.isSet(langId)) {
    		    contentlet.setLanguageId(Long.parseLong(langId));
    		}
    		else {
    		    contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
    		}
		}

		//Return the tags related to this Contentlet in order to show them in the edit window
		contentlet.setTags();

		GregorianCalendar cal = new GregorianCalendar();
		if (contentlet.getModDate() == null) {
			contentlet.setModDate(cal.getTime());
		}

		if(!UtilMethods.isSet(contentlet.getInode())) {
		    req.setAttribute(WebKeys.CONTENT_EDITABLE, true);
		}
		else if(perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, user) && workingContentlet.isLocked()){

			Optional<String> lockedUserId = APILocator.getVersionableAPI().getLockedBy(workingContentlet);
			if(lockedUserId.isPresent() && user.getUserId().equals(lockedUserId.get())){
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
		contentletForm.setMap(new HashMap<>(contentlet.getMap()));

		Logger.debug(this, "EditContentletAction: contentletInode=" + contentlet.getInode());

		req.setAttribute(WebKeys.CONTENTLET_EDIT, contentlet);

		if(UtilMethods.isSet(req.getParameter("is_rel_tab"))) {
			req.setAttribute("is_rel_tab", req.getParameter("is_rel_tab"));
		}

		if(contentlet.isCalendarEvent() && form instanceof EventAwareContentletForm){
			final EventAwareContentletForm eventForm = (EventAwareContentletForm) form;
			if(UtilMethods.isSet(contentlet.getInode())){
				final Event ev = eventAPI.findbyInode(contentlet.getInode(), user, false);
				editEvent(ev, eventForm);
			} else {
				setEventDefaults(eventForm);
			}
		}

	}

	private void unLockIfNecessary(final Contentlet content, final User user) {
		try {
			if (content.isLocked()) {
				Optional<ContentletVersionInfo> contentletVersionInfo =
						APILocator.getVersionableAPI().getContentletVersionInfo(content.getIdentifier(), content.getLanguageId());

				if (contentletVersionInfo.isPresent()
						&& user.getUserId().equals(contentletVersionInfo.get().getLockedBy())) {
					conAPI.unlock(content, user, false);
				}
			}
		} catch (DotDataException|DotSecurityException e) {
			Logger.error(EditContentletAction.class, e.getMessage(), e);
			new DotRuntimeException(e);
		}
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentlet
	 *            -
	 * @return
	 * @throws Exception
	 * @throws DotContentletValidationException
	 */
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
			List<String> disabled = new ArrayList<>();
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
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws Exception
	 */
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

		ArrayList<Category> cats = new ArrayList<>();
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
						if(APILocator.getRelationshipAPI().sameParentAndChild(records.getRelationship()) &&
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
		if (Config.getBooleanProperty("CONTENT_CHANGE_NOTIFICATIONS") && !isNew) {
			_sendContentletPublishNotification(currentContentlet, reqImpl.getHttpServletRequest());
		}
		SessionMessages.add(httpReq, "message", "message.contentlet.save");
		if( subcmd != null && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH) ) {
			SessionMessages.add(httpReq, "message", "message.contentlet.published");
		}
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param con
	 * @return
	 */
	private ArrayList<Permission> _getSelectedPermissions(ActionRequest req, Contentlet con){
		ArrayList<Permission> pers = new ArrayList<>();
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

	/**
	 * 
	 * @param contentlet
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @throws Exception
	 * @throws PortalException
	 * @throws SystemException
	 */
	private void _sendContentletPublishNotification (Contentlet contentlet, HttpServletRequest req) throws Exception,PortalException, SystemException {
		try
		{
			User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			Map<String, String[]> params = new HashMap<> ();
			params.put("struts_action", new String [] {"/ext/contentlet/edit_contentlet"});
			params.put("cmd", new String [] {"edit"});
			params.put("inode", new String [] { String.valueOf(contentlet.getInode()) });
			String contentURL = PortletURLUtil.getActionURL(req, WindowState.MAXIMIZED.toString(), params);
			List<Map<String, Object>> references = conAPI.getContentletReferences(contentlet, currentUser, false);
			List<Map<String, Object>> validReferences = new ArrayList<> ();

			//Avoiding to send the email to the same users
			for (Map<String, Object> reference : references)
			{
				try
				{
					IHTMLPage page = (IHTMLPage)reference.get("page");
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

	/**
	 * Contentlet change notifications thread
	 */
	private class ContentChangeNotificationThread extends Thread {

		private String serverName;
		private String contentletEditURL;
		private Contentlet contentlet;
		private List<Map<String, Object>> references;
		private HostAPI hostAPI = APILocator.getHostAPI();
		private UserAPI userAPI = APILocator.getUserAPI();

		/**
		 * 
		 * @param cont
		 * @param references
		 * @param contentletEditURL
		 * @param serverName
		 */
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
					IHTMLPage page = (IHTMLPage)reference.get("page");
					Host host = hostAPI.findParentHost(page, systemUser, false);
					Company company = PublicCompanyFactory.getDefaultCompany();
					User pageUser = (User)reference.get("owner");

					HashMap<String, Object> parameters = new HashMap<>();
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

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws Exception
	 */
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
		}catch(Exception e) {
          SessionMessages.add(httpReq, "error", e.getMessage());
          return;
      }
		// gets the session object for the messages
		SessionMessages.add(httpReq, "message", "message.contentlet.copy");
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws Exception
	 */
	public void _getVersionBackWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form,
			User user) throws Exception {
		Contentlet conVersionToRestore = conAPI.find(req.getParameter("inode_version"), user, false);
		conAPI.restoreVersion(conVersionToRestore, user, false);
		req.setAttribute(WebKeys.CONTENTLET_EDIT , conVersionToRestore);
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param validate
	 * @throws Exception
	 */
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
				httpReq.getSession().setAttribute(WebKeys.Structure.STRUCTURE_TYPE, Integer.valueOf("1"));
				String selectedStructure = req.getParameter("selectedStructure");
				if (InodeUtils.isSet(selectedStructure)) {
					structure = CacheLocator.getContentTypeCache().getStructureByInode(selectedStructure);
				}
			}

			List<Structure> structures = StructureFactory.getStructuresWithWritePermissions(user, false);

			//Add the structure to the collection if not exists.
            //In case of PERSONA AND FORM, the structure will be included only if the license is enterprise
            if (!structures.contains(structure) &&
                    APILocator.getContentTypeAPI(user)
                            .isContentTypeAllowed(new StructureTransformer(structure).from())) {
                    //avoid exception if the list is immutable
                    structures = new ArrayList<>(structures);
                    structures.add(structure);
			}
			contentletForm.setAllStructures(ImmutableList.of(structures));

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
				List<String> categoriesArr = new ArrayList<> ();
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

	/**
	 * Downloads the list of contentlets of the specified Content Type Inode as
	 * a CSV file.
	 * 
	 * @param response
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentletsToExport
	 *            - The list contentlets that will be exported.
	 * @param contentTypeInode
	 *            - The Content Type Inode of the contentlets.
	 * @throws DotSecurityException
	 *             An error occurred when retrieving contentlet data.
	 */
	public void downloadToExcel(final HttpServletResponse response, final User user, final List<Map<String, String>> contentletsToExport, final String contentTypeInode) throws DotSecurityException, DotDataException {
		PrintWriter writer = null;
		if (!contentletsToExport.isEmpty()) {
			Logger.info(this, String.format("Generating CSV data. Mapping content list of type '%s'", contentTypeInode));
			final List<String> contentletInodes = new ArrayList<>();
			for (int i = 2; i < contentletsToExport.size(); ++i) {
				final Object contentletData = contentletsToExport.get(i);
				if (contentletData != null && contentletData instanceof HashMap) {
                    final Map<String, String> contentletMap = contentletsToExport.get(i);
					contentletInodes.add(contentletMap.get("inode"));
				}
			}
			final List<Contentlet> contentletList = new ArrayList<>();
			for (final String inode : contentletInodes) {
				try{
                    final Contentlet contentlet = this.conAPI.find(inode, user, DONT_RESPECT_FRONTEND_ROLES);
                    contentletList.add(contentlet);
				} catch (final DotDataException ex){
					Logger.warn(this, "Unable to find contentlet with Inode: " + inode);
				}
			}
            int totalSize = 0;
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(contentTypeInode);
            final String csvFileName = contentType.name() + "_contents_" + UtilMethods.dateToHTMLDate(new java
                    .util.Date(), "M_d_yyyy") + ".csv";
			try {
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM + "; " + MediaType.CHARSET_PARAMETER + "="
                        + StandardCharsets.UTF_8.name());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + csvFileName +"\"");
				writer = response.getWriter();
				writer.print("Identifier");
				writer.print(",languageCode");
				writer.print(",countryCode");
                final List<com.dotcms.contenttype.model.field.Field> contentTypeFields =
                        APILocator.getContentTypeFieldAPI().byContentTypeId((contentTypeInode));
				for (final com.dotcms.contenttype.model.field.Field field : contentTypeFields) {
				    // Exclude fields that are not exportable
					if (isNewFieldTypeAllowedOnImportExport(field)) {
						writer.print("," + field.variable());
					}
				}

				writer.print("\r\n");
				int counter = 0;
				int loggingPoint = 2000;
                totalSize = contentletList.size();
				for (final Contentlet content : contentletList) {
                    counter += 1;
                    if (counter % loggingPoint == 0) {
                        Logger.info(this, String.format("Exporting %d out of %d contentlet(s)", counter, totalSize));
                    }
					final List<Category> catList = this.catAPI.getParents(content, user, DONT_RESPECT_FRONTEND_ROLES);
					writer.print(content.getIdentifier());
					final Language lang = APILocator.getLanguageAPI().getLanguage(content.getLanguageId());
					writer.print("," +lang.getLanguageCode());
					writer.print(","+lang.getCountryCode());

					for (final com.dotcms.contenttype.model.field.Field field: contentTypeFields) {
						try {
							// We don't need to export fields of these types
							if (!isNewFieldTypeAllowedOnImportExport(field)) {
                               continue;
							}

							Object value = StringPool.BLANK;
							if (this.conAPI.getFieldValue(content,field) != null) {
								value = this.conAPI.getFieldValue(content,field);
							}
							String text = StringPool.BLANK;
							if (field instanceof CategoryField) {
								final Category category = this.catAPI.find(field.values(), user, DONT_RESPECT_FRONTEND_ROLES);
                                final List<Category> children = catList;
                                final List<Category> allChildren= this.catAPI.getAllChildren(category, user, DONT_RESPECT_FRONTEND_ROLES);
								if (children.size() >= 1 && this.catAPI.canUseCategory(category, user, DONT_RESPECT_FRONTEND_ROLES)) {
									for (final Category cat : children){
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
							} else if (field instanceof TagField) {
							    //Get Content Tags per field's Velocity Var Name
                                final List<Tag> tags = this.tagAPI.getTagsByInodeAndFieldVarName(content.getInode(), field.variable());
							    if (!tags.isEmpty()) {
							        for (final Tag tag : tags){
							            if(text.equals(StringPool.BLANK)) {
							                text = tag.getTagName();
							            } else {
							                text = text + "," + tag.getTagName();
							            }
							        }
							    }
							} else if (field instanceof RelationshipField) {
								text = loadRelationships(((ContentletRelationships)value).getRelationshipsRecords());
							} else if (field instanceof KeyValueField) {
								text = new ObjectMapper().writeValueAsString(value);
                            } else if (BaseContentType.HTMLPAGE.equals(contentType.baseType()) && PageContentType
                                    .PAGE_URL_FIELD_VAR.equalsIgnoreCase(field.variable())) {
                                final Identifier id = APILocator.getIdentifierAPI().find(content.getIdentifier());
                                if (null != id && UtilMethods.isSet(id.getId())) {
                                    text = id.getPath();
                                }
							} else if (field instanceof ImageField) {
								final Identifier id = APILocator.getIdentifierAPI().find((String) value);
								if (null != id && UtilMethods.isSet(id.getId())) {
									text = id.getPath();
								}

							} else if (field instanceof BinaryField){
								final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
								final String fileLink = new ResourceLink.ResourceLinkBuilder().getFileLink(request, user, content, field.variable());
								text = fileLink;
							} else{
								if (value instanceof Date || value instanceof Timestamp) {
									if (field instanceof DateField) {
                                        final SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_DATE);
										text = formatter.format(value);
									} else if (field instanceof DateTimeField) {
                                        final SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_DATETIME);
										text = formatter.format(value);
									} else if (field instanceof TimeField) {
                                        final SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_TIME);
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
							if(text.contains(",") || text.contains("\n")) {
								//Double quotes replacing
								text = text.replaceAll("\"","\"\"");
								writer.print(",\""+text+"\"");
							} else {
								writer.print(","+text);
							}
						} catch (final Exception e) {
							writer.print(",");
                            Logger.error(this, String.format("An error occurred when exporting field '%s' [%s] to CSV" +
                                    " file '%s': %s", field.name(), field.inode(), csvFileName, e.getMessage()), e);
						}
					}
					writer.print("\r\n");
				}
				writer.flush();
				writer.close();
				HibernateUtil.closeSession();
			} catch (final Exception p) {
                final String errorMsg = String.format("An error occurred when exporting contents of type '%s' [%s] to" +
                        " CSV file '%s': %s", contentType.name(), contentTypeInode, csvFileName, p.getMessage());
                final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
                final SystemMessage systemMessage = systemMessageBuilder.setMessage(errorMsg).setType(MessageType
                        .SIMPLE_MESSAGE).setSeverity(MessageSeverity.ERROR).setLife(10000).create();
                SystemMessageEventUtil.getInstance().pushMessage(systemMessage, ImmutableList.of(user.getUserId()));
                Logger.error(this, errorMsg, p);
			}
            final DecimalFormat decimalFormat = new DecimalFormat("###,###");
            final String endMsg = String.format("A total of %s contents of type '%s' [%s] have been successfully " +
                            "exported to CSV file '%s'", decimalFormat.format(totalSize), contentType.name(),
                    contentTypeInode, csvFileName);
            final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
            final SystemMessage systemMessage = systemMessageBuilder.setMessage(endMsg).setType(MessageType
                    .SIMPLE_MESSAGE).setSeverity(MessageSeverity.INFO).setLife(10000).create();
            SystemMessageEventUtil.getInstance().pushMessage(systemMessage, ImmutableList.of(user.getUserId()));
            Logger.info(this, endMsg);
        } else {
			try {writer.print("\r\n");} catch (Exception e) {	Logger.debug(this,"Error: download to excel "+e);	}
		}
	}

	/**
	 * Builds the list of related records separated by comma
	 */
	private String loadRelationships(List<ContentletRelationshipRecords> relationshipRecords)
			throws DotDataException {

		final StringBuilder result = new StringBuilder();

		relationshipRecords.forEach(record -> result.append(String
				.join(StringPool.COMMA,
						record.getRecords().stream().map(Contentlet::getIdentifier).collect(
								Collectors.toList()))));

		return result.toString();
	}

	/**
	 * Returns the relationships associated to the current contentlet
	 *
	 * @param		req ActionRequest.
	 * @param		user User.
	 * @return		ContentletRelationships.
	 */
	private ContentletRelationships getCurrentContentletRelationships(ActionRequest req, User user) {
		List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<>();
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
				relationship = APILocator.getRelationshipAPI().byInode(inodes[0]);
				contentletRelationshipRecords = new ContentletRelationships(null).new ContentletRelationshipRecords(relationship, hasParent);
				records = new ArrayList<>();

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

	/**
	 * 
	 * @param currentcontent
	 * @param user
	 * @param req
	 * @return
	 */
	private ContentletRelationships retrieveRelationshipsData(Contentlet currentcontent, User user, ActionRequest req ){

		Set<String> keys = req.getParameterMap().keySet();

		ContentletRelationships relationshipsData = new ContentletRelationships(currentcontent);
		List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<> ();
		relationshipsData.setRelationshipsRecords(relationshipsRecords);

		for (String key : keys) {
			if (key.startsWith("rel_") && key.endsWith("_inodes")) {
				boolean hasParent = key.contains("_P_");
				String inodesSt = (String) req.getParameter(key);

				String[] inodes = inodesSt.split(",");

				Relationship relationship = APILocator.getRelationshipAPI().byInode(inodes[0]);
				ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
				ArrayList<Contentlet> cons = new ArrayList<>();
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

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentToIndexAfterCommit
	 */
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
				List<Contentlet> contentToIndexAfterCommit = new ArrayList<>();

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
					List<Contentlet> contentlets = new ArrayList<>();
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
								contentlet.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
								conAPI.unpublish(contentlet, user, false);
								HibernateUtil.closeAndCommitTransaction();
								ActivityLogger.logInfo(this.getClass(), "Unublish contentlet action", " User " + user.getFirstName() + " Unpublished content titled '" + contentlet.getTitle()
										+ "' ", currentHost);
							}catch (DotContentletStateException e) {
								stateError = true;
							}catch(DotStateException dse){
								if(dse.getMessage().equals("No live version Contentlet. Call setLive first")){
									if(contentlets.size() < 2)
										throw dse;
								}else{
									throw dse;
								}
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

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentToIndexAfterCommit
	 */
	private void _batchPublish(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Publish Method");
			String [] inodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				inodes = getSelectedInodes(req,user);
			}
			String resetExpiredStr=req.getParameter("expireDateReset");
			Date resetExpireDate=null;
			if(UtilMethods.isSet(resetExpiredStr)) {
			    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			    resetExpireDate=df.parse(resetExpiredStr);
			}

			class PublishThread extends Thread {
				private String[] inodes = new String[0];
				private User user;
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<>();
				Date resetExpireDate;

				public PublishThread(String[] inodes, User user,List<Contentlet> contentToIndexAfterCommit,Date resetExpireDate) {
					this.inodes = inodes;
					this.user = user;
					this.contentToIndexAfterCommit = contentToIndexAfterCommit;
					this.resetExpireDate=resetExpireDate;
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
					List<Contentlet> contentlets = new ArrayList<>();
					for(String inode  : inodes){

						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
							if(contentlet.isLive()){
								continue;
							}
						}catch (DotSecurityException e) {
							hasNoPermissionOnAllContent = true;
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, user) && !contentlet.isLive()) {
							if(resetExpireDate!=null) {
							    Identifier ident=APILocator.getIdentifierAPI().find(contentlet);
							    if(UtilMethods.isSet(ident.getSysExpireDate()) && ident.getSysExpireDate().before(new Date())) {
							        Structure st=contentlet.getStructure();
							        contentlet=APILocator.getContentletAPI().checkout(inode, user, false);
							        contentlet.setDateProperty(st.getExpireDateVar(), resetExpireDate);
							        contentlet=APILocator.getContentletAPI().checkin(contentlet, user, false);
							        APILocator.getContentletAPI().unlock(contentlet, user, false);
							    }
							}
						    contentlets.add(contentlet);
							contentToIndexAfterCommit.add(contentlet);
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
								HibernateUtil.closeAndCommitTransaction();
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

			PublishThread thread = new PublishThread(inodes, user,contentToIndexAfterCommit,resetExpireDate);

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
				} catch (PublishStateException e) {
					SessionMessages.add(httpReq, "message", e.getMessage());
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message.contentlets.batch.publish.error");
				}
			}
		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentToIndexAfterCommit
	 */
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
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<>();

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
					List<Contentlet> contentlets = new ArrayList<>();
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
							if (!contentlet.hasLiveVersion())
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
								ActivityLogger.logInfo(this.getClass(), "Archive contentlet action", " User " + user.getFirstName() + " Archived content titled '" + contentlet.getTitle()
										+ "' ", currentHost);
								HibernateUtil.closeAndCommitTransaction();
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

	/**
	 * Deletes a given list of contentlets. An error will be thrown if the user
	 * performing this action does not have the required permissions or if the
	 * contentlets are not archived.
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration setting for the Liferay portlet.
	 * @param form
	 *            - The Struts wrapper for the HTML form.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentToIndexAfterCommit
	 *            - The list of contentlets that will be indexed.
	 */
	private void _batchDelete(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,List<Contentlet> contentToIndexAfterCommit) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		try {
			Logger.debug(this, "Calling Full List Delete Method");
			String [] tempInodes = req.getParameterValues("publishInode");

			if (Boolean.parseBoolean(req.getParameter("fullCommand"))) {
				tempInodes = getSelectedInodes(req,user);
			}
			
			ArrayList<String> inodes = new ArrayList<>(Arrays.asList(tempInodes));

			class DeleteThread extends Thread {
				private List<String> inodes = new ArrayList<>();
				private User user;
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<>();

				public DeleteThread(List<String> inodes, User user,List<Contentlet> contentToIndexAfterCommit) {
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
					List<Contentlet> contentlets = new ArrayList<>();
					for(String inode  : inodes){
						Contentlet contentlet = new Contentlet();
						try{
							contentlet = conAPI.find(inode, user, false);
						}catch (DotSecurityException e) {
							hasNoPermissionOnAllContent = true;
						}catch (Exception ex){
							Logger.error(this, "Unable to find contentlet with inode " + inode);
						}

						if (perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user) && contentlet.isArchived()) {
							contentlets.add(contentlet);
						} else {
							hasNoPermissionOnAllContent = true;
						}
					}
					List<Contentlet> cons = new ArrayList<>();
					for (Contentlet content : contentlets) {
						cons.clear();
						cons.add(content);
						try{
							conAPI.delete(cons, user, false);
						}catch (DotSecurityException e) {
							Logger.error(this, "Unable to delete content with identifier " + content.getIdentifier()
									+ " because of a lack of permissions. " + e.getMessage(), e);
							hasNoPermissionOnAllContent = true;
						}catch (Exception e) {
							Logger.error(this, "Unable to delete content with identifier " + content.getIdentifier() + ". "
									+ e.getMessage(), e);
						}
					}

				if(hasNoPermissionOnAllContent)
					throw new DotSecurityException("Unable to delete some content due to lack of permissions");
				}
			}

			DeleteThread thread = new DeleteThread(inodes, user,contentToIndexAfterCommit);

			if (inodes.size() > 50) {

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

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentToIndexAfterCommit
	 */
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
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<>();

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
					List<Contentlet> contentlets = new ArrayList<>();
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
								ActivityLogger.logInfo(this.getClass(), "Unarchive contentlet action", " User " + user.getFirstName() + " Unarchived content titled '" + contentlet.getTitle()
										+ "' ", currentHost);
								HibernateUtil.closeAndCommitTransaction();
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

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 */
	private void _batchReindex(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) {

		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

		Logger.info(this, "Calling Batch Reindex Method");
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

				final ContentletCache contentletCache = CacheLocator.getContentletCache();

				int count = 0;
				for(final String inode  : inodes){

					try{
						final Contentlet contentlet = conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), false);
						if(contentlet != null && UtilMethods.isSet(contentlet.getInode())){
							contentlet.setLowIndexPriority(true);
							contentletCache.remove(contentlet.getInode());
							conAPI.reindex(contentlet);
							count++;
						}else{
							Logger.error(this, "Unable to find contentlet with inode " + inode);
							try {
								sendNotification("notification.reindex.error.title","notification.batch.reindexing.error.processrecord",  new Object[] {inode}, null, false);
							} catch ( DotDataException | LanguageException e ) {
								Logger.error(this, "Error creating a system notification informing about problems in the batch indexing process.", e);
							}
							//new ESIndexAPI().removeContentFromIndex(contentlet);
							// TODO: implement a way to clean the index in this case
							continue;
						}
					}catch (DotDataException ex){
						Logger.error(this, "Unable to find contentlet with inode " + inode);
						try {
							sendNotification("notification.reindex.error.title","notification.batch.reindexing.error.processrecord",  new Object[] {inode}, null, false);
						} catch ( DotDataException | LanguageException e ) {
							Logger.error(this, "Error creating a system notification informing about problems in the batch indexing process.", e);
						}
						//jAPI.addContentIndexEntryToDelete(inode);
						continue;
					}
				}
				try {
					sendNotification("notification.reindex.error.title","notification.batch.reindexing.success", null, null, false);
				} catch ( DotDataException | LanguageException e ) {
					Logger.error(this, "Error creating a system notification informing about problems in the batch indexing process.", e);
				}
				Logger.info(this, "Finished Batch Reindexed . Processed "+count+" / "+inodes.length+" contents");
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

	/**
	 * 
	 * @param contentToIndexAfterCommit
	 * @param cmd
	 */
	private void reindexContentlets(List<Contentlet> contentToIndexAfterCommit,String cmd){

		for (Contentlet con : contentToIndexAfterCommit) {
			try {
				Identifier ident=APILocator.getIdentifierAPI().find(con);
				if(ident!=null && UtilMethods.isSet(ident.getId()))
				    APILocator.getContentletIndexAPI().addContentToIndex(con);
				else
					APILocator.getContentletIndexAPI().removeContentFromIndex(con);
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
				conAPI.isInodeIndexed(c.getInode()+addlQry); // this looks necessary
			}
		}

	}

	/**
	 * Retrieves the list of contentlets that belong to the specified Content
	 * Type in order to export them as a CSV file.
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param from
	 *            - The data export format for the contentlets (e.g., "Excel")
	 * @return
	 */
	private List<Map<String, String>> searchContentlets(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String from){
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
			String filterUnpublish = req.getParameter("filterUnpublish");
			String currentSortBy = req.getParameter("expCurrentSortBy");
			String modDateFrom = req.getParameter("modDateFrom");
			String modDateTo = req.getParameter("modDateTo");

			List<String> listFieldsValues = new ArrayList<>();
			if (UtilMethods.isSet(fieldsValues)) {
				String[] fieldsValuesArray = fieldsValues.split(",");
				for (String value: fieldsValuesArray) {
					listFieldsValues.add(value);
				}
			}

			List<String> listCategoriesValues = new ArrayList<>();
			if (UtilMethods.isSet(categoriesValues)) {
				String[] categoriesValuesArray = categoriesValues.split(",");
				for (String value: categoriesValuesArray) {
					if(UtilMethods.isSet(value)) {
						listCategoriesValues.add(value);
					}
				}
			}

			ContentletAjax contentletAjax = new ContentletAjax();
			List<Map<String, String>> contentlets = contentletAjax.searchContentletsByUser(structureInode, listFieldsValues, listCategoriesValues, Boolean.parseBoolean(showDeleted), Boolean.parseBoolean(filterSystemHost), Boolean.parseBoolean(filterUnpublish), Boolean.parseBoolean(filterLocked), 0, currentSortBy, 100000, user, null, modDateFrom, modDateTo);
			return contentlets;
		} catch (Exception e) {
			Logger.debug(this, "Error: searchContentlets (EditContentletAction ): "+e);
			return null;
		}
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param webKeyEdit
	 * @throws Exception
	 */
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
			conAPI.deleteVersion(webAsset,user,false);


		}catch(Exception e){
			resImpl.getHttpServletResponse().getWriter().println("FAILURE:" + LanguageUtil.get(user, "message.contentlet.delete.live_or_working"));
		}
	}

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @return
	 */
	private String[] getSelectedInodes(ActionRequest req, User user){
	    String[] allInodes = new String[0];
        String[] uncheckedInodes = new String[0];
        String[] result;
        ArrayList<String> resultInodes = new ArrayList<>();

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

	/**
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param contentToIndexAfterCommit
	 */
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
				List<Contentlet> contentToIndexAfterCommit  = new ArrayList<>();

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
					List<Contentlet> contentlets = new ArrayList<>();
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
							HibernateUtil.closeAndCommitTransaction();
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

	/**
	 * Generates a new notification displayed at the top left side of the
	 * back-end page in dotCMS. This utility method allows you to send reports
	 * to the user regarding the operations performed during the re-index,
	 * whether they succeeded or failed.
	 * 
	 * @param title
	 *            - The message title that should be present in the language
	 *            properties files.
	 * @param key
	 *            - The message key that should be present in the language
	 *            properties files.
	 * @param msgParams
	 *            - The parameters, if any, that will replace potential
	 *            placeholders in the message. E.g.: "This is {0} test."
	 * @param defaultMsg
	 *            - If set, the default message in case the key does not exist
	 *            in the properties file. Otherwise, the message key will be
	 *            returned.
     * @param error - true if we want to send an error notification
     * @throws DotDataException
	 *             The notification could not be posted to the system.
	 * @throws LanguageException
	 *             The language properties could not be retrieved.
	 */
	protected void sendNotification(final String title, final String key, final Object[] msgParams, final String defaultMsg, boolean error)
			throws DotDataException, LanguageException {

        NotificationLevel notificationLevel = error? NotificationLevel.ERROR: NotificationLevel.INFO;

		//Search for the CMS Admin role and System User
		final Role cmsAdminRole = roleAPI.loadCMSAdminRole();
		final User systemUser = userAPI.getSystemUser();

		this.notificationAPI.generateNotification(
				new I18NMessage(title), // title = Reindex Notification
				new I18NMessage(key, defaultMsg, msgParams),
				null, // no actions
                notificationLevel,
				NotificationType.GENERIC,
				Visibility.ROLE,
				cmsAdminRole.getId(),
				systemUser.getUserId(),
				systemUser.getLocale()
		);
	}

	private void handleEditAssetException(Exception ae, ActionRequest req, ActionResponse res, String referer, HttpServletRequest httpReq) throws Exception {
		if ((referer != null) && (referer.length() != 0)) {
			if (ae.getMessage() != null && ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {
				// The web asset edit threw an exception because it's locked so it should redirect back with message
				java.util.Map<String, String[]> params = new java.util.HashMap<>();
				params.put("struts_action", new String[]{"/ext/director/direct"});
				params.put("cmd", new String[]{"editContentlet"});
				params.put("contentlet", new String[]{req.getParameter("inode")});
				params.put("container", new String[]{(req.getParameter("contentcontainer_inode") != null) ? req.getParameter("contentcontainer_inode") : "0"});
				params.put("htmlPage", new String[]{(req.getParameter("htmlpage_inode") != null) ? req.getParameter("htmlpage_inode") : "0"});
				params.put("referer", new String[]{java.net.URLEncoder.encode(referer, "UTF-8")});

				String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq, WindowState.MAXIMIZED.toString(), params);

				_sendToReferral(req, res, directorURL);
			} else if (ae.getMessage() != null && ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				_sendToReferral(req, res, referer);
			} else {
				_handleException(ae, req);
			}
		} else {
			_handleException(ae, req);
		}
	}

}
