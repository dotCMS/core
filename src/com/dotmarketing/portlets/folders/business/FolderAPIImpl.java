package com.dotmarketing.portlets.folders.business;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import com.dotcms.enterprise.cmis.QueryResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
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
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class FolderAPIImpl implements FolderAPI  {

	public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";

	/**
	 * Will get a folder for you on a given path for a particular host
	 *
	 * @param path
	 * @param hostId
	 * @return
	 * @throws DotHibernateException
	 */
	private FolderFactory ffac = FactoryLocator.getFolderFactory();
	private PermissionAPI papi = APILocator.getPermissionAPI();


	public Folder findFolderByPath(String path, Host host, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException {

		Folder f = ffac.findFolderByPath(path, host);


		if (InodeUtils.isSet(f.getInode()) && !papi.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {

			// SYSTEM_FOLDER means if the user has permissions to the host, then they can see host.com/
			if(FolderAPI.SYSTEM_FOLDER.equals(f.getInode())) {
				if(!Host.SYSTEM_HOST.equals(host.getIdentifier())){
					if(!papi.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
						throw new DotSecurityException("User " + user + " does not have permission to read " + f.getInode());
					}
				}
			}

		}
		return f;


	}


	public Folder findFolderByPath(String path, String hostid, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException {
		if(user==null) user = APILocator.getUserAPI().getSystemUser();
		Host host = APILocator.getHostAPI().find(hostid, user, false);
		return findFolderByPath(path,host,user, respectFrontEndPermissions);
	}

	public boolean renameFolder(Folder folder, String newName, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		boolean renamed = false;

		if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to edit " + folder.getName());
		}

		boolean localTransaction = false;
		try {
			localTransaction = HibernateUtil.getSession().connection().getAutoCommit();

			if (localTransaction) {
				HibernateUtil.startTransaction();
			}

			return ffac.renameFolder(folder, newName, user, respectFrontEndPermissions);
			
		} catch (Exception e) {

			if (localTransaction) {
				HibernateUtil.rollbackTransaction();
			}
			throw new DotDataException(e.getMessage());
		} finally {
			if (localTransaction) {
				HibernateUtil.commitTransaction();
			}
		}
	}


	public Folder findParentFolder(Treeable asset, User user, boolean respectFrontEndPermissions) throws DotIdentifierStateException,
			DotDataException, DotSecurityException {
		Identifier id = APILocator.getIdentifierAPI().find(asset.getIdentifier());
		if(id==null) return null;
		if(id.getParentPath()==null || id.getParentPath().equals("/") || id.getParentPath().equals("/System folder"))
			return null;
		Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontEndPermissions);
		Folder f = ffac.findFolderByPath(id.getParentPath(), host);

		if(f == null) return null;
		if (!papi.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + f.getName());
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

	public List<Folder> findSubFolders(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {
		if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folder.getName());
		}

		return ffac.getFoldersByParent(folder, user, respectFrontEndPermissions);
	}

	/**
	 *
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */

	public List<Folder> findSubFolders(Host host, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {

		if (!papi.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + host.getInode());
		}

		return ffac.findFoldersByHost(host);
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

		if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folder.getName());
		}

		List<Folder> subFolders = findSubFolders(folder, user, respectFrontEndPermissions);
		List<Folder> toIterateOver = new ArrayList<Folder>(subFolders);
		for (Folder f : toIterateOver) {
			if (papi.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
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

	public List<Folder> findSubFoldersRecursively(Host host, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {
		if (!papi.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + host.getHostname());
		}
		List<Folder> subFolders = ffac.findFoldersByHost(host);
		List<Folder> toIterateOver = new ArrayList<Folder>(subFolders);
		for (Folder f : toIterateOver) {
			if (papi.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
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

	public void copy(Folder folderToCopy, Folder newParentFolder, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException, DotStateException, IOException {

		if (!papi.doesUserHavePermission(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folderToCopy.getName());
		}

		if (!papi.doesUserHavePermission(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to add to " + newParentFolder.getName());
		}

		ffac.copy(folderToCopy, newParentFolder);
	}


	public void copy(Folder folderToCopy, Host newParentHost, User user, boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException, DotStateException, IOException {
		if (!papi.doesUserHavePermission(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folderToCopy.getName());
		}

		if (!papi.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to add to " + newParentHost.getHostname());
		}

		ffac.copy(folderToCopy, newParentHost);
	}


	public boolean exists(String folderInode) throws DotDataException {
		return ffac.exists(folderInode);
	}


	@SuppressWarnings("unchecked")
	public List<Inode> findMenuItems(Folder folder, User user, boolean respectFrontEndPermissions) throws DotStateException, DotDataException {
		return ffac.getMenuItems(folder);
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

	public void delete(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {

		if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to edit " + folder.getName());
		}


		boolean localTransaction = false;

		// start transactional delete
		try {
			localTransaction = DbConnectionFactory.getConnection().getAutoCommit();

			if (localTransaction) {
				HibernateUtil.startTransaction();
			}
			PermissionAPI papi = APILocator.getPermissionAPI();
			if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {
				Logger.error(this.getClass(), "User " + user.getUserId() + " does not have permissions to folder " + folder.getInode());
				throw new DotSecurityException("User " + "does not have edit permissions on folder " + folder.getTitle());
			}

			Folder faker = new Folder();
			faker.setShowOnMenu(folder.isShowOnMenu());
			faker.setInode(folder.getInode());
			faker.setIdentifier(folder.getIdentifier());

			List<Folder> folderChildren = findSubFolders(folder, user, respectFrontEndPermissions);

			// recursivily delete
			for (Folder childFolder : folderChildren) {
				// sub deletes use system user - if a user has rights to parent
				// permission (checked above) they can delete to children
				delete(childFolder, user, respectFrontEndPermissions);
			}
			
			// delete assets in this folder
			_deleteChildrenAssetsFromFolder(folder, user, respectFrontEndPermissions);
			APILocator.getPermissionAPI().removePermissions(folder);

			//http://jira.dotmarketing.net/browse/DOTCMS-6362
			APILocator.getContentletAPIImpl().removeFolderReferences(folder);

			// delete folder itself
			ffac.delete(folder);

			// delete the menus using the fake proxy inode
			if (folder.isShowOnMenu()) {
				// RefreshMenus.deleteMenus();
				RefreshMenus.deleteMenu(faker);
			}
			
			if(localTransaction){
                HibernateUtil.commitTransaction();
            }

		} catch (Exception e) {

			if (localTransaction) {
				HibernateUtil.rollbackTransaction();
			}
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
				capi.delete(c, user, false);
			}

			/************ htmlPages *****************/
			HibernateUtil.getSession().clear();
			List<HTMLPage> htmlPages = getHTMLPages(folder, user, respectFrontEndPermissions);
			for (HTMLPage page : htmlPages) {
				APILocator.getHTMLPageAPI().delete((HTMLPage) page, user, false);
			}

			/************ Files *****************/
			HibernateUtil.getSession().clear();
			List<File> files = getFiles(folder, user, respectFrontEndPermissions);
			for (File file : files) {
			    HibernateUtil.getSession().clear();
				APILocator.getFileAPI().delete((File) file, user, false);
			}
			/************ Structures *****************/
			List<Structure> structures = getStructures(folder, user, respectFrontEndPermissions);
			for (Structure struc : structures) {

				// APILocator.getFileAPI().delete((File) file, sys, false);
			}
			/************ Links *****************/
			HibernateUtil.getSession().clear();
			List<Link> links = getLinks(folder, user, respectFrontEndPermissions);
			for (Link linker : links) {
				Link link = (Link) linker;
				if (link.isWorking()) {

					Identifier identifier = APILocator.getIdentifierAPI().find(link);

					if (!InodeUtils.isSet(identifier.getInode())) {
						Logger.warn(FolderFactory.class, "_deleteChildrenAssetsFromFolder: link inode = " + link.getInode()
								+ " doesn't have a valid identifier associated.");
						continue;
					}

					papi.removePermissions(link);

					APILocator.getIdentifierAPI().delete(identifier);
				}
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
			StructureFactory.updateFolderReferences(folder);
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

	public Folder find(String id, User user, boolean respectFrontEndPermissions)throws DotSecurityException, DotDataException {

		Folder folder= ffac.find(id);
		if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folder.getName());
		}
		return folder;
	}


	/**
	 * Saves a folder
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 */

	public void save(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotStateException, DotSecurityException {

		Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
		if(id ==null || !UtilMethods.isSet(id.getId())){
			throw new DotStateException("folder must already have an identifier before saving");
		}

		Host host = APILocator.getHostAPI().find(folder.getHostId(), user, respectFrontEndPermissions);
		Folder parentFolder = findFolderByPath(id.getParentPath(), id.getHostId(), user, respectFrontEndPermissions);
		Permissionable parent = id.getParentPath().equals("/")?host:parentFolder;
		String name = id.getParentPath().equals("/")?host.getHostname():parentFolder.getName();


		if(parent ==null){
			throw new DotStateException("No Folder Found for id: " + id.getParentPath());
		}
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user,respectFrontEndPermissions)
				|| !papi.doesUserHavePermissions(PermissionableType.FOLDERS, PermissionAPI.PERMISSION_EDIT, user)) {
			throw new DotSecurityException("User " + user + " does not have permission to add to " + name);
		}

		ffac.save(folder);

	}


	public Folder findSystemFolder() throws DotDataException {
		return ffac.findSystemFolder();
	}




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
				f.setHostId(host.getIdentifier());
				f.setDefaultFileType(StructureCache.getStructureByVelocityVarName(APILocator.getFileAssetAPI().DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode());
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



	public List<Folder> findFoldersByHost(Host host, User user, boolean respectFrontendRoles) throws DotHibernateException {
		return ffac.findFoldersByHost(host);
	}
	public  List<HTMLPage> getHTMLPages(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
	DotDataException, DotSecurityException{
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
		cond.working=true;
		cond.deleted=false;
		List list = ffac.getChildrenClass(parent, HTMLPage.class,cond);
		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	public  List<HTMLPage> getHTMLPages(Folder parent, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions) throws DotStateException,
    DotDataException, DotSecurityException{
        if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
        }
        ChildrenCondition cond = new ChildrenCondition();
        cond.working=working;
        cond.deleted=deleted;
        List list = ffac.getChildrenClass(parent, HTMLPage.class,cond);
        return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

	public  List<Link> getLinks(Folder parent, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions) throws DotStateException,
    DotDataException, DotSecurityException{
        if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
        }
        ChildrenCondition cond = new ChildrenCondition();
        cond.working=working;
        cond.deleted=deleted;
        List list = ffac.getChildrenClass(parent, Link.class,cond);
        return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

	public  List<Link> getLinks(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + parent.getName());
		}
		List list = ffac.getChildrenClass(parent, Link.class);
		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}
	public  List<File> getFiles(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + parent.getName());
		}
		List list = ffac.getChildrenClass(parent, File.class);
		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}
	public  List<Contentlet> getContent(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + parent.getName());
		}

		List list = ffac.getChildrenClass(parent, Contentlet.class);
		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}
	public  List<Structure> getStructures(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
		DotDataException, DotSecurityException{
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + parent.getName());
		}
		List list = StructureFactory.getStructures("folder='"+parent.getInode()+"'", null, 0, 0, null);
		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	public boolean isChildFolder(Folder folder1, Folder folder2) throws DotDataException,DotSecurityException {
	   return ffac.isChildFolder(folder1, folder2);
	}


	public boolean matchFilter(Folder folder, String fileName) {
		return ffac.matchFilter(folder, fileName);
	}


	public List findMenuItems(Folder folder, int orderDirection) throws DotDataException{
		return ffac.getMenuItems(folder, orderDirection);
	}


	public List<Object> buildNavigationTree(List items, int depth,User user)throws DotDataException {
		return ffac.buildNavigationTree(items, depth,user);
	}


	public List<Inode> findMenuItems(Host host,User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		return ffac.getMenuItems(host);
	}


	public List<Folder> findSubFoldersTitleSort(Folder folder,User user,boolean respectFrontEndPermissions)throws DotDataException {
		return ffac.getSubFoldersTitleSort(folder);
	}


	public boolean move(Folder folderToMove, Folder newParentFolder,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(folderToMove, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folderToMove.getName());
		}

		if (!papi.doesUserHavePermission(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to add to " + newParentFolder.getName());
		}
		return ffac.move(folderToMove, newParentFolder);
	}


	public boolean move(Folder folderToMove, Host newParentHost,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(folderToMove, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folderToMove.getName());
		}

		if (!papi.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to add to " + newParentHost.getHostname());
		}
		return ffac.move(folderToMove, newParentHost);
	}

	public List<Folder> findSubFolders(Host host, boolean showOnMenu) throws DotHibernateException{
		return ffac.findSubFolders(host, showOnMenu);
	}

	public List<Folder> findSubFolders(Folder folder,boolean showOnMenu)throws DotStateException, DotDataException {
		return ffac.findSubFolders(folder, showOnMenu);
	}

	public List<String> getEntriesTree(Folder mainFolder, String openNodes,String view, String content, String structureInode,User user)
			throws DotStateException, DotDataException, DotSecurityException {
		Locale locale = user.getLocale();
		TimeZone timeZone = user.getTimeZone();
		Role[] roles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
		return ffac.getEntriesTree(mainFolder, openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
	}


	public List<String> getFolderTree(String openNodes, String view,
			String content, String structureInode,User user)
			throws DotStateException, DotDataException, DotSecurityException {
		Locale locale = user.getLocale();
		TimeZone timeZone = user.getTimeZone();
		Role[] roles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
		return ffac.getFolderTree(openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
	}

	public List<Contentlet> getLiveContent(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
		cond.live=true;
		cond.deleted=false;
		List list = ffac.getChildrenClass(parent, Contentlet.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	public List<File> getLiveFiles(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
		cond.live=true;
		cond.deleted=false;
		List list = ffac.getChildrenClass(parent, File.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	public List<File> getLiveFilesSortTitle(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.live=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, File.class, cond, "file_name asc");

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	public List<File> getLiveFilesSortOrder(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.live=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, File.class, cond, "sort_order asc");

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}
	public List<HTMLPage> getLiveHTMLPages(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.live=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, HTMLPage.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	public List<Link> getLiveLinks(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.live=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, Link.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	public List<Contentlet> getWorkingContent(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, Contentlet.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	public List<File> getWorkingFiles(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, File.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	public List<HTMLPage> getWorkingHTMLPages(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, HTMLPage.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	public List<Link> getWorkingLinks(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read  " + parent.getName());
		}
		ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        cond.deleted=false;
		List list = ffac.getChildrenClass(parent, Link.class, cond);

		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}


	public List<File> getFiles(Folder parent, User user, boolean respectFrontEndPermissions, ChildrenCondition cond)
	throws DotStateException, DotDataException, DotSecurityException {

		if (!papi.doesUserHavePermission(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + parent.getName());
		}
		List list = ffac.getChildrenClass(parent, File.class, cond, null);
		return papi.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
	}

	public boolean updateIdentifierUrl(Folder folderToUpdate, Folder newParentFolder,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException {
		if (!papi.doesUserHavePermission(folderToUpdate, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + folderToUpdate.getName());
		}

		if (!papi.doesUserHavePermission(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to add to " + newParentFolder.getName());
		}
		return ffac.updateIdentifierUrl(folderToUpdate, newParentFolder);
	}

}
