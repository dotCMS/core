package com.dotmarketing.portlets.folders.business;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.dotcms.api.system.event.*;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.cmis.QueryResult;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import static com.dotmarketing.business.APILocator.getPermissionAPI;

public class FolderAPIImpl implements FolderAPI  {

	public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";
	private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();

	/**
	 * Will get a folder for you on a given path for a particular host
	 *
	 * @param path
	 * @param hostId
	 * @return
	 * @throws DotHibernateException
	 */
	private final FolderFactory folderFactory = FactoryLocator.getFolderFactory();
	private final PermissionAPI permissionAPI = getPermissionAPI();

    @CloseDBIfOpened
	public Folder findFolderByPath(final String path, final Host host,
								   final User user, final boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException {

		final Folder folder = folderFactory.findFolderByPath(path, host);

		if (folder != null && InodeUtils.isSet(folder.getInode()) &&
				!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {

			// SYSTEM_FOLDER means if the user has permissions to the host, then they can see host.com/
			if(FolderAPI.SYSTEM_FOLDER.equals(folder.getInode())) {
				if(!Host.SYSTEM_HOST.equals(host.getIdentifier())){
					if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
						throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read folder " + folder.getPath());
					}
				}
			}

		}

		return folder;
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

		boolean renamed = false;

		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to edit folder" + folder.getPath());
		}

		try {

			renamed = folderFactory.renameFolder(folder, newName, user, respectFrontEndPermissions);

			return renamed;
		} catch (Exception e) {

			throw new DotDataException(e.getMessage(),e);
		}
	}

	@CloseDBIfOpened
	public Folder findParentFolder(final Treeable asset, final User user, final boolean respectFrontEndPermissions) throws DotIdentifierStateException,
			DotDataException, DotSecurityException {
		Identifier id = APILocator.getIdentifierAPI().find(asset.getIdentifier());
		if(id==null) return null;
		if(id.getParentPath()==null || id.getParentPath().equals("/") || id.getParentPath().equals("/System folder"))
			return null;
		Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontEndPermissions);
		Folder f = folderFactory.findFolderByPath(id.getParentPath(), host);

		if(f == null || !UtilMethods.isSet(f.getInode())) return null;
		if (!permissionAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
		    if(UtilMethods.isSet(f.getPath())){
		        //Folder exists in DB, but the user does not have permissions to read it.
		        Logger.error(this, "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + f.getPath());
		    }else{
		        //Despite the Folder Object is not null, It may return an empty Folder Object because the Parent Folder is missing.
		        Logger.error(this, "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + id.getParentPath() + " Please check the folder exists.");
		    }
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + f.getPath());
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

			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folder.getPath());
		}

		return this.folderFactory.getFoldersByParent(folder, user, respectFrontEndPermissions);
	} // findSubFolders.

	/**
	 *
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	public List<Folder> findSubFolders(final Host host, final User user,
									   final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname());
		}

		final List<Folder> allFolders = folderFactory.findFoldersByHost(host);
		final List<Folder> subFolders = new ArrayList<Folder>(allFolders.size());

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
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname());
		}

		List<Folder> full = folderFactory.findThemesByHost(host);
		List<Folder> ret = new ArrayList<Folder>(full.size());
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
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folder.getPath());
		}

		List<Folder> subFolders = findSubFolders(folder, user, respectFrontEndPermissions);
		List<Folder> toIterateOver = new ArrayList<Folder>(subFolders);
		for (Folder f : toIterateOver) {
			if (permissionAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
				subFolders.addAll(findSubFoldersRecursively(f, user, respectFrontEndPermissions));
			}
		}
		return subFolders;
	}

	/**
	 *
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	public List<Folder> findSubFoldersRecursively(Host host, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname());
		}
		List<Folder> subFolders = folderFactory.findFoldersByHost(host);
		List<Folder> toIterateOver = new ArrayList<Folder>(subFolders);
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
	@CloseDBIfOpened
	public void copy(final Folder folderToCopy, final Folder newParentFolder,
					 final User user, final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException, DotStateException, IOException {

		if (!permissionAPI.doesUserHavePermission(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToCopy.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Folder " + newParentFolder.getPath());
		}

		folderFactory.copy(folderToCopy, newParentFolder);

		this.systemEventsAPI.pushAsync(SystemEventType.COPY_FOLDER, new Payload(folderToCopy, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	@CloseDBIfOpened
	public void copy(final Folder folderToCopy, final Host newParentHost,
					 final User user, final boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException, DotStateException, IOException {

		if (!permissionAPI.doesUserHavePermission(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToCopy.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Host " + newParentHost.getHostname());
		}

		folderFactory.copy(folderToCopy, newParentHost);

		this.systemEventsAPI.pushAsync(SystemEventType.COPY_FOLDER, new Payload(folderToCopy, Visibility.EXCLUDE_OWNER,
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
	public void delete(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {

		String path = StringUtils.EMPTY;

		if(folder==null || !UtilMethods.isSet(folder.getInode()) ){
			Logger.debug(getClass(), "Cannot delete null folder");
			return;
		} else {
			AdminLogger.log(this.getClass(), "delete", "Deleting folder with name " + (UtilMethods.isSet(folder.getName()) ? folder.getName() + " ": "name not set "), user);
		}
		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {

			Logger.debug(getClass(), "The user: " + user.getEmailAddress() + " does not have permission for: " + folder );
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to edit Folder " + folder.getPath());
		}


		if(folder != null && FolderAPI.SYSTEM_FOLDER.equals(folder.getInode())) {

			Logger.debug(getClass(), "Cannot delete null folder: " + folder.getInode());
			throw new DotSecurityException("YOU CANNOT DELETE THE SYSTEM FOLDER");
		}


		// start transactional delete
		try {

			PermissionAPI papi = getPermissionAPI();
			if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {

				Logger.error(this.getClass(), "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to Folder " + folder.getPath());
				throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" +  "does not have edit permissions on Folder " + folder.getPath());
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
			path = folder.getPath();
			if (Logger.isDebugEnabled(getClass())) {
				Logger.debug(getClass(), "Deleting the folder assets " + path);
			}
			_deleteChildrenAssetsFromFolder(folder, user, respectFrontEndPermissions);

			// get roles for the event.
			final Set<Role> roles =  papi.getRolesWithPermission
					(folder, PermissionAPI.PERMISSION_READ);
			if (Logger.isDebugEnabled(getClass())) {
				Logger.debug(getClass(), "Getting the folder roles for " + path +
						" roles: " + roles.stream().map(role -> role.getName()).collect(Collectors.toList()));
				Logger.debug(getClass(), "Removing the folder permissions for " + path);
			}

			papi.removePermissions(folder);

			//http://jira.dotmarketing.net/browse/DOTCMS-6362
			if (Logger.isDebugEnabled(getClass())) {
				Logger.debug(getClass(), "Removing the folder references for " + path);
			}
			APILocator.getContentletAPIImpl().removeFolderReferences(folder);

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

			this.systemEventsAPI.pushAsync(SystemEventType.DELETE_FOLDER, new Payload(folder, Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(user.getUserId(),
							new VisibilityRoles(VisibilityRoles.Operator.OR, roles), Visibility.ROLES)));

			// delete the menus using the fake proxy inode
			if (folder.isShowOnMenu()) {
				// RefreshMenus.deleteMenus();
				RefreshMenus.deleteMenu(faker);
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

	private void _deleteChildrenAssetsFromFolder(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotStateException, DotSecurityException {

		try {

			ContentletAPI capi = APILocator.getContentletAPI();

			/************ conList *****************/
			HibernateUtil.getSession().clear();
			List<Contentlet> conList = capi.findContentletsByFolder(folder, user, false);
			for (Contentlet c : conList) {
				// Find all multi-language contentlets and archive them
				Identifier ident = APILocator.getIdentifierAPI().find(c.getIdentifier());
	            List<Contentlet> otherLanguageCons = capi.findAllVersions(ident, user, false);
	            for (Contentlet cv : otherLanguageCons) {
					if(cv.isLive()){
						capi.unpublish(cv, user, false);
					}
					if(!cv.isArchived()){
						capi.archive(cv, user, false);
					}
	            }
				capi.delete(c, user, false);
			}

			/************ Links *****************/
			HibernateUtil.getSession().clear();
			List<Link> links = getLinks(folder, user, respectFrontEndPermissions);
			for (Link linker : links) {
				Link link = (Link) linker;

					Identifier identifier = APILocator.getIdentifierAPI().find(link);
					if (!InodeUtils.isSet(identifier.getInode())) {
						Logger.warn(FolderFactory.class, "_deleteChildrenAssetsFromFolder: link inode = " + link.getInode()
								+ " doesn't have a valid identifier associated.");
						continue;
					}

					permissionAPI.removePermissions(link);
					APILocator.getMenuLinkAPI().delete(link, user, false);


			}

			/******** delete possible orphaned identifiers under the folder *********/
			HibernateUtil.getSession().clear();
			Identifier ident=APILocator.getIdentifierAPI().find(folder);
			List<Identifier> orphanList=APILocator.getIdentifierAPI().findByParentPath(folder.getHostId(), ident.getURI());
			for(Identifier orphan : orphanList) {
			    APILocator.getIdentifierAPI().delete(orphan);
			    HibernateUtil.getSession().clear();
			    try {
    			    DotConnect dc = new DotConnect();
    			    dc.setSQL("delete from identifier where id=?");
    			    dc.addParam(orphan.getId());
    			    dc.loadResult();
			    } catch(Exception ex) {
			        Logger.warn(this, "can't delete the orphan identifier",ex);
			    }
			    HibernateUtil.getSession().clear();
			}

			/************ Structures *****************/
			
		} catch (Exception e) {
			Logger.error(FolderAPI.class, e.getMessage(), e);
			throw new DotStateException(e.getMessage());

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

		if (!this.permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user)) {

			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folder.getPath());
		}

		return folder;
	} // find.


	/**
	 * Saves a folder
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 */
	@WrapInTransaction
	public void save(final Folder folder, final String existingId,
					 final User user, final boolean respectFrontEndPermissions) throws DotDataException, DotStateException, DotSecurityException {

		Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
		if(id ==null || !UtilMethods.isSet(id.getId())){
			throw new DotStateException("Folder must already have an identifier before saving");
		}

		Host host = APILocator.getHostAPI().find(folder.getHostId(), user, respectFrontEndPermissions);
		Folder parentFolder = findFolderByPath(id.getParentPath(), id.getHostId(), user, respectFrontEndPermissions);
		Permissionable parent = id.getParentPath().equals("/") || id.getParentPath().equals("/System folder") 
		        ? host : parentFolder;

		if(parent ==null){
			throw new DotStateException("No Folder Found for id: " + id.getParentPath());
		}
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user,respectFrontEndPermissions)
				|| !permissionAPI.doesUserHavePermissions(PermissionableType.FOLDERS, PermissionAPI.PERMISSION_EDIT, user)) {
			String userId = user.getUserId() != null ? user.getUserId() : "";
			String parentFolderAsString = id.getParentPath().equals("/") || id.getParentPath().equals("/System folder") 
			        ? host.getName(): parentFolder.getPath();
			throw new AddContentToFolderPermissionException(userId, parentFolderAsString);
		}

		boolean isNew = folder.getInode() == null;
		folder.setModDate(new Date());
		folder.setName(folder.getName().toLowerCase());
		folderFactory.save(folder, existingId);

		SystemEventType systemEventType = isNew ? SystemEventType.SAVE_FOLDER : SystemEventType.UPDATE_FOLDER;
		systemEventsAPI.pushAsync(systemEventType, new Payload(folder, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	public void save(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotStateException, DotSecurityException {

		save( folder, null,  user,  respectFrontEndPermissions);

	}

	@CloseDBIfOpened
	public Folder findSystemFolder() throws DotDataException {
		return folderFactory.findSystemFolder();
	}


	@WrapInTransaction
	public Folder createFolders(String path, Host host, User user, boolean respectFrontEndPermissions) throws DotHibernateException,
			DotSecurityException, DotDataException {

		StringTokenizer st = new StringTokenizer(path, "/");
		StringBuffer sb = new StringBuffer("/");

		Folder parent = null;

		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			sb.append(name + "/");
			Folder f = findFolderByPath(sb.toString(), host, user, respectFrontEndPermissions);
			if (f == null || !InodeUtils.isSet(f.getInode())) {
				f= new Folder();
				f.setName(name);
				f.setTitle(name);
				f.setShowOnMenu(false);
				f.setSortOrder(0);
				f.setFilesMasks("");
				f.setHostId(host.getIdentifier());
				f.setDefaultFileType(CacheLocator.getContentTypeCache().getStructureByVelocityVarName(APILocator.getFileAssetAPI().DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode());
				Identifier newIdentifier = new Identifier();
				if(!UtilMethods.isSet(parent)){
					newIdentifier = APILocator.getIdentifierAPI().createNew(f, host);
				}else{
					newIdentifier = APILocator.getIdentifierAPI().createNew(f, parent);
				}

				f.setIdentifier(newIdentifier.getId());
				save(f,  user,  respectFrontEndPermissions);
			}
			parent = f;
		}
		return parent;
	}

	@CloseDBIfOpened
	public List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles) throws ValidationException,
			DotDataException {
		Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();

		if (UtilMethods.isSet(query.getSelectAttributes())) {

			if (!query.getSelectAttributes().contains("title")) {
				query.getSelectAttributes().add("title" + " as " + QueryResult.CMIS_TITLE);
			}
		} else {
			List<String> atts = new ArrayList<String>();
			atts.add("*");
			atts.add("title" + " as " + QueryResult.CMIS_TITLE);
			query.setSelectAttributes(atts);
		}

		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, false, respectFrontendRoles);
	}

	/**
	 * @deprecated Not implemented because it does not take a user
	 */


	@CloseDBIfOpened
	public List<Folder> findFoldersByHost(Host host, User user, boolean respectFrontendRoles) throws DotHibernateException {
		return folderFactory.findFoldersByHost(host);
	}

	@CloseDBIfOpened
	public  List<Link> getLinks(final Folder parent, final boolean working,
								final boolean deleted, final User user,
								final boolean respectFrontEndPermissions)
								throws DotStateException, DotDataException, DotSecurityException {

        if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
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
            throw new DotSecurityException( "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname() );
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
            throw new DotSecurityException( "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname() );
        }

        List list = folderFactory.getChildrenClass( host, Link.class );
        return permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

	@CloseDBIfOpened
    public List<Link> getLinks ( Folder parent, User user, boolean respectFrontEndPermissions ) throws DotStateException, DotDataException, DotSecurityException {

        if ( !permissionAPI.doesUserHavePermission( parent, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions ) ) {
            throw new DotSecurityException( "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath() );
        }

        List list = folderFactory.getChildrenClass( parent, Link.class );
        return permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

	@CloseDBIfOpened
	public  List<Contentlet> getContent(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
		}

		List list = folderFactory.getChildrenClass(parent, Contentlet.class);
		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	@CloseDBIfOpened
	public  List<Structure> getStructures(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
		}
		List list = StructureFactory.getStructures("folder='"+parent.getInode()+"'", "mod_date", Integer.MAX_VALUE, 0, "asc");
		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	@CloseDBIfOpened
	public boolean isChildFolder(Folder folder1, Folder folder2) throws DotDataException,DotSecurityException {
	   return folderFactory.isChildFolder(folder1, folder2);
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
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToMove.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Folder " + newParentFolder.getName());
		}
		boolean move = folderFactory.move(folderToMove, newParentFolder);

		this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FOLDER, new Payload(folderToMove, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

		return move;
	}

	@WrapInTransaction
	public boolean move(Folder folderToMove, Host newParentHost,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(folderToMove, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToMove.getPath());
		}

		if (!permissionAPI.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Folder " + newParentHost.getHostname());
		}
		boolean move = folderFactory.move(folderToMove, newParentHost);

		this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FOLDER, new Payload(folderToMove, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

		return move;
	}

	@CloseDBIfOpened
	public List<Folder> findSubFolders(Host host, boolean showOnMenu) throws DotHibernateException{
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
		TimeZone timeZone = user.getTimeZone();
		Role[] roles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
		return folderFactory.getEntriesTree(mainFolder, openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
	}


	@CloseDBIfOpened
	public List<String> getFolderTree(String openNodes, String view,
			String content, String structureInode,User user)
			throws DotStateException, DotDataException, DotSecurityException {
		Locale locale = user.getLocale();
		TimeZone timeZone = user.getTimeZone();
		Role[] roles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
		return folderFactory.getFolderTree(openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
	}

	@CloseDBIfOpened
	public List<Contentlet> getLiveContent(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getName());
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
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
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
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
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
			throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        //cond.deleted=false;
		List list = folderFactory.getChildrenClass(parent, Link.class, cond);

		return permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}
}
