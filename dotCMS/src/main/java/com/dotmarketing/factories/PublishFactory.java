package com.dotmarketing.factories;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.rendering.velocity.services.TemplateLoader;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

import io.vavr.API;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

/**
 *
 * @author  maria, david (2005)
 */
public class PublishFactory {

	private static PermissionAPI permissionAPI  = APILocator.getPermissionAPI();
	private static SystemMessageEventUtil systemMessageEventUtil;

	static {
		systemMessageEventUtil = SystemMessageEventUtil.getInstance();
	}

	@VisibleForTesting
	public static void setSystemMessageEventUtil(final SystemMessageEventUtil systemMessageEventUtil) {
		PublishFactory.systemMessageEventUtil = systemMessageEventUtil;
	}

	/**
	 * @param permissionAPIRef the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

    /**
     * This method publish a given and asset and its related assets, if a user is passed the method will check permissions to publish,
     * if the user doesn't have permission to publish one of the related assets then that one will be skipped.
     *
     * @param webAsset
     * @param req
     * @return
     * @throws WebAssetException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public static boolean publishAsset(Inode webAsset,HttpServletRequest req) throws WebAssetException, DotSecurityException, DotDataException {
		User user;
		try {
			user = PortalUtil.getUser(req);
		} catch (Exception e1) {
			Logger.error(PublishFactory.class, "publishAsset: Cannot obtain the user from the request.", e1);
			return false;
		}

		return publishAsset(webAsset, user, false);
	}


	/**
	 * This method publish a given folder and its related assets, if a user is passed the method will check permissions to publish,
	 * if the user doesn't have permission to publish one of the related assets then that one will be skipped.
	 *
	 * @param folder
	 * @param req
	 * @return
	 * @throws WebAssetException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public static boolean publishAsset(Folder folder,HttpServletRequest req) throws WebAssetException, DotSecurityException, DotDataException {
		User user;
		try {
			user = PortalUtil.getUser(req);
		} catch (Exception e1) {
			Logger.error(PublishFactory.class, "publishAsset: Cannot obtain the user from the request.", e1);
			return false;
		}

		return publishAsset(folder, user, false, true);
	}
    /**
     * Publishes a given html page (if is HTMLPageAsset) and its related content (Applies to the legacy and new HTML pages).<br/>
     * <strong>NOTE: </strong> Don't call this method directly for legacy HTMLPages, instead call publishAsset, that publishAsset method will
     * call this method after publish the legacy HTMLPage.
     *
     * @param htmlPage
     * @param req
     * @return
     * @throws WebAssetException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public static boolean publishHTMLPage ( IHTMLPage htmlPage, HttpServletRequest req ) throws WebAssetException, DotSecurityException, DotDataException {
        return publishHTMLPage( htmlPage, new java.util.ArrayList(), req );
    }

    /**
     * Publishes a given html page (if is HTMLPageAsset) and its related content (Applies to the legacy and new HTML pages).<br/>
     * <strong>NOTE: </strong> Don't call this method directly for legacy HTMLPages, instead call publishAsset, that publishAsset method will
     * call this method after publish the legacy HTMLPage.
     *
     * @param htmlPage
     * @param relatedNotPublished
     * @param req
     * @return
     * @throws WebAssetException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public static boolean publishHTMLPage ( IHTMLPage htmlPage, List relatedNotPublished, HttpServletRequest req ) throws WebAssetException, DotSecurityException, DotDataException {
        User user;
        try {
            user = PortalUtil.getUser( req );
        } catch ( Exception e1 ) {
            Logger.error( PublishFactory.class, "publishAsset: Cannot obtain the user from the request.", e1 );
            return false;
        }

        return publishHTMLPage( htmlPage, relatedNotPublished, user, false );
    }


	/**
	 * This method publish a given and asset and its related assets
	 * if a user is passed the method will check permissions to publish
	 * if the user doesn't have permission to publish one of the related assets
	 * then that one will be skipped
	 * @param webAsset
	 * @param user
	 * @param respectFrontendRoles TODO
	 * @return
	 * @throws WebAssetException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@SuppressWarnings("unchecked")
	public static boolean publishAsset(Inode webAsset, User user, boolean respectFrontendRoles) throws WebAssetException, DotSecurityException, DotDataException 
	{
  
		return publishAsset(webAsset,user,respectFrontendRoles, true);

	}

	/**
	 * This method publish a given and asset and its related assets
	 * if a user is passed the method will check permissions to publish
	 * if the user doesn't have permission to publish one of the related assets
	 * then that one will be skipped
	 * @param folder
	 * @param user
	 * @param respectFrontendRoles
	 * @param isNewVersion  - if passed false then the webasset's mod user and mod date will NOT be altered. @see {@link ContentletAPI#checkinWithoutVersioning(Contentlet, java.util.Map, List, List, User, boolean)}checkinWithoutVersioning.
	 * @return
	 * @throws WebAssetException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@SuppressWarnings("unchecked")
	public static boolean publishAsset(Folder folder, User user, boolean respectFrontendRoles,
									   boolean isNewVersion) throws WebAssetException, DotSecurityException, DotDataException {
		ContentletAPI conAPI = APILocator.getContentletAPI();

		//http://jira.dotmarketing.net/browse/DOTCMS-6325
		if (user != null &&
				!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT,
						user)) {
			Logger.debug(PublishFactory.class, "publishAsset: user = " + user.getEmailAddress()
					+ ", don't have permissions to publish: " + folder);
			return false;
		}

		Logger.debug(PublishFactory.class, ()-> "*****I'm a Folder -- Publishing" + folder.getName());

//gets all subfolders for this folder
		List<Folder> foldersListSubChildren = APILocator.getFolderAPI()
				.findSubFolders(folder, APILocator.getUserAPI().getSystemUser(), false);
		//gets all links for this folder
		List<Link> linksListSubChildren = APILocator.getFolderAPI()
				.getWorkingLinks(folder, user, false);

		//Recursive call for publish items in the subfolders
		for(final Folder subFolder : foldersListSubChildren){
			publishAsset(subFolder,user,respectFrontendRoles,isNewVersion);
		}

		//Publish links
		for(final Link link : linksListSubChildren){
			if(permissionAPI.doesUserHavePermission((link), PERMISSION_PUBLISH, user, respectFrontendRoles)){
				publishAsset(link,user,respectFrontendRoles,isNewVersion);
			}
		}

		java.util.List<Contentlet> contentlets = conAPI.findContentletsByFolder(folder, user,
				false);
		java.util.Iterator<Contentlet> contentletsIter = contentlets.iterator();
		while (contentletsIter.hasNext()) {
			//publishes each one
			Contentlet contentlet = (Contentlet) contentletsIter.next();
			Logger.debug(PublishFactory.class,
					"*****I'm a Folder -- Publishing my Inode Child=" + contentlet.getInode());
			if (!contentlet.isLive() && !contentlet.isArchived()
					&& (permissionAPI.doesUserHavePermission(contentlet, PERMISSION_PUBLISH, user,
					respectFrontendRoles))) {
				APILocator.getContentletAPI().publish(contentlet, user, false);
			}
		}

		return true;
	}

	/**
	 * This method publish a given and asset and its related assets
	 * if a user is passed the method will check permissions to publish
	 * if the user doesn't have permission to publish one of the related assets
	 * then that one will be skipped
	 * @param webAsset
	 * @param user
	 * @param respectFrontendRoles
	 * @param isNewVersion  - if passed false then the webasset's mod user and mod date will NOT be altered. @see {@link ContentletAPI#checkinWithoutVersioning(Contentlet, java.util.Map, List, List, User, boolean)}checkinWithoutVersioning.
	 * @return
	 * @throws WebAssetException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@SuppressWarnings("unchecked")
	@WrapInTransaction  // TODO: we need to create a publishAsset method in API and move transactional annotation to there
	public static boolean publishAsset(Inode webAsset, User user, boolean respectFrontendRoles, boolean isNewVersion) throws WebAssetException, DotSecurityException, DotDataException 
	{
		ContentletAPI conAPI = APILocator.getContentletAPI();
		HostAPI hostAPI = APILocator.getHostAPI();

		if (webAsset instanceof WebAsset)
		{
			try {
				WebAssetFactory.publishAsset((WebAsset) webAsset, user, isNewVersion); // todo: reviewing here
			} catch (Exception e) {
				Logger.error(PublishFactory.class, "publishAsset: Failed to publish the asset.", e);
			}
		}

		if (webAsset instanceof Container) {

			//saves to live folder under velocity
			new ContainerLoader().invalidate((Container)webAsset);
		}


		if (webAsset instanceof Template) {

			Logger.debug(PublishFactory.class, "*****I'm a Template -- Publishing");

			//gets all identifier children
			java.util.List<Container> identifiers = APILocator.getTemplateAPI().getContainersInTemplate((Template)webAsset, APILocator.getUserAPI().getSystemUser(), false);
			java.util.Iterator<Container> identifiersIter = identifiers.iterator();
			while (identifiersIter.hasNext()) {

				Container container =(Container) identifiersIter.next();

				Logger.debug(PublishFactory.class, "*****I'm a Template -- Publishing my Container Child=" + container.getInode());
				if(!container.isLive()){
					publishAsset(container,user, respectFrontendRoles, isNewVersion);
				}


			}

            //Clean-up the cache for this template
            CacheLocator.getTemplateCache().remove( webAsset.getInode() );
            //writes the template to a live directory under velocity folder
			new TemplateLoader().invalidate((Template)webAsset);

		}

		if (webAsset instanceof IHTMLPage)
		{
            //Get the unpublish content related to this HTMLPage
            List relatedNotPublished = new ArrayList();
            relatedNotPublished = getUnpublishedRelatedAssets( webAsset, relatedNotPublished, user, respectFrontendRoles );
            //Publish the page
            publishHTMLPage( (IHTMLPage) webAsset, relatedNotPublished, user, respectFrontendRoles );
		}

		final ContentletLoader contentletLoader = new ContentletLoader();
		if (webAsset instanceof Link) {
            FactoryLocator.getMenuLinkFactory()
                    .getParentContentlets(webAsset.getInode()).stream().filter(contentlet -> {
                try {
                    return contentlet.isLive();
                } catch (DotDataException | DotSecurityException  e) {
                    throw new DotRuntimeException(e);
                }
					}).forEach( contentlet -> contentletLoader.invalidate(contentlet));

			// Removes static menues to provoke all possible dependencies be generated.
			Folder parentFolder = (Folder)APILocator.getFolderAPI().findParentFolder((Treeable) webAsset,user,false);
			Host host = (Host) hostAPI.findParentHost(parentFolder, APILocator.getUserAPI().getSystemUser(), respectFrontendRoles);
			RefreshMenus.deleteMenu(host);
			CacheLocator.getNavToolCache().removeNav(host.getIdentifier(), parentFolder.getInode());
		}

		return true;

	}

    /**
     * Publishes a given html page (if is HTMLPageAsset) and its related content (Applies to the legacy and new HTML pages).
     * It will also remove the page from the Page/Block cache<br/>
     * <strong>NOTE: </strong> Don't call this method directly for legacy HTMLPages, instead call publishAsset, that publishAsset method will
     * call this method after publish the legacy HTMLPage.
     *
     * @param htmlPage
     * @param relatedNotPublished
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws WebAssetException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public static boolean publishHTMLPage(IHTMLPage htmlPage, List relatedNotPublished, User user, boolean respectFrontendRoles ) throws WebAssetException, DotSecurityException, DotDataException {

        Logger.debug(PublishFactory.class, "*****I'm an HTML Page -- Publishing");

        ContentletAPI contentletAPI = APILocator.getContentletAPI();
		final Set<Contentlet> futureContentlets = new HashSet<>();
		final Set<Contentlet> expiredContentlets = new HashSet<>();

        final ContentletLoader contentletLoader = new ContentletLoader();
        //Publishing related pieces of content
        for ( Object asset : relatedNotPublished ) {
            if ( asset instanceof Contentlet ) {
                Logger.debug( PublishFactory.class, "*****I'm an HTML Page -- Publishing my Contentlet Child=" + ((Contentlet) asset).getInode() );
                try {
                    contentletAPI.publish( (Contentlet) asset, user, false );
                    contentletLoader.invalidate(asset);

                } catch ( DotSecurityException e ) {
                    //User has no permission to publish the content in the page so we just skip it
                    Logger.debug( PublishFactory.class, "publish html page: User has no permission to publish the content inode = " + ((Contentlet) asset).getInode() + " in the page, skipping it." );
                } catch (DotRuntimeException e) {
					final Throwable rootCause = ExceptionUtil.getRootCause(e);

					if (ExpiredContentletPublishStateException.class.equals(rootCause.getClass())) {
						expiredContentlets.add(((ExpiredContentletPublishStateException) rootCause).getContentlet());
					}
				}
            } else if ( asset instanceof Template ) {
                Logger.debug( PublishFactory.class, "*****I'm an HTML Page -- Publishing Template =" + ((Template) asset).getInode() );
                APILocator.getTemplateAPI().publishTemplate(Template.class.cast(asset),user,respectFrontendRoles);
            }
        }

        //writes the htmlpage to a live directory under velocity folder
        new PageLoader().invalidate(htmlPage);
        APILocator.getContentletAPI().publish( (HTMLPageAsset) htmlPage, user, false );


        //Remove from block cache.
        // CacheLocator.getBlockPageCache().remove(htmlPage);

        if (!futureContentlets.isEmpty()) {
			final String listContentlets = futureContentlets.stream()
					.map(contentlet -> String.format("<li>%s</li>", contentlet.getTitle()))
					.collect(Collectors.joining());

        	sendMessage(user,"publish.page.future.fields.error", String.format("<ul>%s</ul>", listContentlets));
		}

		if (!expiredContentlets.isEmpty()) {
			final String listContentlets = expiredContentlets.stream()
					.map(contentlet -> String.format("<li>%s</li>", contentlet.getTitle()))
					.collect(Collectors.joining());

			sendMessage(user,"publish.page.expired.fields.error", String.format("<ul>%s</ul>", listContentlets));
		}

        return true;
    }
	
	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(Inode webAsset, List relatedAssets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getUnpublishedRelatedAssets(webAsset, relatedAssets, true, false, user, respectFrontendRoles);
	}
	
	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(Inode webAsset, List relatedAssets, boolean checkPublishPermissions, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getUnpublishedRelatedAssets(webAsset, relatedAssets, true,checkPublishPermissions, user, respectFrontendRoles);
	}

	/**
	 * Retrieves a list of dependent object (dependent of object of the given webAsset param)
	 * that the given user has permissions to publish
	 * @param webAsset
	 * @param relatedAssets
	 * @param returnOnlyWebAssets
	 * @param checkPublishPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(Inode webAsset, List relatedAssets, boolean returnOnlyWebAssets, boolean checkPublishPermissions, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		ContentletAPI conAPI = APILocator.getContentletAPI();

		if (webAsset instanceof Template) {

			Logger.debug(PublishFactory.class, "*****I'm a Template -- PrePublishing");

			//gets all identifier children

			java.util.List<Container> identifiers = APILocator.getTemplateAPI().getContainersInTemplate((Template)webAsset, APILocator.getUserAPI().getSystemUser(), false);
			Iterator<Container> identifiersIter = identifiers.iterator();
			while (identifiersIter.hasNext()) {

				Container container = (Container)identifiersIter.next();
				if(!container.isLive() && (permissionAPI.doesUserHavePermission(container, PERMISSION_PUBLISH, user, respectFrontendRoles) || !checkPublishPermissions)) {
					relatedAssets.add(container);
				}
			}

		}
		if (webAsset instanceof IHTMLPage ) {
			//Search for the unpublished related content to this HTML page
			getUnpublishedRelatedAssetsForPage( (IHTMLPage) webAsset, relatedAssets, checkPublishPermissions, user, respectFrontendRoles );
		}

		return relatedAssets;
	}

	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(Folder folder, List relatedAssets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getUnpublishedRelatedAssets(folder, relatedAssets, true, false, user, respectFrontendRoles);
	}
	
	/**
	 * Retrieves a list of dependent object (dependent of object of the given folder param)
	 * that the given user has permissions to publish
	 * @param folder
	 * @param relatedAssets
	 * @param returnOnlyWebAssets
	 * @param checkPublishPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(final Folder folder, List relatedAssets,
			final boolean returnOnlyWebAssets, final boolean checkPublishPermissions,
			final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		ContentletAPI conAPI = APILocator.getContentletAPI();

		Logger.debug(PublishFactory.class, "*****I'm a Folder -- PrePublishing" + folder.getName());

		//gets all subfolders
		List<Folder> foldersListSubChildren = APILocator.getFolderAPI()
				.findSubFolders(folder, APILocator.getUserAPI().getSystemUser(), false);
		//gets all links for this folder
		List<Link> linksListSubChildren = APILocator.getFolderAPI()
				.getWorkingLinks(folder, user, false);

		//Recursive call for publish items in the subfolders
		for(final Folder subFolder : foldersListSubChildren){
			relatedAssets = getUnpublishedRelatedAssets(subFolder,relatedAssets,returnOnlyWebAssets,checkPublishPermissions,user,respectFrontendRoles);
		}

		//get unpublished links
		for(final Link link : linksListSubChildren){
			if((permissionAPI.doesUserHavePermission((link), PERMISSION_PUBLISH, user, respectFrontendRoles) || !checkPublishPermissions)) {
				relatedAssets.add(link);
			}
		}

		java.util.List<Contentlet> contentlets = conAPI.findContentletsByFolder(folder, user,
				false);
		java.util.Iterator<Contentlet> contentletsIter = contentlets.iterator();
		while (contentletsIter.hasNext()) {
			Contentlet contentlet = (Contentlet) contentletsIter.next();
			if (!contentlet.isLive() && (
					permissionAPI.doesUserHavePermission(contentlet, PERMISSION_PUBLISH, user,
							respectFrontendRoles) || !checkPublishPermissions)) {
				relatedAssets.add(contentlet);
			}
		}

		return relatedAssets;
	}

    /**
     * Returns a List of unpublished related content to a given HTML page
     *
     * @param htmlPage
     * @param relatedAssets
     * @param checkPublishPermissions
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @CloseDBIfOpened
    public static List getUnpublishedRelatedAssetsForPage ( IHTMLPage htmlPage, List relatedAssets, boolean checkPublishPermissions, User user, boolean respectFrontendRoles ) throws DotDataException, DotSecurityException {

        Logger.debug( PublishFactory.class, "*****I'm an HTML Page -- PrePublishing" );

        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        //gets working (not published) template parent for this html page
        Template templateParent = APILocator.getTemplateAPI().findWorkingTemplate( htmlPage.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false );
        if ( templateParent!=null && InodeUtils.isSet( templateParent.getInode() ) ) {

            if ( !templateParent.isLive() && (permissionAPI.doesUserHavePermission( templateParent, PERMISSION_PUBLISH, user, respectFrontendRoles ) || !checkPublishPermissions) ) {
                relatedAssets.add( templateParent );
            }

            //gets all live container children
            java.util.List<Container> identifiers = APILocator.getTemplateAPI().getContainersInTemplate( templateParent, APILocator.getUserAPI().getSystemUser(), false );
            java.util.Iterator<Container> identifiersIter = identifiers.iterator();
            while ( identifiersIter.hasNext() ) {

                final Container container = identifiersIter.next();

                final List categories = APILocator.getCategoryAPI()
                        .getParents(container, APILocator.getUserAPI().getSystemUser(), false);
                List contentlets;

                if ( categories.size() == 0 ) {
                    Logger.debug( PublishFactory.class, "*******HTML Page PrePublishing Static Container" );
                    Identifier idenHtmlPage = APILocator.getIdentifierAPI().find( htmlPage );
                    Identifier idenContainer = APILocator.getIdentifierAPI().find( container );
                    try {
                        contentlets = contentletAPI.findPageContentlets( idenHtmlPage.getInode(), idenContainer.getInode(), null, true, -1, APILocator.getUserAPI().getSystemUser(), false );
                    } catch ( Exception e ) {
                        Logger.error( PublishFactory.class, "Unable to get contentlets on page", e );
                        contentlets = new ArrayList<Contentlet>();
                    }
                } else {

                    Logger.debug( PublishFactory.class, "*******HTML Page PrePublishing Dynamic Container" );
                    Iterator catsIter = categories.iterator();
                    Set contentletSet = new HashSet();

                    String condition = "working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " and deleted=" + com.dotmarketing.db.DbConnectionFactory.getDBFalse();
                    String sort = (container.getSortContentletsBy() == null) ? "sort_order" : container.getSortContentletsBy();

                    while ( catsIter.hasNext() ) {
                        Category category = (Category) catsIter.next();
                        List contentletsChildren = InodeFactory.getChildrenClassByConditionAndOrderBy( category, Contentlet.class, condition, sort );
                        if ( contentletsChildren != null && contentletsChildren.size() > 0 ) {
                            contentletSet.addAll( contentletsChildren );
                        }
                    }
                    contentlets = new ArrayList();
                    contentlets.addAll( contentletSet );
                }
                java.util.Iterator contentletsIter = contentlets.iterator();
                while ( contentletsIter.hasNext() ) {
                    //publishes each one
                    Contentlet contentlet = (Contentlet) contentletsIter.next();
                    if ( !contentlet.isLive() && (permissionAPI.doesUserHavePermission( contentlet, PERMISSION_PUBLISH, user, respectFrontendRoles ) || !checkPublishPermissions) ) {
                        relatedAssets.add( contentlet );
                    }
                }
            }

        }

        return relatedAssets;
    }

	private static void sendMessage(final User user, final String messageKey, String... arguments) {
		try {

			final String  message = LanguageUtil.get(messageKey, (Object[]) arguments);

			final SystemMessageBuilder messageBuilder = new SystemMessageBuilder()
					.setMessage(message)
					.setSeverity(MessageSeverity.ERROR)
					.setType(MessageType.SIMPLE_MESSAGE)
					.setLife(TimeUnit.SECONDS.toMillis(5));

			systemMessageEventUtil.pushMessage(messageBuilder.create(), list(user.getUserId()));

			Logger.warn(PublishFactory.class, message);
		} catch (final  LanguageException  e) {
			Logger.warn(PublishFactory.class, () -> "messageKey:" + messageKey + ", msg:" + e.getMessage());
		}
	}
}
