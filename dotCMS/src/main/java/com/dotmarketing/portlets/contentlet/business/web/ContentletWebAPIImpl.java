package com.dotmarketing.portlets.contentlet.business.web;

import com.dotcms.api.system.event.ContentletSystemEventUtil;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotLanguageException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.liferay.util.servlet.SessionMessages;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dotmarketing.portlets.contentlet.util.ActionletUtil.hasPushPublishActionlet;
import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER;
import static com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet.getEnvironmentsToSendTo;

/**
 * Implementation class for the {@link ContentletWebAPI} class.
 */
public class ContentletWebAPIImpl implements ContentletWebAPI {

	private CategoryAPI catAPI;
	private PermissionAPI perAPI;
	private ContentletAPI conAPI;
	private FieldAPI fAPI;
	private HostWebAPI hostAPI;

	private UserAPI userAPI;
	private FolderAPI folderAPI;
	private IdentifierAPI identAPI;

	private static DateFormat eventRecurrenceStartDateF = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static DateFormat eventRecurrenceEndDateF = new SimpleDateFormat("yyyy-MM-dd");
	private static final boolean RESPECT_FRONT_END_ROLES = Boolean.TRUE;
    private static final boolean DONT_RESPECT_FRONT_END_ROLES = Boolean.FALSE;

	private final ContentletSystemEventUtil contentletSystemEventUtil;

	public ContentletWebAPIImpl() {
		catAPI = APILocator.getCategoryAPI();
		perAPI = APILocator.getPermissionAPI();
		conAPI = APILocator.getContentletAPI();
		fAPI = APILocator.getFieldAPI();
		hostAPI = WebAPILocator.getHostWebAPI();
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

	@WrapInTransaction
	public String saveContent(final Map<String, Object> contentletFormData,
			  final boolean isAutoSave, final boolean isCheckin, final User user, final boolean generateSystemEvent) throws DotContentletValidationException, Exception {


		final HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		Logger.debug(this, () -> "############################# Contentlet");

		try {

			Logger.debug(this, () -> "Calling Retrieve method");
			retrieveWebAsset(contentletFormData, user);
		} catch (Exception ae) {

			handleException(ae);
			throw new Exception(ae.getMessage());
		}

		Contentlet contentlet = null;
		final boolean isNew   = isNew(contentletFormData);

		try {

			Logger.debug(this, () -> "Calling Save Method");

			try {

				saveWebAsset(contentletFormData, isAutoSave, isCheckin, user, generateSystemEvent);
			} catch (Exception ce) {
				if (!isAutoSave)
					SessionMessages.add(req, "message.contentlet.save.error");
				throw ce;
			}

			Logger.debug(this, () -> "HTMLPage inode=" + contentletFormData.get("htmlpage_inode"));
			Logger.debug(this, () -> "Container inode=" + contentletFormData.get("contentcontainer_inode"));

			if (InodeUtils.isSet((String) contentletFormData.get("htmlpage_inode"))
					&& InodeUtils.isSet((String) contentletFormData.get("contentcontainer_inode"))) {

				try {

					Logger.debug(this, () -> "I'm setting my contentlet parents");
					_addToParents(contentletFormData, user, isAutoSave);
				} catch (DotSecurityException e) {
					throw new DotSecurityException(e.getMessage());
				} catch (Exception ae) {
					throw ae;
				}
			}

			contentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);

			// finally we unlock the asset as the lock attribute is
			// attached to the identifier rather than contentlet as
			// before DOTCMS-6383
			//conAPI.unlock(cont, user, false);
			if (null != contentlet) {
				this.pushSaveEvent(contentlet, isNew);
			}
		} catch (Exception ae) {
			handleException(ae);
			throw ae;
		}

		contentletFormData.put("cache_control", "0");
		return ((contentlet!=null) ? contentlet.getInode() : null);
	}

	private void pushSaveEvent (final Contentlet eventContentlet, final boolean eventCreateNewVersion) throws DotHibernateException {

		HibernateUtil.addCommitListener(() -> {
				this.contentletSystemEventUtil.pushSaveEvent(eventContentlet, eventCreateNewVersion);
		}, 1000);
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
					MultiTree multiTree = APILocator.getMultiTreeAPI().getMultiTree(htmlParentId, containerIdentifier,
							contenletIdentifier, MultiTree.LEGACY_RELATION_TYPE); // todo: check if needs to persona
					Logger.debug(this, "Getting multitree for=" + htmlpage_inode + " ," + containerParent.getInode()
							+ " ," + contentlet.getIdentifier());
					Logger.debug(this, "Coming from multitree parent1=" + multiTree.getParent1() + " parent2="
							+ multiTree.getParent2());

					int contentletCount = APILocator.getMultiTreeAPI().getMultiTrees(htmlParentId).size();

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
							Optional<ContentletVersionInfo> versionInfo = APILocator
									.getVersionableAPI()
									.getContentletVersionInfo(pageIdentifier,
											contentletLang);
							if (versionInfo.isPresent()) {
							    APILocator.getMultiTreeAPI().saveMultiTree(mTree);
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
						    APILocator.getMultiTreeAPI().saveMultiTree(mTree);
						}
					}

				}
			}

			if(!isAutoSave)
			SessionMessages.add(req, "message", "message.contentlet.add.parents");
		}
	}

	private void  saveWebAsset(Map<String, Object> contentletFormData,
			boolean isAutoSave, boolean isCheckin, User user, boolean generateSystemEvent) throws Exception, DotContentletValidationException {

		final HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
		final Set contentletFormKeys     = contentletFormData.keySet();//To replace req.getParameterValues()

		// Getting the contentlets variables to work
		Contentlet currentContentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);

        if (!APILocator.getContentTypeAPI(user).isContentTypeAllowed(currentContentlet.getContentType())) {
            SessionMessages.add(request, "message", "An enterprise license is required to perform this operation");
            throw new DotSecurityException("An enterprise license is required to perform this operation");
        }
		final Map<String, Object> oldContentletMap = currentContentlet.getMap();
		//Form doesn't always contain this value upfront. And since populateContentlet sets 0 we better set it upfront
		contentletFormData.put(Contentlet.IDENTIFIER_KEY, currentContentlet.getIdentifier());
		final boolean isNew = !InodeUtils.isSet(currentContentlet.getInode());

		if (!isNew && Host.HOST_VELOCITY_VAR_NAME.equals(currentContentlet.getStructure().getVelocityVarName())) {
			currentContentlet = conAPI.checkout(currentContentlet.getInode(), user, false);
		}

		/***
		 * Workflow
		 */
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, (String) contentletFormData.get(Contentlet.WORKFLOW_ACTION_KEY));
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, (String) contentletFormData.get(Contentlet.WORKFLOW_COMMENTS_KEY));
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, (String) contentletFormData.get(Contentlet.WORKFLOW_ASSIGN_KEY));

		/**
		 * Push Publishing Actionlet
		 */
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE, (String) contentletFormData.get(Contentlet.WORKFLOW_PUBLISH_DATE));
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME, (String) contentletFormData.get(Contentlet.WORKFLOW_PUBLISH_TIME));
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_TIMEZONE_ID, (String) contentletFormData.get(Contentlet.WORKFLOW_TIMEZONE_ID));
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE, (String) contentletFormData.get(Contentlet.WORKFLOW_EXPIRE_DATE));
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME, (String) contentletFormData.get(Contentlet.WORKFLOW_EXPIRE_TIME));
		currentContentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE, (String) contentletFormData.get(Contentlet.WORKFLOW_NEVER_EXPIRE));
		currentContentlet.setStringProperty(Contentlet.WHERE_TO_SEND, (String) contentletFormData.get(Contentlet.WHERE_TO_SEND));
		currentContentlet.setStringProperty(Contentlet.FILTER_KEY, (String) contentletFormData.get(Contentlet.FILTER_KEY));
		currentContentlet.setStringProperty(Contentlet.I_WANT_TO, (String) contentletFormData.get(Contentlet.I_WANT_TO));

		if (UtilMethods.isSet(contentletFormData.get("wfPathToMove"))) {
			currentContentlet.setStringProperty(Contentlet.PATH_TO_MOVE, (String) contentletFormData.get("wfPathToMove"));
		}


		contentletFormData.put(WebKeys.CONTENTLET_FORM_EDIT, currentContentlet);
		contentletFormData.put(WebKeys.CONTENTLET_EDIT, currentContentlet);

		try{
			populateContent(contentletFormData, user, currentContentlet, isAutoSave);
			//http://jira.dotmarketing.net/browse/DOTCMS-1450
			//The form doesn't have the identifier in it. so the populate content was setting it to 0
			//currentContentlet.setIdentifier(currentContentident);
			if(UtilMethods.isSet(contentletFormData.get("new_owner_permissions"))) {
				currentContentlet.setOwner((String) contentletFormData.get("new_owner_permissions"));
			}
		}catch (DotContentletValidationException ve) {
			throw new DotContentletValidationException(ve.getMessage());
		}

		final String subCommand = UtilMethods.isSet(contentletFormData.get("subcmd"))?
				(String) contentletFormData.get("subcmd"): StringPool.BLANK;

		final ArrayList<Category> categories   = new ArrayList<>();
		// Getting categories that come from the entity
		final ArrayList<String> categoriesList = new ArrayList<>();
		Host host =null;
		Folder folder = null;

		for (final Iterator iterator = contentletFormKeys.iterator(); iterator.hasNext();) {

			final String elementName = (String) iterator.next();
			if(elementName.startsWith("categories") && elementName.endsWith("_")) {

				categoriesList.add((String)contentletFormData.get(elementName));
			}

			//http://jira.dotmarketing.net/browse/DOTCMS-3232
			if("hostId".equalsIgnoreCase(elementName) &&
					InodeUtils.isSet(contentletFormData.get(elementName).toString())) {

				final String hostId = contentletFormData.get(elementName).toString();
				host = hostAPI.find(hostId, user, false);
				if(host == null) {
					host = new Host();
				}

				if(!perAPI.doesUserHavePermission(host,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)){
					SessionMessages.add(request, "message", "User needs 'Add Children' Permissions on selected host");
					throw new DotSecurityException("User has no Add Children Permissions on selected host");
				}
				currentContentlet.setHost(hostId);
				currentContentlet.setFolder(SYSTEM_FOLDER);
			}

			if("folderInode".equalsIgnoreCase(elementName) &&
					InodeUtils.isSet(contentletFormData.get(elementName).toString())) {

				final String folderInode = contentletFormData.get(elementName).toString();
				folder = this.folderAPI.find(folderInode, user, true);

				if(isNew && !perAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)) {
					SessionMessages.add(request, "message", "User needs 'Add Children Permissions' on selected folder");
					throw new DotSecurityException("User has no Add Children Permissions on selected folder");
				}
				currentContentlet.setHost(folder.getHostId());
				currentContentlet.setFolder(folderInode);
			}
		 }

		if (UtilMethods.isSet(categoriesList)) {
			for (final Iterator<String> iterator = categoriesList.iterator(); iterator.hasNext();) {
				final String categoryId = iterator.next();
				if (UtilMethods.isSet(categoryId)) {
					categories.add(catAPI.find(categoryId, user, PageMode.get().respectAnonPerms));
				}
			}
		}

		try {

			final ContentletRelationships contentletRelationships =
					retrieveRelationshipsData(currentContentlet,user, contentletFormData );

			// http://jira.dotmarketing.net/browse/DOTCMS-65
			// Coming from other contentlet to relate it automatically
			final String relateWith   = (UtilMethods.isSet(contentletFormData.get("relwith")))?
				(String) contentletFormData.get("relwith"): null;

			final String relationType = (UtilMethods.isSet(contentletFormData.get("reltype")))?
				(String) contentletFormData.get("reltype"): null;

			final String relationHasParent = (String) contentletFormData.get("relisparent");
			if(relateWith != null){
				try {

					final List<ContentletRelationshipRecords> recordsList =
							contentletRelationships.getRelationshipsRecords();
					for(final ContentletRelationshipRecords records : recordsList) {

						if ((!records.getRelationship().getRelationTypeValue().equals(relationType)) ||
							(APILocator.getRelationshipAPI().sameParentAndChild(records.getRelationship()) &&
								((!records.isHasParent() && relationHasParent.equals("no")) ||
								 (records.isHasParent() && relationHasParent.equals("yes"))))) {
							continue;
						}

						records.getRecords().add(conAPI.find(relateWith, user, false));
					}


				} catch (Exception e) {
					Logger.error(this,"Contentlet failed while creating new relationship",e);
				}
			}

			if(com.dotmarketing.util.Constants.PUBLISH.equals(subCommand)) {
				currentContentlet.setBoolProperty("live", true);
			}

			// Perform some validations before saving it
			if (currentContentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {

				final String status = validateNewContentPage(currentContentlet);
				if (UtilMethods.isSet(status)) {
					final String msg = LanguageUtil.get(user, status);
					throw new DotRuntimeException(msg);
				}
			}

			if (shouldUseWorkflow(contentletFormData)) {

				final ContentletDependencies.Builder contentletDependencies =
						new ContentletDependencies.Builder().respectAnonymousPermissions(PageMode.get(request).respectAnonPerms)
								.modUser(user)
								.relationships(contentletRelationships)
								.workflowActionId((String) contentletFormData.get("wfActionId"))
								.workflowActionComments((String) contentletFormData.get("wfActionComments"))
								.workflowAssignKey((String) contentletFormData.get("wfActionAssign"))
								.categories(categories)
								.indexPolicy(IndexPolicyProvider.getInstance().forSingleContent())
								.generateSystemEvent(generateSystemEvent);

				if (UtilMethods.isSet((String) contentletFormData.get("wfFilterKey"))) {

					contentletDependencies.workflowFilterKey((String) contentletFormData.get("wfFilterKey"));
				}

				if (UtilMethods.isSet((String) contentletFormData.get("wfTimezoneId"))) {

					contentletDependencies.workflowTimezoneId((String) contentletFormData.get("wfTimezoneId"));
				}

				if (UtilMethods.isSet((String) contentletFormData.get("wfiWantTo"))) {

					contentletDependencies.workflowIWantTo((String) contentletFormData.get("wfiWantTo"));
				}

				currentContentlet = APILocator.getWorkflowAPI().fireContentWorkflow(currentContentlet, contentletDependencies.build());

				if (hasPushPublishActionlet(APILocator.getWorkflowAPI().findAction((String) contentletFormData.get("wfActionId"), user))) {
					final String whoToSendTmp = (String)currentContentlet.get(Contentlet.WHERE_TO_SEND);
					final List<Environment> envsToSendTo = getEnvironmentsToSendTo(whoToSendTmp);
					request.getSession().setAttribute( WebKeys.SELECTED_ENVIRONMENTS + user.getUserId(), envsToSendTo );
				}
			} else {

				Logger.warn(this, "Calling Save Web Asset: " + currentContentlet.getIdentifier() +
								", without an action.");
				if (currentContentlet.isHost()) {

					Logger.info(this, "Saving the Host");
					currentContentlet.setInode(null);
                    currentContentlet.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
					currentContentlet = this.conAPI.checkin
							(currentContentlet, contentletRelationships, categories, null, user, false, generateSystemEvent);

					if (!currentContentlet.isNew() &&
							!currentContentlet.getTitle().equals(oldContentletMap.get(Host.HOST_NAME_KEY))) {
						UpdateContainersPathsJob.triggerUpdateContainersPathsJob(
								oldContentletMap.get(Host.HOST_NAME_KEY).toString(),
								(String) currentContentlet.get("hostName")
						);
						UpdatePageTemplatePathJob.triggerUpdatePageTemplatePathJob(
								oldContentletMap.get(Host.HOST_NAME_KEY).toString(),
								(String) currentContentlet.get("hostName")
						);
					}
				} else {
					Logger.warn(this, "Calling Save Web Asset: " + currentContentlet.getIdentifier() +
							", without an action is just allow to Host.");
				}
			}
		} catch(DotContentletValidationException ve) {

			Logger.warnAndDebug(this.getClass(), ve.getMessage(), ve);
			throw ve;
		}

		/*
		We can not expect to always have a contentlet after executing a Workflow Action as
		for example if we run the delete actionlet we will have no contentlet to process at this point
		 */
		if (null != currentContentlet) {

			currentContentlet.setStringProperty("wfActionComments",
					(String) contentletFormData.get("wfActionComments"));
			currentContentlet.setStringProperty("wfActionAssign",
					(String) contentletFormData.get("wfActionAssign"));

			contentletFormData.put(WebKeys.CONTENTLET_EDIT, currentContentlet);
			contentletFormData.put(WebKeys.CONTENTLET_FORM_EDIT, currentContentlet);

			if (Config.getBooleanProperty("CONTENT_CHANGE_NOTIFICATIONS", false) && !isNew
					&& !isAutoSave) {

				_sendContentletPublishNotification(currentContentlet, request);
			}

			if (!isAutoSave) {

				SessionMessages.add(request, "message", "message.contentlet.save");
			}

			if ((subCommand != null) && Constants.PUBLISH.equals(subCommand)) {

				APILocator.getVersionableAPI().setLive(currentContentlet);
				if (!isAutoSave) {

					SessionMessages.add(request, "message", "message.contentlet.published");
				}
			}
		} else {
			/*
			No contentlet was returned (probably a delete was executed).
			Marking it for upper layers.
			 */
			contentletFormData.put(WebKeys.CONTENTLET_DELETED, Boolean.TRUE);
		}
	}

	/**
	 * return true if should use workflow with to save the contentlet
	 * @param contentletFormData
	 * @return
	 */
	private boolean shouldUseWorkflow(Map<String, Object> contentletFormData) {
		return UtilMethods.isSet((String) contentletFormData.get("wfActionId"));
	}

    @Override
	public String validateNewContentPage(final Contentlet contentPage) {
		final String parentFolderId = contentPage.getFolder();
        final String pageUrl = !UtilMethods.isSet(contentPage.getMap().get(PageContentType.PAGE_URL_FIELD_VAR)) ?
                StringUtils.EMPTY : contentPage.getMap().get(PageContentType.PAGE_URL_FIELD_VAR).toString();
        String status = null;
		try {
            final Folder parentFolder = this.folderAPI.find(parentFolderId, this.userAPI.getSystemUser(), DONT_RESPECT_FRONT_END_ROLES);
            if (parentFolder != null && InodeUtils.isSet(parentFolder.getInode())) {
				final String siteId = SYSTEM_FOLDER.equals(parentFolder.getInode()) ? contentPage.getHost() :
						parentFolder.getHostId();
				final Host site = this.hostAPI.find(siteId, this.userAPI.getSystemUser(), RESPECT_FRONT_END_ROLES);
				String parentFolderPath = parentFolder.getPath();
				if (UtilMethods.isSet(parentFolderPath)) {
					if (!parentFolderPath.startsWith(StringPool.SLASH)) {
						parentFolderPath = StringPool.SLASH + parentFolderPath;
					}
					if (!parentFolderPath.endsWith(StringPool.SLASH)) {
						parentFolderPath = parentFolderPath + StringPool.SLASH;
					}
					final String fullPageUrl = parentFolderPath + pageUrl;
					if (!pageUrl.endsWith(".html")) {
						final List<Identifier> folders = this.identAPI
								.findByURIPattern("folder", fullPageUrl,true, site);
						if (!folders.isEmpty()) {
							// Found a folder with same path
							status = "message.htmlpage.error.htmlpage.exists.folder";
						}
					}
					if (!UtilMethods.isSet(status)) {
						final Identifier identifier = this.identAPI.find(site, fullPageUrl);
						if (identifier != null && InodeUtils.isSet(identifier.getId())) {
							try {
                                final Contentlet existingContent = this.conAPI.findContentletByIdentifier(identifier
                                        .getId(), true, contentPage.getLanguageId(), this.userAPI.getSystemUser(),
                                        DONT_RESPECT_FRONT_END_ROLES);
								if (null != existingContent) {
                                    if (existingContent.getContentType().baseType().equals(BaseContentType.FILEASSET)
                                            && !existingContent.getIdentifier().equals(contentPage.getIdentifier())) {
										// Found a file asset with same path
										status = "message.htmlpage.error.htmlpage.exists.file";
									} else if (!existingContent.getIdentifier().equals(contentPage.getIdentifier())) {
										// Found page with same path and language
										status = "message.htmlpage.error.htmlpage.exists";
									}
								}
							} catch (final DotContentletStateException e) {
								// If it's a brand new page...
								if (!UtilMethods.isSet(contentPage.getIdentifier())) {
									// Found page with same path
									status = "message.htmlpage.error.htmlpage.exists";
								}
							}
						}
					}
				}
			}
        } catch (final DotDataException e) {
            final String errorMsg = String.format("An error occurred when validating page '%s' [%s]: %s", pageUrl,
                    contentPage.getIdentifier(), e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        } catch (final DotSecurityException e) {
            final String errorMsg = String.format("The current user has no permission to perform the selected action " +
                    "on page '%s' [%s]: %s", pageUrl, contentPage.getIdentifier(), e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
		}
		return status;
	}

	private void handleEventRecurrence(final Map<String, Object> contentletFormData, final Contentlet contentlet) throws DotRuntimeException, ParseException, DotDataException, DotSecurityException{
		if(!contentlet.isCalendarEvent()){
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
					   contentlet.setProperty("recurrenceDayOfMonth",0);
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

	private Contentlet populateContent(Map<String, Object> contentletFormData,
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

			if(UtilMethods.isSet(contentletFormData.get(VariantAPI.VARIANT_KEY))) {
				final String variantName = contentletFormData.get(VariantAPI.VARIANT_KEY).toString();

				final Variant variant = APILocator.getVariantAPI().get(variantName)
						.orElse(VariantAPI.DEFAULT_VARIANT);

				contentlet.setVariantId(variant.name());
			}

			List<String> disabled = new ArrayList<>();
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
			final String message = String.format("Unable to populate content: %s", e.getMessage());
			Logger.error(this, message, e);
			throw new Exception(message, e);
		}

		return contentlet;
	}

	/**
	 * Prints more or less details of the specified Exception depending on its original cause. For
	 * more verbose exceptions such as {@link DotContentletValidationException} or
	 * {@link DotLanguageException}, all details have already made it to the log, so those are not
	 * required.
	 *
	 * @param exception The exception to handle.
	 */
	private void handleException(final Exception exception) {
		if (ExceptionUtil.causedBy(exception, DotContentletValidationException.class) || ExceptionUtil.causedBy(exception, DotLanguageException.class)) {
			Logger.debug(this, exception.toString(), exception);
		} else {
			Logger.warnAndDebug(this.getClass(), exception.toString(), exception);
		}
	}

	private Structure transform(final ContentType contentType) {

		return (null != contentType)?new StructureTransformer(contentType).asStructure():null;
	} // transform.

	private void retrieveWebAsset(final Map<String,Object> contentletFormData, final User user) throws Exception {

		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
		HttpServletRequest req =WebContextFactory.get().getHttpServletRequest();

		String inode = (String) contentletFormData.get("contentletInode");

		String inodeStr = (InodeUtils.isSet(inode) ? inode : "");

		Contentlet contentlet = new Contentlet();

		if(InodeUtils.isSet(inodeStr))
		{
			Contentlet contentletFind = conAPI.find(inodeStr, user, false);

			if(contentletFind != null) {

				final String variantNameFormData = UtilMethods.isSet(contentletFormData.get(VariantAPI.VARIANT_KEY)) ?
						contentletFormData.get(VariantAPI.VARIANT_KEY).toString() :
						VariantAPI.DEFAULT_VARIANT.name();

				contentlet.getMap().putAll(contentletFind.getMap());

				if (!contentletFind.getVariantId().equals(variantNameFormData)) {
					contentletFormData.put("contentletInode", StringPool.BLANK);
					creatingNewVersion(inodeStr, contentletFormData, contentlet);
				}
			} else{
				throw new NotFoundInDbException("contentlet with that inode does not exist");
			}
		}else {

			/*In case of multi-language first ocurrence new contentlet*/
			String sibblingInode = (String) contentletFormData.get("sibbling");
			creatingNewVersion(sibblingInode, contentletFormData, contentlet);
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

		loadContentletRelationshipsInRequest(contentletFormData, contentlet, st);


		// Asset Versions to list in the versions tab
		contentletFormData.put(WebKeys.VERSIONS_INODE_EDIT, contentlet);
	}

	private void creatingNewVersion(final String inode, final Map<String, Object> contentletFormData, final Contentlet contentlet)
			throws DotDataException, DotSecurityException {


		if(InodeUtils.isSet(inode) && !inode.equals("0")){

			Contentlet sibblingContentlet = conAPI.find(inode,APILocator.getUserAPI().getSystemUser(), false);

			Logger.debug(UtilHTML.class, "getLanguagesIcons :: Sibbling Contentlet = "+ sibblingContentlet.getInode());

			Identifier identifier = APILocator.getIdentifierAPI().find(sibblingContentlet);

			contentlet.setIdentifier(identifier.getInode());

			String langId = (String) contentletFormData.get("lang");

			if(UtilMethods.isSet(langId)){
				contentlet.setLanguageId(Long.parseLong(langId));
			}

			final String variantName = (String) contentletFormData.get(VariantAPI.VARIANT_KEY);

			if(UtilMethods.isSet(variantName)){
				contentlet.setVariantId(variantName);
			}

			contentlet.setStructureInode(sibblingContentlet.getStructureInode());
		}
	}

	private void loadContentletRelationshipsInRequest(Map<String, Object> contentletFormData, Contentlet contentlet, Structure structure) throws DotDataException {
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
			Map<String, String[]> params = new HashMap<> ();
			params.put("struts_action", new String [] {"/ext/contentlet/edit_contentlet"});
			params.put("cmd", new String [] {"edit"});
			params.put("inode", new String [] { String.valueOf(contentlet.getInode()) });
			String contentURL = PortletURLUtil.getActionURL(req, WindowState.MAXIMIZED.toString(), params);
			List<Map<String, Object>> references = conAPI.getContentletReferences(contentlet, currentUser, false);
			List<Map<String, Object>> validReferences = new ArrayList<> ();

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



		}catch(Exception ex){
			throw ex;
		}
	}



	/**
	 * Returns the relationships associated to the current contentlet
	 *
	 * @param		contentletFormData Contentlet form map.
	 * @param		user User.
	 * @return		ContentletRelationships.
	 */
	private ContentletRelationships getCurrentContentletRelationships(Map contentletFormData, User user) {

		List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<>();
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

		ContentletRelationships result = new ContentletRelationships((Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT), relationshipsRecords);

		return result;
	}

	private ContentletRelationships retrieveRelationshipsData(Contentlet currentContentlet, User user, Map<String, Object> contentletFormData ){

		final Set<String> keys = contentletFormData.keySet();

		ContentletRelationships relationshipsData = null;
		List<ContentletRelationshipRecords> relationshipsRecords = null;

		for (String key : keys) {
			if (key.startsWith("rel_") && key.endsWith("_inodes")) {

                final String inodesSt = (String) contentletFormData.get(key);
                if(!UtilMethods.isSet(inodesSt)){
                    continue;
                }

                final String[] inodes = inodesSt.split(",");

                final Relationship relationship = APILocator.getRelationshipAPI().byInode(inodes[0]);
                final boolean isRelatingNewContent = isRelatingNewContent(contentletFormData,
                        relationship);

                //This boolean determines if the contentletFormData is a parent or a child
				//This is the proper behaviour of the boolean:
				//if you are relating a new child should be false, because the contentletFormData is a child
				//if you are relating a new parent should be true, because the contentletFormData is a parent
				//if you are relating an existing child should be true, because the contentletFormData is a parent
				//if you are relating an existing parent should be false, because the contentletFormData is a child
				final boolean isContentletAParent = (key.contains("_P_")) ? !isRelatingNewContent : isRelatingNewContent;

                final String relationHasParent =
                        contentletFormData.get("relisparent") != null ? (String) contentletFormData
                                .get("relisparent") : "";

				if (relationshipsData == null){
					relationshipsData = new ContentletRelationships(currentContentlet);
					relationshipsRecords = new ArrayList<> ();
					relationshipsData.setRelationshipsRecords(relationshipsRecords);
				}
				final ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(
						relationship, isContentletAParent);
				final ArrayList<Contentlet> cons = new ArrayList<>();
				for (String inode : inodes) {
					if(relationship.getInode().equalsIgnoreCase(inode) ||
                            (APILocator.getRelationshipAPI().sameParentAndChild(records.getRelationship()) &&
                                    ((!records.isHasParent() && relationHasParent.equals("no")) ||
                                            (records.isHasParent() && relationHasParent.equals("yes"))))){
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
     * When you click the option Relate New Content, it adds the relwith key with the id of the contentlet that you edited at first and reltype with the relationship name.
     * Also, it is important to verify that the reltype applies to the relationship that we are currently populating (case when there are multiple relationships).
     * View issue: https://github.com/dotCMS/core/issues/17743
     * @param contentletFormData
     * @param relationship
     * @return
     */
    private boolean isRelatingNewContent(Map<String, Object> contentletFormData,
            Relationship relationship) {

        return contentletFormData.containsKey("relwith") && relationship
                .getRelationTypeValue().equals(contentletFormData.get("reltype"));
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
