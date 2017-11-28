package com.dotmarketing.portlets.contentlet.business.web;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.api.system.event.ContentletSystemEventUtil;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.collections.CollectionUtils;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotLanguageException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.business.FieldAPI;

import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Config;
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
import com.liferay.util.servlet.SessionMessages;
/*
 *     //http://jira.dotmarketing.net/browse/DOTCMS-2273
 *     To save content via ajax.
 */
public class ContentletWebAPIImpl implements ContentletWebAPI {

	private CategoryAPI catAPI;
	private PermissionAPI perAPI;
	private ContentletAPI conAPI;
	private FieldAPI fAPI;
	private HostWebAPI hostAPI;
	private FolderAPI fldrAPI;
	private UserAPI userAPI;
	private FolderAPI folderAPI;
	private IdentifierAPI identAPI;

	private static DateFormat eventRecurrenceStartDateF = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static DateFormat eventRecurrenceEndDateF = new SimpleDateFormat("yyyy-MM-dd");

	private final ContentletSystemEventUtil contentletSystemEventUtil;

	public ContentletWebAPIImpl() {
		catAPI = APILocator.getCategoryAPI();
		perAPI = APILocator.getPermissionAPI();
		conAPI = APILocator.getContentletAPI();
		fAPI = APILocator.getFieldAPI();
		hostAPI = WebAPILocator.getHostWebAPI();
		fldrAPI = APILocator.getFolderAPI();
		this.userAPI = APILocator.getUserAPI();
		this.folderAPI = APILocator.getFolderAPI();
		this.identAPI = APILocator.getIdentifierAPI();

		contentletSystemEventUtil = ContentletSystemEventUtil.getInstance();
	}

	/*
	 * 	(non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPI#saveContent(java.util.Map, boolean, boolean, com.liferay.portal.model.User)
	 * This funtion works similar to EditContentletAction cmd = Constants.ADD
	 */
	public String saveContent(Map<String, Object> contentletFormData,
			  boolean isAutoSave, boolean isCheckin, User user) throws DotContentletValidationException, Exception {
		return saveContent(contentletFormData, isAutoSave, isCheckin, user, false);
	}

	public String saveContent(Map<String, Object> contentletFormData,
			  boolean isAutoSave, boolean isCheckin, User user, boolean generateSystemEvent) throws DotContentletValidationException, Exception {


		HttpServletRequest req =WebContextFactory.get().getHttpServletRequest();

		Logger.debug(this, "############################# Contentlet");

		boolean autocommit=DbConnectionFactory.getConnection().getAutoCommit();

		if(autocommit)
		    HibernateUtil.startTransaction();

		try {
			Logger.debug(this, "Calling Retrieve method");

			_retrieveWebAsset(contentletFormData, user);

		} catch (Exception ae) {
			_handleException(ae,autocommit);
			throw new Exception(ae.getMessage());
		}

		Contentlet cont;
		boolean isNew = isNew(contentletFormData);

		try {
			Logger.debug(this, "Calling Save Method");
			try{
				_saveWebAsset(contentletFormData,isAutoSave,isCheckin,user, generateSystemEvent);
			}catch (DotContentletValidationException ce) {
				if(!isAutoSave)
				SessionMessages.add(req, "message.contentlet.save.error");
				throw ce;

			}catch (Exception ce) {
				if(!isAutoSave)
				SessionMessages.add(req, "message.contentlet.save.error");
				throw ce;
			}

			Logger.debug(this, "HTMLPage inode=" + contentletFormData.get("htmlpage_inode"));
			Logger.debug(this, "Container inode=" + contentletFormData.get("contentcontainer_inode"));

            if ( InodeUtils.isSet( (String) contentletFormData.get( "htmlpage_inode" ) )
                    && InodeUtils.isSet( (String) contentletFormData.get( "contentcontainer_inode" ) ) ) {

                try {
                    Logger.debug( this, "I'm setting my contentlet parents" );
                    _addToParents( contentletFormData, user, isAutoSave );
                } catch ( DotSecurityException e ) {
                    throw new DotSecurityException( e.getMessage() );
                } catch ( Exception ae ) {
                    throw ae;
                }
            }


			cont = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);

			// finally we unlock the asset as the lock attribute is
			// attached to the identifier rather than contentlet as
			// before DOTCMS-6383
		    //conAPI.unlock(cont, user, false);
		} catch (Exception ae) {
			cont = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);
			//conAPI.refresh(cont);
			_handleException(ae,autocommit);
			throw ae;
		}

		if(autocommit) {
			HibernateUtil.closeAndCommitTransaction();
		}

		// todo: make it async by thread pool
		contentletSystemEventUtil.pushSaveEvent(cont, isNew);

		contentletFormData.put("cache_control", "0");


		return ((cont!=null) ? cont.getInode() : null);
	}

	private boolean isNew(Map<String, Object> contentletFormData) {
		Contentlet currentContentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);
		return !InodeUtils.isSet(currentContentlet.getInode());
	}

	/**
	 * Creates the relationship between a given Legacy or Content Page, a
	 * container, and its new or existing contentlet.
	 * 
	 * @param contentletFormData
	 *            - The information passed down after form submission.
	 * @param user
	 *            - The user performing this action.
	 * @param isAutoSave
	 *            -
	 * @throws Exception
	 *             It can be thrown if the user does not have the permission to
	 *             perform this action, or if an error occurred during the save
	 *             process.
	 */
	private void _addToParents(Map<String, Object> contentletFormData, User user,boolean isAutoSave) throws Exception {

		Logger.debug(this, "Inside AddContentletToParentsAction");

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		Contentlet contentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_FORM_EDIT);

		Contentlet currentContentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);

		Logger.debug(this, "currentContentlet inode=" + currentContentlet.getInode());
		Logger.debug(this, "contentlet inode=" + contentlet.getInode());

		// it's a new contentlet. we should add to parents
		// if it's a version the parents get copied on save asset method
		if (currentContentlet.getInode().equalsIgnoreCase(contentlet.getInode())
				&&(UtilMethods.isSet(contentletFormData.get("htmlpage_inode"))
						 && UtilMethods.isSet(contentletFormData.get("contentcontainer_inode")))) {

			String htmlpage_inode = (String) contentletFormData.get("htmlpage_inode");
			String contentcontainer_inode = (String) contentletFormData.get("contentcontainer_inode");

			final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
			final VersionableAPI versionableAPI = APILocator.getVersionableAPI();

			Identifier htmlParentId = identifierAPI.findFromInode(htmlpage_inode);
			Logger.debug(this, "Added Contentlet to parent=" + htmlpage_inode);

			Identifier containerParentId = null;
			Container containerParent = null;
			try{
				containerParentId =  identifierAPI.findFromInode(contentcontainer_inode);
				containerParent = (Container) versionableAPI.findWorkingVersion(containerParentId, user, false);
			}
			catch(Exception e){
				if(e instanceof DotSecurityException){
					SessionMessages.add(req, "message", "User needs 'View' Permissions on container");
					throw new DotSecurityException("User have no View Permissions on container");
				}else{
					throw e;
				}
			}

			if(containerParent != null){
				Logger.debug(this, "Added Contentlet to parent=" + containerParent.getInode());


				if (InodeUtils.isSet(htmlParentId.getId()) && InodeUtils.isSet(containerParent.getInode()) && InodeUtils.isSet(contentlet.getInode())) {
					Identifier containerIdentifier = identifierAPI.find(containerParent);
					Identifier contenletIdentifier = identifierAPI.find(contentlet);
					MultiTree multiTree = MultiTreeFactory.getMultiTree(htmlParentId, containerIdentifier,
							contenletIdentifier);
					Logger.debug(this, "Getting multitree for=" + htmlpage_inode + " ," + containerParent.getInode()
							+ " ," + contentlet.getIdentifier());
					Logger.debug(this, "Coming from multitree parent1=" + multiTree.getParent1() + " parent2="
							+ multiTree.getParent2());

					int contentletCount = MultiTreeFactory.getMultiTree(htmlParentId).size();

					if (!InodeUtils.isSet(multiTree.getParent1()) && !InodeUtils.isSet(multiTree.getParent2()) && !InodeUtils.isSet(multiTree.getChild())) {
						Logger.debug(this, "MTree is null!!! Creating new one!");
						MultiTree mTree = new MultiTree(htmlParentId.getInode(), containerIdentifier.getInode(),
														contenletIdentifier.getInode(),null,contentletCount);
						
						Contentlet htmlContentlet = conAPI.find(htmlpage_inode,
								user, false);						
						if (UtilMethods.isSet(htmlContentlet) && UtilMethods.isSet(htmlContentlet.getInode())
								&& (htmlContentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE)) {
							String pageIdentifier = htmlContentlet.getIdentifier();
							long contentletLang = contentlet.getLanguageId();
							ContentletVersionInfo versionInfo = APILocator
									.getVersionableAPI()
									.getContentletVersionInfo(pageIdentifier,
											contentletLang);
							if (versionInfo != null) {
								MultiTreeFactory.saveMultiTree(mTree,
										contentlet.getLanguageId());
							} else {
								// The language in the page and the contentlet 
								// do not match
								String language = APILocator.getLanguageAPI()
										.getLanguage(contentletLang)
										.getLanguage();
								Logger.debug(this,
										"Creating MultiTree failed: Contentlet with identifier "
												+ pageIdentifier
												+ " does not exist in "
												+ language);
								String msg = MessageFormat
										.format(LanguageUtil
												.get(user,
														"message.htmlpage.error.addcontent.invalidlanguage"),
												language);
								throw new DotLanguageException(msg);
							}
						} else {
							MultiTreeFactory.saveMultiTree(mTree);
						}
					}

				}
			}

			if(!isAutoSave)
			SessionMessages.add(req, "message", "message.contentlet.add.parents");
		}
	}

	private void _saveWebAsset(Map<String, Object> contentletFormData,
			boolean isAutoSave, boolean isCheckin, User user, boolean generateSystemEvent) throws Exception, DotContentletValidationException {

		/**
		System.out.println("----------------------------from form-------------------------");
		for(String x: contentletFormData.keySet()){
			System.out.println(x +":" + contentletFormData.get(x));
		}
		 **/


		HttpServletRequest req =WebContextFactory.get().getHttpServletRequest();
		Set contentletFormKeys = contentletFormData.keySet();//To replace req.getParameterValues()

		// Getting the contentlets variables to work
		Contentlet currentContentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);
		String currentContentident = currentContentlet.getIdentifier();
		boolean isNew = false;

		if(!InodeUtils.isSet(currentContentlet.getInode())){
			isNew = true;
		}





		/***
		 *
		 * Workflow
		 *
		 */
		currentContentlet.setStringProperty("wfActionId", (String) contentletFormData.get("wfActionId"));
		currentContentlet.setStringProperty("wfActionComments", (String) contentletFormData.get("wfActionComments"));
		currentContentlet.setStringProperty("wfActionAssign", (String) contentletFormData.get("wfActionAssign"));

		/**
		 *
		 * Push Publishing Actionlet
		 *
		 */
		currentContentlet.setStringProperty("wfPublishDate", (String) contentletFormData.get("wfPublishDate"));
		currentContentlet.setStringProperty("wfPublishTime", (String) contentletFormData.get("wfPublishTime"));
		currentContentlet.setStringProperty("wfExpireDate", (String) contentletFormData.get("wfExpireDate"));
		currentContentlet.setStringProperty("wfExpireTime", (String) contentletFormData.get("wfExpireTime"));
		currentContentlet.setStringProperty("wfNeverExpire", (String) contentletFormData.get("wfNeverExpire"));
		currentContentlet.setStringProperty("whereToSend", (String) contentletFormData.get("whereToSend"));
		currentContentlet.setStringProperty("forcePush", (String) contentletFormData.get("forcePush"));










		if(!isNew){


			WorkflowAPI wapi = APILocator.getWorkflowAPI();
			String wfActionId = (String) contentletFormData.get("wfActionId");
			if(UtilMethods.isSet(wfActionId)){
				WorkflowAction action = null;
				try{
					action = APILocator.getWorkflowAPI().findAction(wfActionId, user);
				}
				catch(Exception e){

				}
				if(action != null
						&& ! action.requiresCheckout()
						&& APILocator.getContentletAPI().canLock(currentContentlet, user)){

				    if(currentContentlet.isLocked())
				        APILocator.getContentletAPI().unlock(currentContentlet, user, false);

						currentContentlet.setModUser(user.getUserId());
						currentContentlet = APILocator.getWorkflowAPI().fireWorkflowNoCheckin(currentContentlet,user).getContentlet();
						contentletFormData.put(WebKeys.CONTENTLET_EDIT, currentContentlet);
						contentletFormData.put(WebKeys.CONTENTLET_FORM_EDIT, currentContentlet);
						SessionMessages.add(req, "message", "Workflow-executed");

						return;
				}
			}









			try{
				currentContentlet = conAPI.checkout(currentContentlet.getInode(), user, false);
			}
			catch(DotLockException dle){
				SessionMessages.add(req, "message", "message.cannot.unlock.content.for.editing");
				throw new DotLockException("User cannot lock contentlet : ", dle);
			}catch (DotSecurityException dse) {
				if(!isAutoSave)
					SessionMessages.add(req, "message", "message.insufficient.permissions.to.save");

				throw new DotSecurityException("User cannot checkout contentlet : ", dse);
			}
		}

		/***
		 *
		 * Workflow
		 *
		 */
		currentContentlet.setStringProperty("wfActionId", (String) contentletFormData.get("wfActionId"));
		currentContentlet.setStringProperty("wfActionComments", (String) contentletFormData.get("wfActionComments"));
		currentContentlet.setStringProperty("wfActionAssign", (String) contentletFormData.get("wfActionAssign"));

		/**
		 *
		 * Push Publishing Actionlet
		 *
		 */
		currentContentlet.setStringProperty("wfPublishDate", (String) contentletFormData.get("wfPublishDate"));
		currentContentlet.setStringProperty("wfPublishTime", (String) contentletFormData.get("wfPublishTime"));
		currentContentlet.setStringProperty("wfExpireDate", (String) contentletFormData.get("wfExpireDate"));
		currentContentlet.setStringProperty("wfExpireTime", (String) contentletFormData.get("wfExpireTime"));
		currentContentlet.setStringProperty("wfNeverExpire", (String) contentletFormData.get("wfNeverExpire"));
		currentContentlet.setStringProperty("whereToSend", (String) contentletFormData.get("whereToSend"));
		currentContentlet.setStringProperty("forcePush", (String) contentletFormData.get("forcePush"));


		contentletFormData.put(WebKeys.CONTENTLET_FORM_EDIT, currentContentlet);
		contentletFormData.put(WebKeys.CONTENTLET_EDIT, currentContentlet);

		try{
			_populateContent(contentletFormData, user, currentContentlet,isAutoSave);
			//http://jira.dotmarketing.net/browse/DOTCMS-1450
			//The form doesn't have the identifier in it. so the populate content was setting it to 0
			currentContentlet.setIdentifier(currentContentident);
			if(UtilMethods.isSet(contentletFormData.get("new_owner_permissions"))) {
				currentContentlet.setOwner((String) contentletFormData.get("new_owner_permissions"));
			}
		}catch (DotContentletValidationException ve) {
			throw new DotContentletValidationException(ve.getMessage());
		}

		String subcmd = "";
		if(UtilMethods.isSet(contentletFormData.get("subcmd")))
			subcmd = (String) contentletFormData.get("subcmd");

		//Saving interval review properties
		if (contentletFormData.get("reviewContent") != null && contentletFormData.get("reviewContent").toString().equalsIgnoreCase("true")) {
			currentContentlet.setReviewInterval((String)contentletFormData.get("reviewIntervalNum") + (String)contentletFormData.get("reviewIntervalSelect"));
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
		ArrayList<String> categoriesList = new ArrayList<String>();
		Host host =null;
		Folder folder = null;
		for (Iterator iterator = contentletFormKeys.iterator(); iterator.hasNext();) {
			String elementName = (String) iterator.next();
			if(elementName.startsWith("categories") && elementName.endsWith("_")){
				categoriesList.add((String)contentletFormData.get(elementName));
			}

			//http://jira.dotmarketing.net/browse/DOTCMS-3232
			if(elementName.equalsIgnoreCase("hostId") &&
					InodeUtils.isSet(contentletFormData.get(elementName).toString())){
				String hostId = contentletFormData.get(elementName).toString();
				 host = hostAPI.find(hostId, user, false);
				if(host == null)
					host = new Host();
				if(!perAPI.doesUserHavePermission(host,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)){
					SessionMessages.add(req, "message", "User needs 'Add Children' Permissions on selected host");
					throw new DotSecurityException("User has no Add Children Permissions on selected host");
				}
				currentContentlet.setHost(hostId);
				currentContentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
			}

			if(elementName.equalsIgnoreCase("folderInode") &&
					InodeUtils.isSet(contentletFormData.get(elementName).toString())){
				String folderInode = contentletFormData.get(elementName).toString();
				folder = fldrAPI.find(folderInode, user, true);
				if(isNew && !perAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)){
					SessionMessages.add(req, "message", "User needs 'Add Children Permissions' on selected folder");
					throw new DotSecurityException("User has no Add Children Permissions on selected folder");
				}
				currentContentlet.setHost(folder.getHostId());
				currentContentlet.setFolder(folderInode);
			}


		 }

		if (categoriesList != null && categoriesList.size() > 0) {
			for (Iterator iterator = categoriesList.iterator(); iterator
					.hasNext();) {
				String tmpString = (String) iterator.next();
				cats.add(catAPI.find(tmpString, user, false));
			}
		}

		try{
			ContentletRelationships contRel = retrieveRelationshipsData(currentContentlet,user, contentletFormData );

			// http://jira.dotmarketing.net/browse/DOTCMS-65
			// Coming from other contentlet to relate it automatically
			String relateWith = null;
			if(UtilMethods.isSet(contentletFormData.get("relwith")))
				relateWith = (String) contentletFormData.get("relwith");

			String relationType = null;
			if(UtilMethods.isSet(contentletFormData.get("reltype")))
				relationType = (String) contentletFormData.get("reltype");

			String relationHasParent = null;
			relationHasParent = (String) contentletFormData.get("relisparent");
			if(relateWith != null){
				try {

					List<ContentletRelationshipRecords> recordsList = contRel.getRelationshipsRecords();
					for(ContentletRelationshipRecords records : recordsList) {
						if(!records.getRelationship().getRelationTypeValue().equals(relationType))
							continue;
						if(FactoryLocator.getRelationshipFactory().sameParentAndChild(records.getRelationship()) &&
								((!records.isHasParent() && relationHasParent.equals("no")) ||
								 (records.isHasParent() && relationHasParent.equals("yes"))))
							continue;
						records.getRecords().add(conAPI.find(relateWith, user, false));

					}


				} catch (Exception e) {
					Logger.error(this,"Contentlet failed while creating new relationship",e);
				}
			}

			if("publish".equals(subcmd)){
				currentContentlet.setBoolProperty("live", true);
			}

			// Perform some validations before saving it
			if (currentContentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {
				String status = validateNewContentPage(currentContentlet);
				if (UtilMethods.isSet(status)) {
					String msg = LanguageUtil.get(user, status);
					throw new DotRuntimeException(msg);
				}
			}
			
			if(!isAutoSave){

				currentContentlet.setInode(null);
				currentContentlet = conAPI.checkin(currentContentlet, contRel,cats, perAPI.getPermissions(currentContentlet, false, true), user, false,generateSystemEvent);


			}else{
				 // Existing contentlet auto save
				Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();
				List<Relationship> rels = FactoryLocator.getRelationshipFactory()
											.byContentType( currentContentlet
                                                    .getStructure() );
				for (Relationship r : rels) {
					if (!contentRelationships.containsKey(r)) {
						contentRelationships
								.put( r, new ArrayList<Contentlet>() );
					}
					List<Contentlet> cons = conAPI.getRelatedContent(
							currentContentlet, r, user, true);
					for (Contentlet co : cons) {
						List<Contentlet> l2 = contentRelationships.get(r);
						l2.add(co);
					}
				}
				currentContentlet = conAPI.checkinWithoutVersioning(
											currentContentlet, contentRelationships, cats,
											perAPI.getPermissions(currentContentlet, false, true), user, false);
			}


		}catch(DotContentletValidationException ve) {
				throw ve;
		}
		currentContentlet.setStringProperty("wfActionComments", (String) contentletFormData.get("wfActionComments"));
		currentContentlet.setStringProperty("wfActionAssign", (String) contentletFormData.get("wfActionAssign"));


		contentletFormData.put(WebKeys.CONTENTLET_EDIT, currentContentlet);
		contentletFormData.put(WebKeys.CONTENTLET_FORM_EDIT, currentContentlet);


		if (Config.getBooleanProperty("CONTENT_CHANGE_NOTIFICATIONS") && !isNew && !isAutoSave)
			_sendContentletPublishNotification(currentContentlet, req);

		if(!isAutoSave)
		    SessionMessages.add(req, "message", "message.contentlet.save");

        if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
            APILocator.getVersionableAPI().setLive(currentContentlet);
            if(!isAutoSave)
                SessionMessages.add(req, "message", "message.contentlet.published");
        }
	}

	/**
     * {@inheritDoc}
	 */

    @Override
	public String validateNewContentPage(Contentlet contentPage) {
		String parentFolderId = contentPage.getFolder();
		String pageUrl = contentPage.getMap().get("url") == null ? "" : contentPage.getMap().get("url").toString();
		String status = null;
		try {
			User systemUser = userAPI.getSystemUser();
			Folder parentFolder = folderAPI.find(parentFolderId, systemUser,
					false);
			if (parentFolder != null
					&& InodeUtils.isSet(parentFolder.getInode())) {
				Host host = hostAPI.find(parentFolder.getHostId(), systemUser,
						true);
				String parentFolderPath = parentFolder.getPath();
				if (UtilMethods.isSet(parentFolderPath)) {
					if (!parentFolderPath.startsWith("/")) {
						parentFolderPath = "/" + parentFolderPath;
					}
					if (!parentFolderPath.endsWith("/")) {
						parentFolderPath = parentFolderPath + "/";
					}
					String fullPageUrl = parentFolderPath + pageUrl;
					if (!pageUrl.endsWith(".html")) {
						List<Identifier> folders = identAPI
								.findByURIPattern("folder", fullPageUrl,true, host);
						if (folders.size() > 0) {
							// Found a folder with same path
							status = "message.htmlpage.error.htmlpage.exists.folder";
						}
					}
					if (!UtilMethods.isSet(status)) {
						Identifier i = identAPI.find(host, fullPageUrl);
						if (i != null && InodeUtils.isSet(i.getId())) {
							try {
								Contentlet existingContent = conAPI
										.findContentletByIdentifier(i.getId(),
												true,
												contentPage.getLanguageId(),
												systemUser, false);
								if (existingContent.getStructure()
										.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET && !existingContent.getIdentifier().equals(contentPage.getIdentifier())) {
									// Found a file asset with same path
									status = "message.htmlpage.error.htmlpage.exists.file";
								} else if(!existingContent.getIdentifier().equals(contentPage.getIdentifier())){
									// Found page with same path and language
									status = "message.htmlpage.error.htmlpage.exists";
								}
							} catch (DotContentletStateException e) {
								// If it's a brand new page...
								if (!UtilMethods.isSet(contentPage
										.getIdentifier())) {
									// Found page with same path
									status = "message.htmlpage.error.htmlpage.exists";
								} else {
									Logger.info(getClass(),
											"Page with same URI and same language does not exist, so we are OK");
								}
							}
						}
					}
				}
			}
		} catch (DotDataException e) {
			Logger.debug(this,
					"Error trying to retreive information from page '"
							+ contentPage.getIdentifier() + "'");
			throw new DotRuntimeException("Page information is not valid", e);
		} catch (DotSecurityException e) {
			Logger.debug(this,
					"Current user has no permission to perform the selected action on page '"
							+ contentPage.getIdentifier() + "'");
			throw new DotRuntimeException(
					"Current user has no permission to perform the selected action",
					e);
		}
		return status;
	}

	private void handleEventRecurrence(Map<String, Object> contentletFormData, Contentlet contentlet) throws DotRuntimeException, ParseException{
		if(!contentlet.getStructure().getVelocityVarName().equals(EventAPI.EVENT_STRUCTURE_VAR)){
			return;
		}
		if (contentletFormData.get("recurrenceChanged") != null && Boolean.parseBoolean(contentletFormData.get("recurrenceChanged").toString())) {
			contentlet.setBoolProperty("recurs",true);
			contentlet.setDateProperty("recurrenceStart",eventRecurrenceStartDateF.parse((String)contentletFormData.get("recurrenceStarts")));
			if(contentletFormData.get("noEndDate")==null || (contentletFormData.get("noEndDate")!=null && !Boolean.parseBoolean(contentletFormData.get("noEndDate").toString()))){
				contentlet.setDateProperty("recurrenceEnd",eventRecurrenceEndDateF.parse((String)contentletFormData.get("recurrenceEnds")));
				contentlet.setBoolProperty("noRecurrenceEnd", false);
			}else if(contentletFormData.get("noEndDate")!=null && Boolean.parseBoolean(contentletFormData.get("noEndDate").toString())){
				contentlet.setDateProperty("recurrenceEnd",null);
				contentlet.setBoolProperty("noRecurrenceEnd", true);
			}

			contentlet.setStringProperty("recurrenceDaysOfWeek",contentletFormData.get("recurrenceDaysOfWeek").toString());
		}


			try {
				contentlet.setProperty("recurrenceWeekOfMonth",Long.valueOf(contentletFormData.get("recurrenceWeekOfMonth").toString()));
			} catch (Exception e) {
				contentlet.setProperty("recurrenceWeekOfMonth",1);
			}

			try {
				contentlet.setProperty("recurrenceDayOfWeek",Long.valueOf(contentletFormData.get("recurrenceDayOfWeek").toString()));
			} catch (Exception e) {
				contentlet.setProperty("recurrenceDayOfWeek",1);
			}


			try {
				contentlet.setProperty("recurrenceMonthOfYear",Long.valueOf(contentletFormData.get("recurrenceMonthOfYear").toString()));
			} catch (Exception e) {
				contentlet.setProperty("recurrenceMonthOfYear",1);
			}

			if(contentletFormData.get("recurrenceOccurs") == null){
					contentlet.setBoolProperty("recurs",false);
			}else if (contentletFormData.get("recurrenceOccurs").toString().equals("daily")) {
				contentlet.setLongProperty("recurrenceInterval",Long.valueOf(contentletFormData.get("recurrenceIntervalDaily").toString()));
				contentlet.setStringProperty("recurrenceOccurs",Event.Occurrency.DAILY.toString());
			}else if (contentletFormData.get("recurrenceOccurs").toString().equals("weekly")) {
				contentlet.setProperty("recurrenceInterval",Long.valueOf(contentletFormData.get("recurrenceIntervalWeekly").toString()));
				contentlet.setStringProperty("recurrenceOccurs",Event.Occurrency.WEEKLY.toString());
			}else if (contentletFormData.get("recurrenceOccurs").toString().equals("monthly")){

				   if(Boolean.parseBoolean(contentletFormData.get("isSpecificDate").toString())
						   && UtilMethods.isSet((String) contentletFormData.get("recurrenceDayOfMonth"))){
					   try {
							contentlet.setProperty("recurrenceDayOfMonth",Long.valueOf(contentletFormData.get("recurrenceDayOfMonth").toString()));
						} catch (Exception e) {}

				   } else {
					   contentlet.setProperty("recurrenceDayOfMonth","0");
				   }

				contentlet.setProperty("recurrenceInterval",Long.valueOf(contentletFormData.get("recurrenceIntervalMonthly").toString()));
				contentlet.setStringProperty("recurrenceOccurs",Event.Occurrency.MONTHLY.toString());
			}else if(contentletFormData.get("recurrenceOccurs").toString().equals("annually")){

				 if(UtilMethods.isSet((String) contentletFormData.get("recurrenceDayOfMonth"))){
					   try {
							contentlet.setProperty("recurrenceDayOfMonth",Long.valueOf(contentletFormData.get("recurrenceDayOfMonth").toString()));
						} catch (Exception e) {}

				   }

				contentlet.setProperty("recurrenceInterval",Long.valueOf(contentletFormData.get("recurrenceIntervalYearly").toString()));
				contentlet.setStringProperty("recurrenceOccurs",Event.Occurrency.ANNUALLY.toString());
			}else{
				contentlet.setBoolProperty("recurs",false);
			}


	}

	private void _populateContent(Map<String, Object> contentletFormData,
			User user, Contentlet contentlet, boolean isAutoSave)  throws Exception {

		handleEventRecurrence(contentletFormData, contentlet);

		if(UtilMethods.isSet(contentletFormData.get("identifier")))
			if(UtilMethods.isSet(contentletFormData.get("identifier").toString()) && (!contentletFormData.get("identifier").toString().equalsIgnoreCase(contentlet.getIdentifier()))){
				//exceptionData.append("<li>The content form submission data id different from the content which is trying to be edited</li>");
				throw new DotContentletValidationException("The content form submission data id different from the content which is trying to be edited");
			}

		try {
			//IF EVENT HANDLE RECURRENCE


			String structureInode = contentlet.getStructureInode();
			if (!InodeUtils.isSet(structureInode)) {
				String selectedStructure = (String)contentletFormData.get("selectedStructure");
				if (InodeUtils.isSet(selectedStructure)) {
					structureInode = selectedStructure;
				}
			}
			contentlet.setStructureInode(structureInode);

			if(UtilMethods.isSet(contentletFormData.get("identifier")))
				contentlet.setIdentifier(contentletFormData.get("identifier").toString());

			//http://jira.dotmarketing.net/browse/DOTCMS-3232
			if(UtilMethods.isSet(contentletFormData.get("hostId")))
				contentlet.setHost(APILocator.getHostAPI().findSystemHost(user, false).getIdentifier());

			if(UtilMethods.isSet(contentletFormData.get("folderInode")) && InodeUtils.isSet(contentletFormData.get("folderInode").toString())){
					contentlet.setFolder(APILocator.getFolderAPI().find(contentletFormData.get("folderInode").toString(), user, false).getIdentifier());
			}


			contentlet.setInode(contentletFormData.get("contentletInode").toString());

			if(UtilMethods.isSet(contentletFormData.get("languageId")))
				contentlet.setLanguageId(Long.parseLong(contentletFormData.get("languageId").toString()));

			if(UtilMethods.isSet(contentletFormData.get("reviewInterval")))
				contentlet.setReviewInterval(contentletFormData.get("reviewInterval").toString());

			List<String> disabled = new ArrayList<String>();
			if(UtilMethods.isSet(contentletFormData.get("disabledWysiwyg")))
				CollectionUtils.addAll(disabled, contentletFormData.get("disabledWysiwyg").toString().split(","));

			contentlet.setDisabledWysiwyg(disabled);

			List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
			for (Field field : fields){
				if(fAPI.isElementConstant(field)){
					continue;
				}
				Object value = contentletFormData.get(field.getFieldContentlet());
				String typeField = field.getFieldType();

				if(field.getFieldType().equals(Field.FieldType.TAG.toString())){
					contentlet.setStringProperty(field.getVelocityVarName(), (String) contentletFormData.get(field.getVelocityVarName()));
				}

				//http://jira.dotmarketing.net/browse/DOTCMS-5334
				if(field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())){
					if(field.getFieldContentlet().startsWith("float")
							|| field.getFieldContentlet().startsWith("integer")){

						if(UtilMethods.isSet((String)value)){
							value = String.valueOf(value);
							if(((String)value).endsWith(",")){
								value = ((String)value).substring(0, ((String)value).lastIndexOf(","));
							}
						}else{
							value = "0";
						}

					}
				}
				if(field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())){
					if(field.getFieldContentlet().startsWith("date") && contentletFormData.get("fieldNeverExpire") != null){
						String fieldNeverExpire = contentletFormData.get("fieldNeverExpire").toString();
						Structure structure = CacheLocator.getContentTypeCache().getStructureByInode(contentlet.getStructureInode());
						if(field.getVelocityVarName().equals(structure.getExpireDateVar())){
							if(fieldNeverExpire.equalsIgnoreCase("true")){
								contentlet.getMap().put("NeverExpire", "NeverExpire");
							}else{
								contentlet.getMap().put("NeverExpire", "");
							}
						}
					}
				}

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
				if ((value != null || field.getFieldType().equals(Field.FieldType.BINARY.toString()))
						&& APILocator.getFieldAPI().valueSettable(field)
						&& !field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())
						&& !field.getFieldContentlet().startsWith("system")) {
					conAPI.setContentletProperty(contentlet, field, value);
				}
			}

		} catch (DotContentletStateException e) {
			throw e;
		} catch (Exception e) {
			Logger.error(this, "Unable to populate content. ", e);
			throw new Exception("Unable to populate content");
		}
	}

	private void _handleException(Exception ae, boolean autocommit) {
		
		if(!(ae instanceof DotContentletValidationException) && !(ae instanceof DotLanguageException)){
			Logger.warn(this, ae.toString(), ae);
		}else{
			Logger.debug(this, ae.toString(), ae);
		}

		try {
		    if(autocommit)
		        HibernateUtil.rollbackTransaction();
		} catch (DotHibernateException e) {
			Logger.error(this, e.getMessage());
		}
	}

	private Structure transform(final ContentType contentType) {

		return (null != contentType)?new StructureTransformer(contentType).asStructure():null;
	} // transform.

	protected void _retrieveWebAsset(final Map<String,Object> contentletFormData, final User user) throws Exception {

		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
		HttpServletRequest req =WebContextFactory.get().getHttpServletRequest();

		String inode = (String) contentletFormData.get("contentletInode");

		String inodeStr = (InodeUtils.isSet(inode) ? inode : "");

		Contentlet contentlet = new Contentlet();

		if(InodeUtils.isSet(inodeStr))
		{
			contentlet = conAPI.find(inodeStr, user, false);
		}else {

			/*In case of multi-language first ocurrence new contentlet*/
			String sibblingInode = (String) contentletFormData.get("sibbling");

			if(InodeUtils.isSet(sibblingInode) && !sibblingInode.equals("0")){

				Contentlet sibblingContentlet = conAPI.find(sibblingInode,APILocator.getUserAPI().getSystemUser(), false);

				Logger.debug(UtilHTML.class, "getLanguagesIcons :: Sibbling Contentlet = "+ sibblingContentlet.getInode());

				Identifier identifier = APILocator.getIdentifierAPI().find(sibblingContentlet);

				contentlet.setIdentifier(identifier.getInode());

				String langId = (String) contentletFormData.get("lang");

				if(UtilMethods.isSet(langId)){
					contentlet.setLanguageId(Long.parseLong(langId));
				}

				contentlet.setStructureInode(sibblingContentlet.getStructureInode());
			}
		}

		//if(perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false));
			contentletFormData.put(WebKeys.CONTENTLET_EDIT, contentlet);

		// Contententlets Relationships
		Structure st = contentlet.getStructure();
		if (st == null || !InodeUtils.isSet(st.getInode())) {

			String selectedStructure = "";
			if (UtilMethods.isSet(contentletFormData.get("selectedStructure"))) {
				selectedStructure = (String) contentletFormData.get("selectedStructure");
				st = this.transform(contentTypeAPI.find(selectedStructure));

			}else if (UtilMethods.isSet(contentletFormData.get("sibblingStructure"))) {
				selectedStructure = (String) contentletFormData.get("sibblingStructure");
				st = this.transform(contentTypeAPI.find(selectedStructure));

			}else{
				st = StructureFactory.getDefaultStructure();
			}
		}

		_loadContentletRelationshipsInRequest(contentletFormData, contentlet, st);

		//This parameter is used to determine if the structure was selected from Add/Edit Content link in subnav.jsp, from
		//the Content Search Manager
		if(contentletFormData.get("selected") != null){
			req.getSession().setAttribute("selectedStructure", st.getInode());
		}

		// Asset Versions to list in the versions tab
		contentletFormData.put(WebKeys.VERSIONS_INODE_EDIT, contentlet);
	}

	private void _loadContentletRelationshipsInRequest(Map<String, Object> contentletFormData, Contentlet contentlet, Structure structure) throws DotDataException {
		ContentletAPI contentletService = APILocator.getContentletAPI();
		contentlet.setStructureInode(structure.getInode());
		ContentletRelationships cRelationships = contentletService.getAllRelationships(contentlet);
		contentletFormData.put(WebKeys.CONTENTLET_RELATIONSHIPS_EDIT, cRelationships);
	}

	private void _sendContentletPublishNotification (Contentlet contentlet, HttpServletRequest req) throws Exception,PortalException,SystemException {

		try{
			req.setAttribute(com.liferay.portal.util.WebKeys.LAYOUT,req.getSession().getAttribute(com.dotmarketing.util.WebKeys.LAYOUT));
			req.setAttribute(com.liferay.portal.util.WebKeys.JAVAX_PORTLET_CONFIG,req.getSession().getAttribute(com.dotmarketing.util.WebKeys.JAVAX_PORTLET_CONFIG));

			User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			Map<String, String[]> params = new HashMap<String, String[]> ();
			params.put("struts_action", new String [] {"/ext/contentlet/edit_contentlet"});
			params.put("cmd", new String [] {"edit"});
			params.put("inode", new String [] { String.valueOf(contentlet.getInode()) });
			String contentURL = PortletURLUtil.getActionURL(req, WindowState.MAXIMIZED.toString(), params);
			List<Map<String, Object>> references = conAPI.getContentletReferences(contentlet, currentUser, false);
			List<Map<String, Object>> validReferences = new ArrayList<Map<String, Object>> ();

			//Avoinding to send the email to the same users
			for (Map<String, Object> reference : references){
				try{
					IHTMLPage page = (IHTMLPage)reference.get("page");
					User pageUser = APILocator.getUserAPI().loadUserById(page.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
					if (!pageUser.getUserId().equals(currentUser.getUserId())){
						reference.put("owner", pageUser);
						validReferences.add(reference);
					}
				}catch(Exception ex){
					Logger.debug(this, "the reference has a null page");
				}
			}

			if (validReferences.size() > 0) {
				ContentChangeNotificationThread notificationThread =
					this.new ContentChangeNotificationThread (contentlet, validReferences, contentURL, hostAPI.getCurrentHost(req).getHostname());
				notificationThread.start();
			}

		}catch(Exception ex){
			throw ex;
		}
	}

	//	Contentlet change notifications thread
	private class ContentChangeNotificationThread extends Thread {

		private String serverName;
		private String contentletEditURL;
		private Contentlet contentlet;
		private List<Map<String, Object>> references;

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
				User systemUser = APILocator.getUserAPI().getSystemUser();
				String editorName = UtilMethods.getUserFullName(contentlet.getModUser());

				for (Map<String, Object> reference : references) {
					IHTMLPage page = (IHTMLPage)reference.get("page");
					Host host = APILocator.getHTMLPageAssetAPI().getParentHost(page);
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
						Logger.error(this,e.getMessage());
					}
				}
			}
	}

	/**
	 * Returns the relationships associated to the current contentlet
	 *
	 * @param		req ActionRequest.
	 * @param		user User.
	 * @return		ContentletRelationships.
	 */
	private ContentletRelationships getCurrentContentletRelationships(Map contentletFormData, User user) {

		List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationships.ContentletRelationshipRecords>();
		Set<String> keys = contentletFormData.keySet();
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
				inodesSt = (String) contentletFormData.get(key);
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

		ContentletRelationships result = new ContentletRelationships((Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT), relationshipsRecords);

		return result;
	}

	private ContentletRelationships retrieveRelationshipsData(Contentlet currentContentlet, User user, Map<String, Object> contentletFormData ){

		Set<String> keys = contentletFormData.keySet();

		ContentletRelationships relationshipsData = new ContentletRelationships(currentContentlet);
		List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationshipRecords> ();
		relationshipsData.setRelationshipsRecords(relationshipsRecords);

		for (String key : keys) {
			if (key.startsWith("rel_") && key.endsWith("_inodes")) {
				boolean hasParent = key.contains("_P_");
				String inodesSt = (String) contentletFormData.get(key);
				if(!UtilMethods.isSet(inodesSt)){
					continue;
				}
				String[] inodes = inodesSt.split(",");

				Relationship relationship = (Relationship) InodeFactory.getInode(inodes[0], Relationship.class);
				ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
				ArrayList<Contentlet> cons = new ArrayList<Contentlet>();
				for (String inode : inodes) {
					/*long i = 0;
					try{
						i = Long.valueOf(inode);
					}catch (Exception e) {
						Logger.error(this, "Relationship not a number value : ",e);
					}*/
					if(relationship.getInode().equalsIgnoreCase(inode)){
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

	public void cancelContentEdit(String workingContentletInode,
			String currentContentletInode,User user) throws Exception {

		HibernateUtil.startTransaction();
		HttpServletRequest req =WebContextFactory.get().getHttpServletRequest();

		try {

			Logger.debug(this, "Calling Unlock Method");

            // http://jira.dotmarketing.net/browse/DOTCMS-1073
			// deleting uploaded files from temp binary path
			/*Logger.debug(this, "Deleting uploaded files");

			java.io.File tempUserFolder = new java.io.File(Config.CONTEXT.
													getRealPath(com.dotmarketing.util.Constants.TEMP_BINARY_PATH)
													+ java.io.File.separator + user.getUserId());

			FileUtil.deltree(tempUserFolder);*/

			if(InodeUtils.isSet(workingContentletInode) ){

				Contentlet workingContentlet = conAPI.find(workingContentletInode, user, false);

				if(perAPI.doesUserHavePermission(workingContentlet, PermissionAPI.PERMISSION_WRITE, user)) {

					if(InodeUtils.isSet(currentContentletInode)){
						conAPI.restoreVersion(workingContentlet, user, false);
					}

					conAPI.unlock(workingContentlet, user, false);
					SessionMessages.add(req, "message", "message.contentlet.unlocked");

				}
			}

			if(InodeUtils.isSet(currentContentletInode)){

				Contentlet currentContentlet = conAPI.find(currentContentletInode, user, false);

				// Deleting auto saved version of a New Content upon "Cancel".
				if(!InodeUtils.isSet(workingContentletInode)&& InodeUtils.isSet(currentContentletInode)){
					conAPI.delete(currentContentlet, user, false, true);
					//conAPI.reindex(currentContentlet);
				}

				// Deleting auto saved version of an Existing Content upon "Cancel".
				/*  Commenting as this makes the content to disappear when editing from HTML PAGE
				 * if(workingContentletInode > 0 && currentContentletInode > 0 ){
					conAPI.delete(currentContentlet, user, false, false);
				}*/

			}

		} catch (Exception ae) {
			HibernateUtil.rollbackTransaction();
			SessionMessages.add(req, "message", "message.contentlets.batch.deleted.error");
			throw ae;
		}
		HibernateUtil.closeAndCommitTransaction();
	}
}
