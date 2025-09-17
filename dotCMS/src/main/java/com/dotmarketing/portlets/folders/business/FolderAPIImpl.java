package com.dotmarketing.portlets.folders.business;

import static com.dotcms.util.DotPreconditions.checkNotNull;
import static com.dotmarketing.business.APILocator.getPermissionAPI;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;
import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.FORWARD_SLASH;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.VisibilityRoles;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.api.tree.Parentable;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 */
public class FolderAPIImpl implements FolderAPI  {

	public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";
	private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();
	private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
	private final ContentletAPI contentletAPI = APILocator.getContentletAPI();

	@VisibleForTesting
	protected static final Set<String> reservedFolderNames =
			Collections.unmodifiableSet(
					new HashSet<>(CollectionsUtils
							.set(Config.getStringArrayProperty("RESERVEDFOLDERNAMES",
									new String[]{"WEB-INF", "META-INF", "assets", "dotcms", "html",
											"System folder",
											"portal",
											"email_backups",
											"DOTLESS", "DOTSASS", "dotAdmin", "custom_elements"})
							))
			);

	/**
	 * Will get a folder on a given path for a particular host.
	 *
	 * If the folder does not exists will return a folder with null values.
	 * If the user does not have permissions over the folder a DotSecurityException will be thrown.
	 *
	 * @param path path of the requested folder
	 * @param host host where the folder should live
	 * @return the requested folder if the user has permissions, if not an exception.
	 * @throws DotSecurityException
	 */

	private final FolderFactory folderFactory = FactoryLocator.getFolderFactory();
	private final PermissionAPI permissionAPI = getPermissionAPI();

	@CloseDBIfOpened
	public Folder findFolderByPath(final String path, final Host host,
			final User user, final boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException {
		final Folder folder = folderFactory.findFolderByPath(path, host);
		if (folder == null || UtilMethods.isEmpty(folder.getInode()) ||
				permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)){
			return folder;
		}

		if(FolderAPI.SYSTEM_FOLDER.equals(folder.getInode()) &&
				(permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions) || host.isSystemHost())){
					return findSystemFolder();
		}

		final String errorMsg = "User " + (user.getUserId() != null ? user.getUserId() : BLANK) + " does not have permission to read folder " + folder.getPath()+ " on host " + host.getHostname();
		Logger.error(FolderAPIImpl.class,errorMsg);
		throw new DotSecurityException(errorMsg);
	}


	public Folder findFolderByPath(String path, String hostid, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException {
		if(user==null) user = APILocator.getUserAPI().getSystemUser();
		Host host = APILocator.getHostAPI().find(hostid, user, false);
		return findFolderByPath(path,host,user, respectFrontEndPermissions);
	}

	@WrapInTransaction
	public boolean renameFolder(final Folder folder, final String newName,
								final User user, final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		boolean renamed;

		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId() : BLANK) + " does not have permission to edit folder " + folder.getPath());
		}

		try {
			validateFolderName(folder, newName);
			renamed = folderFactory.renameFolder(folder, newName, user, respectFrontEndPermissions);
			CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
			Identifier folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
			CacheLocator.getNavToolCache().removeNavByPath(folderId.getHostId(), folderId.getParentPath());
			return renamed;
		} catch (InvalidFolderNameException e) {
			Logger.error(FolderAPIImpl.class, "Error renaming folder '"
					+ folder.getPath() + "' with id: " + folder.getIdentifier() + " to name: "
					+ newName + ". Error: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			Logger.error(FolderAPIImpl.class, "Error renaming folder '"
					+ folder.getPath() + "' with id: " + folder.getIdentifier() + " to name: "
					+ newName + ". Error: " + e.getMessage());
			throw new DotDataException(e.getMessage(),e);
		}
	}

	@CloseDBIfOpened
	public Folder findParentFolder(final Treeable asset, final User user, final boolean respectFrontEndPermissions) throws DotIdentifierStateException,
			DotDataException, DotSecurityException {
		Identifier id = APILocator.getIdentifierAPI().find(asset.getIdentifier());
		if(id==null) return null;
		if(id.getParentPath()==null || id.getParentPath().equals("/") || id.getParentPath().equals(SYSTEM_FOLDER_PARENT_PATH))
			return null;
		Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontEndPermissions);
		Folder f = folderFactory.findFolderByPath(id.getParentPath(), host);

		if(f == null || !UtilMethods.isSet(f.getInode())) return null;
		if (!permissionAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
		    if(UtilMethods.isSet(f.getPath())){
		        //Folder exists in DB, but the user does not have permissions to read it.
		        Logger.error(this, "User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + f.getPath());
		    }else{
		        //Despite the Folder Object is not null, It may return an empty Folder Object because the Parent Folder is missing.
		        Logger.error(this, "User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + id.getParentPath() + " Please check the folder exists.");
		    }
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + f.getPath());
		}
		return f;

	}

	/**
	 *
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@CloseDBIfOpened
	public List<Folder> findSubFolders(final Folder folder, final User user,
									   final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		if (!this.permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {

			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + folder.getPath());
		}

		return this.folderFactory.getFoldersByParent(folder, user, respectFrontEndPermissions);
	} // findSubFolders.

	/**
	 *
	 * @return List of sub folders for passed in folder
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	public List<Folder> findSubFolders(final Host host, final User user,
									   final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Host " + host.getHostname());
		}

		final List<Folder> allFolders = folderFactory.findFoldersByHost(host);
		final List<Folder> subFolders = new ArrayList<>(allFolders.size());

		for(Folder folder : allFolders)
		    if(permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions))
		        subFolders.add(folder);
		return subFolders;
	}

	@CloseDBIfOpened
	public List<Folder> findThemes(final Host host, final User user,
								   final boolean respectFrontEndPermissions) throws DotDataException,
	DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Host " + host.getHostname());
		}

		List<Folder> full = folderFactory.findThemesByHost(host);
		List<Folder> ret = new ArrayList<>(full.size());
		for(Folder ff : full)
			if(permissionAPI.doesUserHavePermission(ff, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions))
				ret.add(ff);
				return ret;
	}

	/**
	 *
	 * @param folder
	 *            Recursively
	 * @return List of sub folders for passed in folder
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */

	public List<Folder> findSubFoldersRecursively(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + folder.getPath());
		}

		List<Folder> subFolders = findSubFolders(folder, user, respectFrontEndPermissions);
		List<Folder> toIterateOver = new ArrayList<>(subFolders);
		for (Folder f : toIterateOver) {
			if (permissionAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
				subFolders.addAll(findSubFoldersRecursively(f, user, respectFrontEndPermissions));
			}
		}
		return subFolders;
	}

	/**
	 *
	 * @return List of sub folders for passed in folder
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	public List<Folder> findSubFoldersRecursively(Host host, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Host " + host.getHostname());
		}
		List<Folder> subFolders = folderFactory.findFoldersByHost(host);
		List<Folder> toIterateOver = new ArrayList<>(subFolders);
		for (Folder f : toIterateOver) {
			if (permissionAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
				subFolders.addAll(findSubFoldersRecursively(f, user, respectFrontEndPermissions));
			}
		}
		return subFolders;
	}

	/**
	 * Will copy a folder to a new location with all it contains.
	 *
	 * @param folderToCopy
	 * @param newParentFolder
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws IOException
	 * @throws DotStateException
	 */
	@WrapInTransaction
	public void copy(final Folder folderToCopy, final Folder newParentFolder,
					 final User user, final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException, DotStateException, IOException {

		if (!permissionAPI.doesUserHavePermission(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + folderToCopy.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to add to Folder " + newParentFolder.getPath());
		}

		folderFactory.copy(folderToCopy, newParentFolder);

		this.systemEventsAPI.pushAsync(SystemEventType.COPY_FOLDER, new Payload(folderToCopy.getMap(), Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	@WrapInTransaction
	public void copy(final Folder folderToCopy, final Host newParentHost,
					 final User user, final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException, DotStateException, IOException {

		if (!permissionAPI.doesUserHavePermission(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + folderToCopy.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to add to Host " + newParentHost.getHostname());
		}

		validateFolderName(folderToCopy);
		folderFactory.copy(folderToCopy, newParentHost);

		this.systemEventsAPI.pushAsync(SystemEventType.COPY_FOLDER, new Payload(folderToCopy.getMap(), Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	@CloseDBIfOpened
	public boolean exists(String folderInode) throws DotDataException {
		return folderFactory.exists(folderInode);
	}


	@SuppressWarnings("unchecked")
	@CloseDBIfOpened
	public List<Inode> findMenuItems(Folder folder, User user, boolean respectFrontEndPermissions) throws DotStateException, DotDataException {
		return folderFactory.getMenuItems(folder);
	}

	/**
	 * Takes a folder and a user and deletes all underneith assets User needs
	 * edit permssions on folder to delete everything undernieth
	 *
	 * @param folder
	 * @param user
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@WrapInTransaction
	public void delete(final Folder folder, final User user, final boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {

		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, respectFrontEndPermissions);

		if(folder==null || !UtilMethods.isSet(folder.getInode()) ){
			Logger.debug(getClass(), "Cannot delete null folder");
			return;
		} else {
			AdminLogger.log(this.getClass(), "delete", "Deleting folder with name " + (UtilMethods.isSet(folder.getName()) ? folder.getName() + " ": "name not set "), user);
		}
		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {

			Logger.debug(getClass(), "The user: " + user.getEmailAddress() + " does not have permission for: " + folder );
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to edit Folder " + folder.getPath());
		}


		if(folder != null && FolderAPI.SYSTEM_FOLDER.equals(folder.getInode())) {

			Logger.debug(getClass(), "Cannot delete null folder: " + folder.getInode());
			throw new DotSecurityException("YOU CANNOT DELETE THE SYSTEM FOLDER");
		}


		// start transactional delete
		try {

			PermissionAPI papi = getPermissionAPI();
			if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {

				Logger.error(this.getClass(), "User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to Folder " + folder.getPath());
				throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) +  "does not have edit permissions on Folder " + folder.getPath());
			}

			Folder faker = new Folder();
			faker.setShowOnMenu(folder.isShowOnMenu());
			faker.setInode(folder.getInode());
			faker.setIdentifier(folder.getIdentifier());
			faker.setHostId(folder.getHostId());
			Identifier ident = APILocator.getIdentifierAPI().find(faker.getIdentifier());
			
			List<Folder> folderChildren = findSubFolders(folder, user, respectFrontEndPermissions);

			// recursivily delete
			for (Folder childFolder : folderChildren) {
				// sub deletes use system user - if a user has rights to parent
				// permission (checked above) they can delete to children
				if (Logger.isDebugEnabled(getClass())) {
					Logger.debug(getClass(), "Deleting the folder " + childFolder.getPath());
				}
				delete(childFolder, user, respectFrontEndPermissions);
			}

			// delete assets in this folder
			final String path = folder.getPath();
			if (Logger.isDebugEnabled(getClass())) {
				Logger.debug(getClass(), "Deleting the folder assets " + path);
			}
			_deleteChildrenAssetsFromFolder(folder, user, respectFrontEndPermissions);

			// get roles for the event.
			final Set<Role> roles =  papi.getRolesWithPermission
					(folder, PermissionAPI.PERMISSION_READ);
			if (Logger.isDebugEnabled(getClass())) {
				Logger.debug(getClass(), "Getting the folder roles for " + path +
						" roles: " + roles.stream().map(Role::getName).collect(Collectors.toList()));
				Logger.debug(getClass(), "Removing the folder permissions for " + path);
			}

			papi.removePermissions(folder);

			//http://jira.dotmarketing.net/browse/DOTCMS-6362
			if (Logger.isDebugEnabled(getClass())) {
				Logger.debug(getClass(), "Removing the folder references for " + path);
			}
			contentletAPI.removeFolderReferences(folder);

			contentTypeAPI.moveToSystemFolder(folder);

			// delete folder itself
			if (Logger.isDebugEnabled(getClass())) {
				Logger.debug(getClass(), "Removing the folder itself: " + path);
			}
			folderFactory.delete(folder);

			// The delete folder will avoid to send the message to the current user
			// in addition will check any match roles to propagate the event.
			if (Logger.isDebugEnabled(getClass())) {
				
				Logger.debug(getClass(), "Pushing async events: " + path);
			}

			this.systemEventsAPI.pushAsync(SystemEventType.DELETE_FOLDER, new Payload(folder.getMap(), Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(user.getUserId(),
							new VisibilityRoles(VisibilityRoles.Operator.OR, roles), Visibility.ROLES)));

			// delete the menus using the fake proxy inode
			if (folder.isShowOnMenu()) {
				CacheLocator.getNavToolCache().removeNav(faker.getHostId(), faker.getInode());
				
				CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());
				//remove value in the parent folder from the children listing
				Folder parentFolder = !ident.getParentPath().equals("/") ? APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), faker.getHostId(), user, false) : APILocator.getFolderAPI().findSystemFolder();
				if(parentFolder != null){
					CacheLocator.getNavToolCache().removeNav(faker.getHostId(), parentFolder.getInode());
				}
			}

			PublisherAPI.getInstance().deleteElementFromPublishQueueTable(folder.getInode());
		} catch (Exception e) {

			throw new DotDataException(e.getMessage(),e);
		}
	}

	/**
	 * Deletes all the contents inside the specified {@link Folder}.
	 *
	 * @param folder                     The folder whose contents will be deleted.
	 * @param user                       The user performing this action.
	 * @param respectFrontEndPermissions Set to {@code true} if this method requires that front-end roles are take into
	 *                                   account for this (which means this is being called from the front-end).
	 *                                   Otherwise, set to {@code false}.
	 *
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 * @throws DotStateException    An error occurred when performing this action
	 * @throws DotSecurityException The specified user does not have the required permissions to perform this action.
	 */
	private void _deleteChildrenAssetsFromFolder(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotStateException, DotSecurityException {

		try {

			ContentletAPI capi = APILocator.getContentletAPI();

			/************ conList *****************/
			List<Contentlet> conList = capi.findContentletsByFolder(folder, user, false);
			capi.destroy(conList, user, false);
			

			/************ Links *****************/
			List<Link> links = getLinks(folder, user, respectFrontEndPermissions);
			for (Link linker : links) {
				Link link = (Link) linker;

					Identifier identifier = APILocator.getIdentifierAPI().find(link);
					if (!InodeUtils.isSet(identifier.getId())) {
						Logger.warn(FolderFactory.class, "Link with Inode [" + link.getInode() + "] doesn't have a valid associated Identifier.");
						continue;
					}

					APILocator.getMenuLinkAPI().delete(link, user, false);
			}

			/******** delete possible orphaned identifiers under the folder *********/
			Identifier ident=APILocator.getIdentifierAPI().find(folder.getIdentifier());
			List<Identifier> orphanList=APILocator.getIdentifierAPI().findByParentPath(folder.getHostId(), ident.getURI());
			for(Identifier orphan : orphanList) {
			    APILocator.getIdentifierAPI().delete(orphan);
			}

			/************ Structures *****************/
			
		} catch (Exception e) {
			final String errorMsg = "An error occurred when deleting contents of folder '" + folder.getPath() + "' " +
					"with Identifier [" + folder.getIdentifier() + "]: " + e.getMessage();
			Logger.error(FolderAPI.class, errorMsg, e);
			throw new DotStateException(e.getMessage(),e);
		}
	}

	/**
	 * @param id
	 *            the inode or id of the folder
	 * @return Folder with a given id or inode
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	public Folder find(final String id, final User user,
					   final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		final Folder folder = this.folderFactory.find(id);

		if (folder!= null && !this.permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {

			throw new DotSecurityException("User " + ( user.getUserId() != null ? user.getUserId(): BLANK ) + " does not have permission to read Folder " + folder.getPath());
		}

		return folder;
	} // find.

	@Override
	public void validateFolderName(final Folder folder) throws DotDataException {
		validateFolderName(folder, folder.getName());
	}

	private void validateFolderName(final Folder folder, final String folderName) throws DotDataException {
		if (UtilMethods.isSet(folder.getParentPermissionable())
				&& folder.getParentPermissionable() instanceof Host
				&& UtilMethods.isSet(folderName)
				&& reservedFolderNames.stream()
				.anyMatch((name)->name.equalsIgnoreCase(folderName))) {
			throw new InvalidFolderNameException("Folder can't be saved. You entered a reserved folder name: " + folder.getName());
		}
	}


	@Override
	@WrapInTransaction
	public void save(final Folder folder, final String existingId,
					 final User user, final boolean respectFrontEndPermissions) throws DotDataException, DotStateException, DotSecurityException {

		final Identifier existingID = APILocator.getIdentifierAPI().find(folder.getIdentifier());

		// if we ingest bad folder ids, we should fix them
		if(!folder.getIdentifier().equalsIgnoreCase(folder.getInode()) && Config.getBooleanProperty("FIX_FOLDER_IDS_AUTOMATICALLY", false)) {
			final String FIX_FOLDER_IDS_JOB= "FIX_FOLDER_IDS_JOB";
			HibernateUtil.addCommitListener(FIX_FOLDER_IDS_JOB,()->{
				if(folderIdsNeedFixing()){
					fixFolderIds();
				}
			});
		}

		if(existingID ==null || !UtilMethods.isSet(existingID.getId())){
			throw new DotStateException("Folder must already have an identifier before saving");
		}

		if (SYSTEM_FOLDER.equals(folder.getInode()) && !Host.SYSTEM_HOST.equals(folder.getHostId())) {
			throw new DotRuntimeException(String.format("Host ID for SYSTEM_FOLDER must always be SYSTEM_HOST. Value " +
					"'%s' was set.", folder.getHostId()));
		}

		validateFolderName(folder);
		if (UtilMethods.isNotSet(folder.getTitle())) {
			folder.setTitle(existingID.getAssetName());
		}
		final Host folderSite = APILocator.getHostAPI().find(folder.getHostId(), user, respectFrontEndPermissions);
		final Folder parentFolder = findFolderByPath(existingID.getParentPath(), existingID.getHostId(), user, respectFrontEndPermissions);
		final Permissionable parent = existingID.getParentPath().equals(FORWARD_SLASH) ? folderSite : parentFolder;

		if (parent == null || UtilMethods.isNotSet(parent.getPermissionId())) {
			throw new DotStateException("No Parent Folder Found for id: " + existingID.getParentPath());
		}
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user,respectFrontEndPermissions)
				|| !permissionAPI.doesUserHavePermissions(PermissionableType.FOLDERS, PermissionAPI.PERMISSION_EDIT, user)) {
			final String userId = user.getUserId() != null ? user.getUserId() : BLANK;
			throw new AddContentToFolderPermissionException(userId, parentFolder.getPath());
		}

		final boolean isNew = folder.getInode() == null;
		//if the folder was renamed, we will need to create a new identifier
		if (!folder.getName().equalsIgnoreCase(existingID.getAssetName())){
			folderFactory.renameFolder(folder, folder.getName(), user, respectFrontEndPermissions);
		} else{
			folder.setModDate(new Date());
			folderFactory.save(folder);
		}

        // remove folder and parent from navigation cache
		if (!isNew) {
			CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
			CacheLocator.getNavToolCache()
					.removeNavByPath(existingID.getHostId(), existingID.getParentPath());
		}
        final SystemEventType systemEventType = isNew ? SystemEventType.SAVE_FOLDER : SystemEventType.UPDATE_FOLDER;
		systemEventsAPI.pushAsync(systemEventType, new Payload(folder.getMap(), Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	@WrapInTransaction
	public void save(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotStateException, DotSecurityException {

		save( folder, null,  user,  respectFrontEndPermissions);

	}

	
	
	@CloseDBIfOpened
	public Folder findSystemFolder()  {
		return Try.of(folderFactory::findSystemFolder).getOrElseThrow(DotRuntimeException::new);
	}


	@WrapInTransaction
	public Folder createFolders(String path, Host host, User user, boolean respectFrontEndPermissions) throws
			DotSecurityException, DotDataException {

		if(!UtilMethods.isSet(host)){
			throw new IllegalArgumentException("Host is not set");
		}
		StringTokenizer st = new StringTokenizer(path, "/"); // todo: shouldn't use multiplatform path separator
		var sb = new StringBuilder("/");

		Folder parent = null;

		
		final String defaultFileAssetType=Try.of(
                        ()->
                        APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).id())
		                .getOrElseThrow(e-> new DotRuntimeException("unable to find default fileAssetType"));
		
		
		
		while (st.hasMoreTokens()) {
			final String name = st.nextToken();
			sb.append(name).append("/");
			Folder folder = findFolderByPath(sb.toString(), host, user, respectFrontEndPermissions);
			if (folder == null || !InodeUtils.isSet(folder.getInode())) {
				folder= new Folder();
				folder.setName(name);
				folder.setTitle(name);
				folder.setShowOnMenu(false);
				folder.setSortOrder(0);
				folder.setFilesMasks("");
				folder.setHostId(host.getIdentifier());
				folder.setDefaultFileType((parent!=null && parent.getDefaultFileType() !=null)
				                ? parent.getDefaultFileType() 
				                : defaultFileAssetType);

				if (!UtilMethods.isSet(parent)) {
					APILocator.getIdentifierAPI().createNew(folder, host);
				} else {
					APILocator.getIdentifierAPI().createNew(folder, parent);
				}

				save(folder,  user,  respectFrontEndPermissions);
			}
			parent = folder;
		}
		return parent;
	}

	@CloseDBIfOpened
	public List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles) throws ValidationException,
			DotDataException {
		Map<String, String> dbColToObjectAttribute = new HashMap<>();

		if (UtilMethods.isSet(query.getSelectAttributes())) {

			if (!query.getSelectAttributes().contains("title")) {
				query.getSelectAttributes().add("title");
			}
		} else {
			List<String> atts = new ArrayList<>();
			atts.add("*");
			atts.add("title");
			query.setSelectAttributes(atts);
		}

		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, false, respectFrontendRoles);
	}

	/**
	 * @deprecated Not implemented because it does not take a user
	 */


	@CloseDBIfOpened
	public List<Folder> findFoldersByHost(Host host, User user, boolean respectFrontendRoles) {
		return folderFactory.findFoldersByHost(host);
	}

	@CloseDBIfOpened
	public  List<Link> getLinks(final Folder parent, final boolean working,
								final boolean deleted, final User user,
								final boolean respectFrontEndPermissions)
								throws DotStateException, DotDataException, DotSecurityException {

        if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getPath());
        }

        final ChildrenCondition cond = new ChildrenCondition();
        cond.working=working;
        cond.deleted=deleted;
        List list = folderFactory.getChildrenClass(parent, Link.class,cond);
        return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

	@CloseDBIfOpened
    public List<Link> getLinks ( Host host, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions ) throws DotStateException, DotDataException, DotSecurityException {

        if ( !permissionAPI.doesUserHavePermission( host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions ) ) {
            throw new DotSecurityException( "User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Host " + host.getHostname() );
        }

        ChildrenCondition cond = new ChildrenCondition();
        cond.working = working;
        cond.deleted = deleted;
        List list = folderFactory.getChildrenClass( host, Link.class, cond );
        return permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

	@CloseDBIfOpened
    public List<Link> getLinks ( Host host, User user, boolean respectFrontEndPermissions ) throws DotStateException, DotDataException, DotSecurityException {

        if ( !permissionAPI.doesUserHavePermission( host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions ) ) {
            throw new DotSecurityException( "User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Host " + host.getHostname() );
        }

        List list = folderFactory.getChildrenClass( host, Link.class );
        return permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

	@CloseDBIfOpened
    public List<Link> getLinks ( Folder parent, User user, boolean respectFrontEndPermissions ) throws DotStateException, DotDataException, DotSecurityException {

        if ( !permissionAPI.doesUserHavePermission( parent, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions ) ) {
            throw new DotSecurityException( "User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getPath() );
        }

        List list = folderFactory.getChildrenClass( parent, Link.class );
        return permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

	@CloseDBIfOpened
	public  List<Contentlet> getContent(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getPath());
		}

		List list = folderFactory.getChildrenClass(parent, Contentlet.class);
		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	@CloseDBIfOpened
	public  List<Structure> getStructures(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getPath());
		}
		List list = StructureFactory.getStructures("folder='"+parent.getInode()+"'", "mod_date", Integer.MAX_VALUE, 0, "asc");
		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	@CloseDBIfOpened
	public boolean isChildFolder(final Folder child, final Folder parent) throws DotDataException,DotSecurityException {
	   return folderFactory.isChildFolder(child, parent);
	}

	@CloseDBIfOpened
	public boolean matchFilter(Folder folder, String fileName) {
		return folderFactory.matchFilter(folder, fileName);
	}

	@CloseDBIfOpened
	public List findMenuItems(Folder folder, int orderDirection) throws DotDataException{
		return folderFactory.getMenuItems(folder, orderDirection);
	}

	@CloseDBIfOpened
	public List<Inode> findMenuItems(Host host,User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		return folderFactory.getMenuItems(host);
	}

	@CloseDBIfOpened
	public List<Folder> findSubFoldersTitleSort(Folder folder,User user,boolean respectFrontEndPermissions)throws DotDataException {
		return folderFactory.getSubFoldersTitleSort(folder);
	}

	@WrapInTransaction
	public boolean move(Folder folderToMove, Folder newParentFolder,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(folderToMove, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + folderToMove.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to add to Folder " + newParentFolder.getName());
		}
		boolean move = folderFactory.move(folderToMove, newParentFolder);

		this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FOLDER, new Payload(folderToMove.getMap(), Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

		return move;
	}

	@WrapInTransaction
	public boolean move(final Folder folderToMove,
						final Host newParentHost,
						final User user,
						final boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(folderToMove, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {

			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + folderToMove.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {

			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to add to Folder " + newParentHost.getHostname());
		}

		final boolean move = folderFactory.move(folderToMove, newParentHost);

		HibernateUtil.addCommitListener(Sneaky.sneaked(()->sendMoveFolderSystemEvent(folderToMove, user)),1000);

		return move;
	}

	@Override
	@WrapInTransaction
    public boolean move (final String folderId, final String newFolderId,
							  final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {


		//Searching for the folder to move
		final Folder folder = this.find( folderId, user, false );

		return
				!this.exists(newFolderId)?
						this.moveWhenDestinationDoesNotExists
							(newFolderId, folder, user, respectFrontendRoles):
						this.moveToExistingDestination
							(newFolderId, folder, user, respectFrontendRoles);

    }

	private boolean moveWhenDestinationDoesNotExists(final String newFolder,
											 final Folder currentFolder,
											 final User user,
											 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Host parentHost = APILocator.getHostAPI().find(newFolder, user, respectFrontendRoles);

		if (!permissionAPI.doesUserHavePermission(currentFolder, PERMISSION_WRITE, user)
				|| !permissionAPI.doesUserHavePermission(parentHost, PERMISSION_WRITE, user)) {

			throw new DotRuntimeException( "The user doesn't have the required permissions." );
		}

		if (!this.move(currentFolder, parentHost, user, respectFrontendRoles)) {
			//A folder with the same name already exists on the destination
			return false;
		}

		this.addRefreshIndexCommitListener(null, parentHost, currentFolder);
		return true;
	}

	private boolean moveToExistingDestination(final String newFolder,
											 final Folder currentFolder,
											 final User user,
											 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Folder parentFolder = this.find(newFolder, user, false);

		if (!permissionAPI.doesUserHavePermission( currentFolder, PERMISSION_WRITE, user )
				|| !permissionAPI.doesUserHavePermission( parentFolder, PERMISSION_WRITE, user )) {

			throw new DotRuntimeException( "The user doesn't have the required permissions.");
		}

		if (parentFolder.getInode().equalsIgnoreCase(currentFolder.getInode()) || //Trying to move a folder over itself
				this.isChildFolder(parentFolder, currentFolder)) {    //Trying to move a folder over one of its children

			return false;
		}

		if (!this.move(currentFolder, parentFolder, user, respectFrontendRoles)) { //A folder with the same name already exists on the destination

			return false;
		}

		this.addRefreshIndexCommitListener(parentFolder,null, currentFolder );
		APILocator.getPermissionAPI().resetPermissionReferences(currentFolder);
		return true;
	}

	private void addRefreshIndexCommitListener(final Folder parent,
											   final Host host,
											   final Folder folder ) throws DotDataException {
		HibernateUtil.addCommitListener(new FlushCacheRunnable() {
			@Override
			public void run() {
				try {
					if (folder!=null) {

						FolderAPIImpl.this.contentletAPI.refreshContentUnderFolderPath(folder.getHostId(), folder.getPath());
					}

					if ( parent != null ) {
						FolderAPIImpl.this.contentletAPI.refreshContentUnderFolderPath(parent.getHostId(), parent.getPath());
					} else {
						FolderAPIImpl.this.contentletAPI.refreshContentUnderHost(host);
					}
				} catch (Exception e) {
					Logger.error(this, e.getMessage(), e);
				}
			}
		});
	}

	private void sendMoveFolderSystemEvent (final Folder folderToMove, final User user)
			throws DotDataException, DotSecurityException {

		this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FOLDER, new Payload(folderToMove.getMap(), Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}


	@CloseDBIfOpened
	public List<Folder> findSubFolders(Host host, boolean showOnMenu) {
		return folderFactory.findSubFolders(host, showOnMenu);
	}

	@CloseDBIfOpened
	public List<Folder> findSubFolders(Folder folder,boolean showOnMenu)throws DotStateException, DotDataException {
		return folderFactory.findSubFolders(folder, showOnMenu);
	}

	@CloseDBIfOpened
	public List<String> getEntriesTree(Folder mainFolder, String openNodes,String view, String content, String structureInode,User user)
			throws DotStateException, DotDataException, DotSecurityException {
		Locale locale = user.getLocale();
		TimeZone timeZone = APILocator.systemTimeZone();
		Role[] roles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
		return folderFactory.getEntriesTree(mainFolder, openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
	}


	@CloseDBIfOpened
	public List<String> getFolderTree(String openNodes, String view,
			String content, String structureInode,User user)
			throws DotStateException, DotDataException, DotSecurityException {
		Locale locale = user.getLocale();
		TimeZone timeZone = APILocator.systemTimeZone();
		Role[] roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
		return folderFactory.getFolderTree(openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
	}

	@CloseDBIfOpened
	public List<Contentlet> getLiveContent(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
		cond.live=true;
		cond.deleted=false;
		List list = folderFactory.getChildrenClass(parent, Contentlet.class, cond);

		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	@CloseDBIfOpened
	public List<Link> getLiveLinks(final Folder parent, final User user,
								   final boolean respectFrontEndPermissions)
								   throws DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getPath());
		}

		final ChildrenCondition cond = new ChildrenCondition();
        cond.live=true;
        cond.deleted=false;
		final List list = folderFactory.getChildrenClass(parent, Link.class, cond);

		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	@CloseDBIfOpened
	public List<Contentlet> getWorkingContent(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getPath());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        cond.deleted=false;
		List list = folderFactory.getChildrenClass(parent, Contentlet.class, cond);

		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	@CloseDBIfOpened
	public List<Link> getWorkingLinks(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + (user.getUserId() != null?user.getUserId():BLANK) + " does not have permission to read Folder " + parent.getPath());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        //cond.deleted=false;
		List list = folderFactory.getChildrenClass(parent, Link.class, cond);

		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	/**
	 * Subscribe a listener to handle changes over the folder
	 * @param folder {@link Folder}
	 * @param folderListener folderListener
	 */
	public void subscribeFolderListener (final Folder folder, final FolderListener folderListener) {

		this.subscribeFolderListener(folder, folderListener, null);
	}

	/**
	 * Subscribe a listener to handle changes over the folder, this one filters the child events by name.
	 * @param @param folder {@link Folder}
	 * @param folderListener folderListener
	 * @param childNameFilter {@link Predicate} filter
	 */
	public void subscribeFolderListener (final Folder folder, final FolderListener folderListener, final Predicate<String> childNameFilter) {

		if (null != folder && null != folder.getName()) {

			Logger.info(this, () -> "Subscribing the folder listener: " + folderListener.getId() +
					", to the folder: " + folder);

			// handle archive and unarchive
			this.localSystemEventsAPI.subscribe(ContentletArchiveEvent.class, new EventSubscriber<ContentletArchiveEvent>() {

				@Override
				public String getId() {

					return folderListener.getId() + StringPool.FORWARD_SLASH + ContentletArchiveEvent.class.getName();
				}

				@Override
				public void notify(final ContentletArchiveEvent event) {

					FolderAPIImpl.this.triggerChildModifiedEvent(event, folder, folderListener, childNameFilter);
				}
			});

			// handle publish and unpublish
			this.localSystemEventsAPI.subscribe(ContentletPublishEvent.class, new EventSubscriber<ContentletPublishEvent>() {

				@Override
				public String getId() {

					return folderListener.getId() + StringPool.FORWARD_SLASH + ContentletPublishEvent.class.getName();
				}

				@Override
				public void notify(final ContentletPublishEvent event) {

					FolderAPIImpl.this.triggerChildModifiedEvent(event, folder, folderListener, childNameFilter);
				}
			});

			// handle delete
			this.localSystemEventsAPI.subscribe(ContentletDeletedEvent.class, new EventSubscriber<ContentletDeletedEvent>() {

				@Override
				public String getId() {

					return folderListener.getId() + StringPool.FORWARD_SLASH + ContentletDeletedEvent.class.getName();
				}

				@Override
				public void notify(final ContentletDeletedEvent event) {

					FolderAPIImpl.this.triggerChildDeleteEvent(event, folder, folderListener, childNameFilter);
				}
			});

			// handle checkin
			this.localSystemEventsAPI.subscribe(ContentletCheckinEvent.class, new EventSubscriber<ContentletCheckinEvent>() {

				@Override
				public String getId() {

					return folderListener.getId() + StringPool.FORWARD_SLASH + ContentletCheckinEvent.class.getName();
				}

				@Override
				public void notify(final ContentletCheckinEvent event) {

					FolderAPIImpl.this.triggerChildModifiedEvent(event, folder, folderListener, childNameFilter);
				}
			});
		}
	}

	private void triggerChildModifiedEvent(final ContentletArchiveEvent event,
										   final Folder parentFolder,
										   final FolderListener folderListener,
										   final Predicate<String> childNameFilter) {

		final Contentlet contentlet = event.getContentlet();
		this.triggerChildEvent(contentlet, event.getUser(), event.getDate(), parentFolder, childNameFilter,
				folderEvent-> folderListener.folderChildModified(folderEvent));
	}

	private void triggerChildModifiedEvent(final ContentletPublishEvent event,
										   final Folder parentFolder,
										   final FolderListener folderListener,
										   final Predicate<String> childNameFilter) {

		final Contentlet contentlet = event.getContentlet();
		this.triggerChildEvent(contentlet, event.getUser(), event.getDate(), parentFolder, childNameFilter,
				folderEvent-> folderListener.folderChildModified(folderEvent));
	}

	private void triggerChildModifiedEvent(final ContentletCheckinEvent event,
										   final Folder parentFolder,
										   final FolderListener folderListener,
										   final Predicate<String> childNameFilter) {

		final Contentlet contentlet = event.getContentlet();
		this.triggerChildEvent(contentlet, event.getUser(), event.getDate(), parentFolder, childNameFilter,
				folderEvent-> folderListener.folderChildModified(folderEvent));
	}

	private void triggerChildDeleteEvent(final ContentletDeletedEvent event,
										 final Folder parentFolder,
										 final FolderListener folderListener,
										 final Predicate<String> childNameFilter) {

		final Contentlet contentlet = event.getContentlet();
		this.triggerChildEvent(contentlet, event.getUser(), event.getDate(), parentFolder, childNameFilter,
				folderEvent-> folderListener.folderChildDeleted(folderEvent));
	} // triggerChildDeleteEvent

	private void triggerChildEvent (final Contentlet contentlet, final User user, final Date date,
									final Folder parentFolder, final Predicate<String> childNameFilter,
									final Consumer<FolderEvent> folderEventConsumer) {

		if (null != contentlet && (contentlet instanceof IFileAsset || contentlet.getContentType() instanceof FileAssetContentType)) {

			try {

				final String name = contentlet instanceof  IFileAsset?
						IFileAsset.class.cast(contentlet).getFileName(): (String)contentlet.getMap().get("fileName");

				if (null != childNameFilter && !childNameFilter.test(name)) {
					return;
				}

				final String fileAssetFolderParentId = contentlet instanceof  IFileAsset?
						IFileAsset.class.cast(contentlet).getParent(): (String)contentlet.getMap().get(Contentlet.FOLDER_KEY);

				final Folder childFolder = this.find(fileAssetFolderParentId, APILocator.systemUser(), false);
				if (null != childFolder && this.isChildFolder(childFolder, parentFolder)) {

					folderEventConsumer.accept(
							new FolderEvent(UUIDUtil.uuid(), user, contentlet, name, childFolder, date));
				}
			} catch (DotSecurityException | DotDataException e) {
				Logger.error(this, e.getMessage(), e);
			}
		}
	}

	/**
	 *
	 * @param parent (host or folder)
	 * @return List of sub folders for passed in folder regardless the show on menu boolean
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Override
	@CloseDBIfOpened
	public List<Folder> findSubFoldersByParent(final Parentable parent, final User user,
			final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		return (parent instanceof Folder )
				? findSubFolders((Folder)parent, user, respectFrontEndPermissions)
				: findSubFolders((Host)parent, user, respectFrontEndPermissions);
	} // findSubFolders.

	@Override
	@CloseDBIfOpened
	public void updateUserReferences(final String userId, final String replacementUserId)
			throws DotDataException {
		Logger.debug(this, () -> "Updating references for user " + userId);
		folderFactory.updateUserReferences(userId, replacementUserId);
	}

	@CloseDBIfOpened
	@Override
	public List<Map<String, Object>> getContentReport(final Folder folder, final String orderBy,
													  final String orderDirection, final int limit,
													  final int offset, final User user) throws DotDataException {
		checkNotNull(folder, "'folder' parameter cannot be null");
		final List<Map<String, Object>> contentReport =
				this.folderFactory.getContentReport(folder.getPath(), folder.getHostId(),
						SQLUtil.sanitizeParameter(orderBy), SQLUtil.sanitizeCondition(orderDirection),
						limit, offset);
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
		return contentReport.stream().map(entry -> {

			final String contentTypeVarName =
					entry.get(IdentifierFactory.ASSET_SUBTYPE).toString();
			final String contentTypeName =
					Try.of(() -> contentTypeAPI.find(contentTypeVarName).name()).getOrElse(contentTypeVarName);
			final Map<String, Object> map = new HashMap<>();
			map.put("contentTypeName", contentTypeName);
			map.put("entries", entry.get("total"));
			return map;

		}).collect(Collectors.toList());
	}

	@CloseDBIfOpened
	@Override
	public int getContentTypeCount(final Folder folder, final User user, final boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		checkNotNull(folder, "'folder' parameter cannot be null");
		checkNotNull(user, "'user' parameter cannot be null");
		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			final String errorMsg = String.format("User '%s' does not have permission to read Folder '%s'", user.getUserId(), folder.getPath());
			throw new DotSecurityException(errorMsg);
		}
		return this.folderFactory.getContentTypeCount(folder.getPath(), folder.getHostId());
	}


	String BROKEN_FOLDERS_SHOULD_BE_EMPTY = "select * from folder where inode <> identifier limit 1";
	String SYSTEM_FOLDER_SHOULD_NOT_BE_EMPTY =
			"select * from folder where inode ='SYSTEM_FOLDER' and identifier='SYSTEM_FOLDER'";


	@CloseDBIfOpened
	@Override
	public boolean folderIdsNeedFixing(){

			try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
				DotConnect db = new DotConnect();
				return !db.setSQL(BROKEN_FOLDERS_SHOULD_BE_EMPTY).loadObjectResults(conn).isEmpty() || db.setSQL(SYSTEM_FOLDER_SHOULD_NOT_BE_EMPTY)
						.loadObjectResults(conn).isEmpty();
			} catch (Exception e) {
				Logger.error(this, e);
				throw new DotRuntimeException(e);
			}


	}
	String ALLOW_DEFER_CONSTRAINT_SQL = "ALTER TABLE folder ALTER CONSTRAINT folder_identifier_fk DEFERRABLE;";
	String DENY_DEFER_CONSTRAINT_SQL = "ALTER TABLE folder ALTER CONSTRAINT folder_identifier_fk NOT DEFERRABLE;";
	String DEFER_CONSTRAINT_SQL = "SET CONSTRAINTS folder_identifier_fk DEFERRED;";
	String UPDATE_SYSTEM_FOLDER_IDENTIFIER = "update identifier set id ='SYSTEM_FOLDER' where parent_path = '/System folder' or id='"+ FolderAPI.OLD_SYSTEM_FOLDER_ID + "';";
	String UPDATE_SYSTEM_FOLDER_FOLDER = "update folder set identifier ='SYSTEM_FOLDER' where inode = 'SYSTEM_FOLDER' or inode='"+ FolderAPI.OLD_SYSTEM_FOLDER_ID + "';";
	String UPDATE_FOLDER_IDENTIFIERS="update identifier "
			+ "set id =subquery.inode "
			+ "from (select inode, identifier from folder where identifier <> inode) as subquery "
			+ "where  "
			+ "subquery.identifier =identifier.id;";

	String UPDATE_ALL_FOLDERS = "update folder set identifier = inode where identifier <> inode;";
	
	// Template theme fix queries
	String FIX_TEMPLATE_THEMES_QUERY = 
		"UPDATE template SET theme = f.inode " +
		"FROM folder f " +
		"WHERE template.theme = f.identifier " +
		"AND template.theme NOT IN (SELECT identifier FROM folder WHERE identifier = inode) " +
		"AND template.theme != 'SYSTEM_THEME';";


	@CloseDBIfOpened
	@Override
	public void fixFolderIds() {
		Logger.info(this,
				"Found non-matching folder inodes/identifiers, Fixing folder IDs task.");

		try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
			// allow deferred constraints
			conn.createStatement().execute(ALLOW_DEFER_CONSTRAINT_SQL);

			conn.setAutoCommit(false);

			// defer folder/identifier constraint
			conn.createStatement().execute(DEFER_CONSTRAINT_SQL);

			// Fix template themes before updating folder IDs to prevent broken references
			Logger.info(this, "Fixing template themes that reference folder identifiers");
			conn.createStatement().execute(FIX_TEMPLATE_THEMES_QUERY);

			// update system folder identifier
			conn.createStatement().execute(UPDATE_SYSTEM_FOLDER_IDENTIFIER);

			// update system folder folder
			conn.createStatement().execute(UPDATE_SYSTEM_FOLDER_FOLDER);

			// update folder ids with the inodes
			conn.createStatement().execute(UPDATE_FOLDER_IDENTIFIERS);

			// set all folder inodes=identifer
			conn.createStatement().execute(UPDATE_ALL_FOLDERS);

			conn.commit();

			conn.setAutoCommit(true);

			// just in case
			CacheLocator.getFolderCache().clearCache();
			CacheLocator.getPermissionCache().clearCache();
			CacheLocator.getTemplateCache().clearCache();
			CacheLocator.getVeloctyResourceCache().clearCache();

		} catch (Exception e) {
			Logger.error(this, e);
			throw new DotRuntimeException(e);
		} finally {

			// reset the constraint to not deferred
			try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
				conn.createStatement().execute(DENY_DEFER_CONSTRAINT_SQL);
			} catch (Exception e) {
				Logger.error(this, e);
				throw new DotRuntimeException(e);
			}
		}

	}


}
