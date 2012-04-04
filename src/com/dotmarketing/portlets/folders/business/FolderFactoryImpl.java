package com.dotmarketing.portlets.folders.business;
// 1212
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionCache;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.AssetsComparator;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 *
 * @author maria 2323
 */
public class FolderFactoryImpl extends FolderFactory {
	private int nodeId;

	private FolderCache fc = CacheLocator.getFolderCache();
	private java.text.DateFormat loginDateFormat;

	@Override
	protected boolean exists(String folderInode) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("select inode  from folder where inode = ?");
		dc.addParam(folderInode);
		return dc.loadResults().size() > 0;

	}


	@Override
	protected void delete(Folder f) throws DotDataException {
		Identifier id = APILocator.getIdentifierAPI().find(f.getIdentifier());
		HibernateUtil.delete(f);
		fc.removeFolder(f, id);
	}

	/*
	 * protected boolean existsFolder(long folderInode) { return
	 * existsFolder(Long.toString(folderInode)); }
	 */
	@Override
	protected Folder find(String folderInode) throws DotDataException {
		Folder folder = fc.getFolder(folderInode);
		if (folder == null) {
			try{
				folder = (Folder) new HibernateUtil(Folder.class).load(folderInode);
				Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
				fc.addFolder(folder, id);
			}
			catch(Exception e){
				throw new DotDataException(e.getMessage());
			}

		}
		return folder;
	}


	@SuppressWarnings("unchecked")
	@Override
	protected java.util.List<Folder> getSubFolders(Folder folder) throws DotStateException, DotDataException {

		Identifier id = APILocator.getIdentifierAPI().find(folder);

		HibernateUtil dh = new HibernateUtil(Folder.class);
		List<Folder> list = null;
		String query = "SELECT {folder.*} from folder folder, inode folder_1_, identifier identifier where folder.identifier = identifier.id and "
				+ "folder_1_.type = 'folder' and folder_1_.inode = folder.inode and identifier.parent_path = ? and host_inode = ? order by name, sort_order";

		dh.setSQLQuery(query);
		dh.setParam(id.getPath());
		dh.setParam(id.getHostId());
		list = (java.util.List<Folder>) dh.list();
        Collections.sort(list,new Comparator<Folder>() {
            public int compare(Folder o1, Folder o2) {
                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
            }
        });
		return list;
	}
	@SuppressWarnings("unchecked")
	@Override
	protected java.util.List<Folder> getSubFoldersTitleSort(Folder folder) throws DotDataException  {
		Identifier id = APILocator.getIdentifierAPI().find(folder);
		HibernateUtil dh = new HibernateUtil(Folder.class);
		List<Folder> folders = null;

		String query = "SELECT {folder.*} from folder folder, inode folder_1_, identifier identifier where folder.identifier = identifier.id and "
				+ "folder_1_.type = 'folder' and folder_1_.inode = folder.inode and identifier.parent_path = ? and host_inode = ? order by folder.title";

		dh.setSQLQuery(query);
		dh.setParam(id.getPath());
		dh.setParam(id.getHostId());
		folders = (java.util.List<Folder>) dh.list();

		return folders;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Folder> findSubFolders(Folder folder, boolean showOnMenu) throws DotStateException, DotDataException {
		Identifier id = APILocator.getIdentifierAPI().find(folder);
		HibernateUtil dh = new HibernateUtil(Folder.class);
		String condition = "";
		if(UtilMethods.isSet(showOnMenu)){
			condition = "show_on_menu = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue();
		}
		dh.setSQLQuery("SELECT {folder.*} from folder folder, inode folder_1_, identifier identifier where folder.identifier = identifier.id and "
				+ "folder_1_.type = 'folder' and folder_1_.inode = folder.inode and identifier.parent_path = ? and host_inode = ? and "
				+ condition + " order by sort_order, name");
		dh.setParam(id.getPath());
		dh.setParam(id.getHostId());
		return (java.util.List<Folder>)dh.list();

	}
	@SuppressWarnings("unchecked")
	@Override
	protected List<Folder> findSubFolders(Host host, boolean showOnMenu) throws DotHibernateException   {

		HibernateUtil dh = new HibernateUtil(Folder.class);
		String condition = "";
		if(UtilMethods.isSet(showOnMenu)){
			condition = "show_on_menu = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue();
		}
		String query = "select {folder.*} from folder, inode folder_1_, identifier identifier where parent_path = '/' and "+
		               "folder_1_.type = 'folder' and folder.inode = folder_1_.inode and folder.identifier = identifier.id and host_inode = ? and "
					   + condition + " order by sort_order, name";

		dh.setSQLQuery(query);
		dh.setParam(host.getIdentifier());

		return (java.util.List<Folder>)dh.list();
	}

	@Override
	protected Folder findFolderByPath(String path, Host host) throws DotDataException {

		if(host==null) return null;

		Folder folder = fc.getFolderByPathAndHost(path, host);
		if(folder ==null){
			String parentPath;
			String assetName;
			String hostId;
			try{
				if(path.equals("/") || path.equals("/System folder")) {
					parentPath="/System folder";
					assetName="system folder";
					hostId="SYSTEM_HOST";
				}
				else {
					// trailing / is removed
					if (path.endsWith("/"))
						path = path.substring(0, path.length()-1);
					// split path into parent and asset name
					int idx=path.lastIndexOf('/');
					parentPath=path.substring(0,idx+1);
					assetName=path.substring(idx+1);
					hostId=host.getIdentifier();
				}
				HibernateUtil dh = new HibernateUtil(Folder.class);
				dh.setSQLQuery("select {folder.*} from folder, inode folder_1_, identifier identifier where asset_name = ? and parent_path = ? and "
						+ "folder_1_.type = 'folder' and folder.inode = folder_1_.inode and folder.identifier = identifier.id and host_inode = ?");
				dh.setParam(assetName);
				dh.setParam(parentPath);
				dh.setParam(hostId);
				folder = (Folder) dh.load();
				if(UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
					// if it is found add it to folder cache
					Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
					fc.addFolder(folder, id);
				}
			}
			catch(Exception e){
				throw new DotDataException(e.getMessage());
			}

		}
		return folder;
	}

	protected List<String> getFolderTree(String hostId, String openNodes, String view, String content, String structureInode, Locale locale,
			TimeZone timeZone, Role[] roles, boolean isAdminUser, User user) throws DotStateException, DotDataException,
			DotSecurityException {
		return getFolderTree(openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
	}

	protected List<Folder> getFoldersByParent(Folder folder, User user, boolean respectFrontendRoles) throws DotDataException {
		List<Folder> entries = new ArrayList<Folder>();
		List<Folder> elements = getSubFoldersTitleSort(folder);
		for (Folder childFolder : elements) {
			if (APILocator.getPermissionAPI().doesUserHavePermission(childFolder, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				entries.add(childFolder);
			}
		}
		return entries;
	}

	@SuppressWarnings("unchecked")
	protected java.util.List getMenuItems(Folder folder) throws DotDataException {
		return getMenuItems(folder, 1);
	}

	@SuppressWarnings("unchecked")
	protected java.util.List getMenuItems(Host host) throws DotDataException, DotSecurityException {
		return getMenuItems(host, 1);
	}

	@SuppressWarnings("unchecked")
	protected java.util.List getMenuItems(Folder folder, int orderDirection) throws  DotDataException{
		List<Folder> folders = new ArrayList<Folder>();
		folders.add(folder);
		return getMenuItems(folders, orderDirection);
	}

	@SuppressWarnings("unchecked")
	protected java.util.List getMenuItems(Host host, int orderDirection) throws DotDataException, DotSecurityException {
		List<Folder> subFolders = APILocator.getFolderAPI().findSubFolders(host,APILocator.getUserAPI().getSystemUser(),false);
		return getMenuItems(subFolders, orderDirection);
	}

	@SuppressWarnings("unchecked")
	private List getMenuItems(List<Folder> folders, int orderDirection) throws DotDataException {

		List menuList = new ArrayList();

		for (Folder folder : folders) {
		    ChildrenCondition cond = new ChildrenCondition();
		    cond.showOnMenu=true;

			// gets all subfolders
			List subFolders = getChildrenClass(folder, Folder.class, cond);

			cond.deleted=false;
			cond.live=true;

			// gets all links for this folder
			List linksListSubChildren = getChildrenClass(folder, Link.class, cond);

			// gets all html pages for this folder
			List htmlPagesSubListChildren = getChildrenClass(folder, HTMLPage.class, cond);

			// gets all files for this folder
			List filesListSubChildren = getChildrenClass(folder, File.class, cond);


			List<FileAsset> fileAssets = null;
			try {
				fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
				for(FileAsset fileAsset : fileAssets) {
					if(fileAsset.isShowOnMenu()){
						filesListSubChildren.add(fileAsset);
					}					
				}				
			} catch (DotSecurityException e) {}

			// gets all subitems
			menuList.addAll(subFolders);
			menuList.addAll(linksListSubChildren);
			menuList.addAll(htmlPagesSubListChildren);
			menuList.addAll(filesListSubChildren);

			Comparator comparator = new AssetsComparator(orderDirection);
			java.util.Collections.sort(menuList, comparator);
		}

		return menuList;
	}

	@SuppressWarnings("unchecked")
	protected java.util.List getAllMenuItems(Inode inode) throws DotStateException, DotDataException {
		return getAllMenuItems(inode, 1);
	}

	@SuppressWarnings("unchecked")
	protected java.util.List getAllMenuItems(Inode inode, int orderDirection) throws DotStateException, DotDataException {

		// gets all subfolders
		java.util.List folderListChildren = getChildrenClass((Folder)inode, Folder.class);

		ChildrenCondition cond=new ChildrenCondition();
		cond.showOnMenu=true;
		cond.deleted=false;
		cond.live=true;

		// gets all links for this folder
		java.util.List linksListSubChildren = getChildrenClass((Folder) inode, Link.class, cond);

		// gets all html pages for this folder
		java.util.List htmlPagesSubListChildren = getChildrenClass((Folder) inode, HTMLPage.class, cond);

		// gets all files for this folder
		java.util.List filesListSubChildren = getChildrenClass((Folder) inode, File.class, cond);

		List<FileAsset> fileAssets = null;
		try {
			fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder((Folder) inode, APILocator.getUserAPI().getSystemUser(), false);
			filesListSubChildren.addAll(fileAssets);
		} catch (DotSecurityException e) {}

		// gets all subitems
		java.util.List menuList = new java.util.ArrayList();
		menuList.addAll(folderListChildren);
		menuList.addAll(linksListSubChildren);
		menuList.addAll(htmlPagesSubListChildren);
		menuList.addAll(filesListSubChildren);

		Comparator comparator = new AssetsComparator(orderDirection);
		java.util.Collections.sort(menuList, comparator);

		return menuList;
	}

	protected Folder createFolders(String path, Host host) throws DotDataException {

		StringTokenizer st = new StringTokenizer(path, "/");
		StringBuffer sb = new StringBuffer("/");

		Folder parent = null;

		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			sb.append(name + "/");
			Folder f = findFolderByPath(sb.toString(), host);
			if (!InodeUtils.isSet(f.getInode())) {
				f.setName(name);
				f.setTitle(name);
				//f.setPath(sb.toString());
				f.setShowOnMenu(false);
				f.setSortOrder(0);
				f.setHostId(host.getIdentifier());
				Identifier ident;
				if(!UtilMethods.isSet(parent))
					ident = createIdentifierForFolder(f, "/");
				else {
					Identifier parentId=APILocator.getIdentifierAPI().find(parent.getIdentifier());
					ident = createIdentifierForFolder(f, parentId.getPath());
				}
				f.setIdentifier(ident.getId());
				HibernateUtil.saveOrUpdate(f);
			}
			parent = f;

		}
		return parent;

	}

	@SuppressWarnings("unchecked")
	private void copy(Folder folder, Host destination, Hashtable copiedObjects) throws DotDataException, DotSecurityException, DotStateException, IOException {

		boolean rename = APILocator.getHostAPI().doesHostContainsFolder((Host) destination, folder.getName());

		Folder newFolder = new Folder();
		newFolder.copy(folder);
		newFolder.setName(folder.getName());
		while (rename) {
			newFolder.setName(newFolder.getName() + "_copy");
			rename = APILocator.getHostAPI().doesHostContainsFolder((Host) destination, newFolder.getName());
		}

		//newFolder.setPath("/" + newFolder.getName() + "/");
		newFolder.setHostId(destination.getIdentifier());
		Identifier parentId=APILocator.getIdentifierAPI().find(destination.getIdentifier());
		Identifier newFolderId = createIdentifierForFolder(newFolder, parentId.getPath());
		newFolder.setIdentifier(newFolderId.getId());

		save(newFolder);

		saveCopiedFolder(folder, newFolder, copiedObjects);
	}

	@SuppressWarnings("unchecked")
	private void copy(Folder folder, Folder destination, Hashtable copiedObjects) throws DotDataException, DotStateException, DotSecurityException, IOException {

		boolean rename = folderContains(folder.getName(), (Folder) destination);

		Folder newFolder = new Folder();
		newFolder.copy(folder);
		newFolder.setName(folder.getName());
		while (rename) {
			newFolder.setName(newFolder.getName() + "_copy");
			rename = folderContains(newFolder.getName(), (Folder) destination);
		}

		//newFolder.setPath(((Folder) destination).getPath() + newFolder.getName() + "/");
		newFolder.setHostId(((Folder) destination).getHostId());
		Identifier parentId=APILocator.getIdentifierAPI().find(destination.getIdentifier());
		Identifier newFolderId = createIdentifierForFolder(newFolder, parentId.getPath());
		newFolder.setIdentifier(newFolderId.getId());

		save(newFolder);

		// TreeFactory.saveTree(new Tree(destination.getInode(),
		// newFolder.getInode()));

		saveCopiedFolder(folder, newFolder, copiedObjects);
	}

	private void saveCopiedFolder(Folder source, Folder newFolder, Hashtable copiedObjects) throws DotDataException, DotStateException, DotSecurityException, IOException {
		User systemUser = APILocator.getUserAPI().getSystemUser();

		if (copiedObjects == null)
			copiedObjects = new Hashtable();

		// Copying folder permissions
		APILocator.getPermissionAPI().copyPermissions(source, newFolder);

		// Copying children html pages
		Map<String, HTMLPage[]> pagesCopied;
		if (copiedObjects.get("HTMLPages") == null) {
			pagesCopied = new HashMap<String, HTMLPage[]>();
			copiedObjects.put("HTMLPages", pagesCopied);
		} else {
			pagesCopied = (Map<String, HTMLPage[]>) copiedObjects.get("HTMLPages");
		}

		List pages = getChildrenClass(source, HTMLPage.class);
		for (HTMLPage page : (List<HTMLPage>) pages) {
			if (page.isWorking()) {
				HTMLPage newPage = HTMLPageFactory.copyHTMLPage(page, newFolder);
				// Saving copied pages to update template - pages relationships
				// later
				pagesCopied.put(page.getInode(), new HTMLPage[] { page, newPage });
			}
		}

		// Copying Files
		Map<String, IFileAsset[]> filesCopied;
		if (copiedObjects.get("Files") == null) {
			filesCopied = new HashMap<String, IFileAsset[]>();
			copiedObjects.put("Files", filesCopied);
		} else {
			filesCopied = (Map<String, IFileAsset[]>) copiedObjects.get("Files");
		}

		List files = getChildrenClass(source, File.class);
		for (File file : (List<File>) files) {
			if (file.isWorking()) {
				File newFile = APILocator.getFileAPI().copyFile(file, newFolder, APILocator.getUserAPI().getSystemUser(), false);
				// Saving copied pages to update template - pages relationships
				// later
				filesCopied.put(file.getInode(), new File[] { file, newFile });
			}
		}

		//Content Files
		List<FileAsset> faConts = APILocator.getFileAssetAPI().findFileAssetsByFolder(source, APILocator.getUserAPI().getSystemUser(), false);
		for(FileAsset fa : faConts){
			if(fa.isWorking()){
				Contentlet cont = APILocator.getContentletAPI().find(fa.getInode(), APILocator.getUserAPI().getSystemUser(), false);
				APILocator.getContentletAPI().copyContentlet(cont, newFolder, APILocator.getUserAPI().getSystemUser(), false);
				filesCopied.put(cont.getInode(), new IFileAsset[] {fa , APILocator.getFileAssetAPI().fromContentlet(cont)});
			}
		}

		// Copying links
		Map<String, Link[]> linksCopied;
		if (copiedObjects.get("Links") == null) {
			linksCopied = new HashMap<String, Link[]>();
			copiedObjects.put("Links", linksCopied);
		} else {
			linksCopied = (Map<String, Link[]>) copiedObjects.get("Links");
		}

		List links = getChildrenClass(source,Link.class);
		for (Link link : (List<Link>) links) {
			if (link.isWorking()) {
				Link newLink = LinkFactory.copyLink(link, newFolder);
				// Saving copied pages to update template - pages relationships
				// later
				linksCopied.put(link.getInode(), new Link[] { link, newLink });
			}
		}

		// Copying Inner Folders
		List childrenFolder = APILocator.getFolderAPI().findSubFolders(source,systemUser,false);
		for (Folder childFolder : (List<Folder>) childrenFolder) {
			copy(childFolder, newFolder, copiedObjects);
		}

	}

	protected void copy(Folder folder, Host destination) throws DotDataException, DotSecurityException, DotStateException, IOException {
		copy(folder, destination, null);
	}

	protected void copy(Folder folder, Folder destination) throws DotDataException, DotStateException, DotSecurityException, IOException {
		copy(folder, destination, null);
	}

	@SuppressWarnings("unchecked")
	private boolean folderContains(String name, Folder destination) throws DotStateException, DotDataException, DotSecurityException {
		List<Folder> children = APILocator.getFolderAPI().findSubFolders(destination,APILocator.getUserAPI().getSystemUser(),false);
		for (Folder folder : children) {
			if (folder.getName().equals(name))
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean move(Folder folder, Object destination) throws DotDataException, DotStateException, DotSecurityException {

		IdentifierAPI identAPI = APILocator.getIdentifierAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();
		boolean contains = false;
		String newParentPath;
		String newParentHostId;
		if (destination instanceof Folder) {
			contains = folderContains(folder.getName(), (Folder) destination);
			Identifier destinationId = identAPI.find(((Folder) destination).getIdentifier());
			newParentPath = destinationId.getPath();
			newParentHostId = destinationId.getHostId();
		} else {
			contains = APILocator.getHostAPI().doesHostContainsFolder((Host) destination, folder.getName());
			newParentPath = "/";
			newParentHostId = ((Host)destination).getIdentifier();
		}
		if (contains)
			return false;

		List<Folder> subFolders = getSubFolders(folder);
		List htmlPages = getChildrenClass(folder, HTMLPage.class);
		List files = getChildrenClass(folder, File.class);
		List links = getChildrenClass(folder, Link.class);
		List<FileAsset> fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
		List<Contentlet> contentlets = APILocator.getContentletAPI().findContentletsByFolder(folder, systemUser, false);

		Identifier folderId = identAPI.find(folder.getIdentifier());
		folderId.setParentPath(newParentPath);
		folderId.setHostId(newParentHostId);
		identAPI.save(folderId);

		for (Object page : htmlPages) {
			APILocator.getHTMLPageAPI().movePage((HTMLPage) page, folder, systemUser, false);
		}

		for (Object file : files) {
			APILocator.getFileAPI().moveFile((File) file, folder, systemUser, false);
		}

		for (Object link : links) {
			if (((Link) link).isWorking()) {
				LinkFactory.moveLink((Link) link, folder);
			}
		}

		for(FileAsset fa : fileAssets){
			APILocator.getFileAssetAPI().moveFile(fa, folder, systemUser, false);
		}

		for(Contentlet cont : contentlets){
			boolean isLive = cont.isLive();
			cont.setFolder(folder.getInode());
			cont.setInode(null);
			Contentlet newCont = APILocator.getContentletAPI().checkin(cont, systemUser, false);
			if(isLive){
				APILocator.getContentletAPI().publish(newCont, systemUser, false);
			}
		}

		for(Folder subFolder : subFolders){
			move(subFolder, (Object)folder);
		}

		CacheLocator.getIdentifierCache().clearCache();

		return true;
	}

	/***
	 * This methos update recursively the inner folders of the specified folder
	 *
	 * @param folder
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	@SuppressWarnings("unchecked")
	private void moveRecursiveFolders(Folder oldFolder, Folder newFolder) throws DotStateException, DotDataException, DotSecurityException {
		Stack<Folder> innerFolders = new Stack();

		innerFolders.addAll(APILocator.getFolderAPI().findSubFolders(oldFolder, APILocator.getUserAPI().getSystemUser(), false));
		while (!innerFolders.empty()) {
			Folder nextFolder = innerFolders.pop();
			Host destinationHost = APILocator.getHostAPI().findParentHost(newFolder, APILocator.getUserAPI().getSystemUser(), false);
			String newPath = APILocator.getIdentifierAPI().find(newFolder).getPath()+nextFolder.getName()+"/";
			Folder nextNewFolder = APILocator.getFolderAPI().createFolders(newPath, destinationHost, APILocator.getUserAPI().getSystemUser(), false);

			List htmlPages = getChildrenClass(nextFolder, HTMLPage.class);
			List files = getChildrenClass(nextFolder,File.class);
			List links = getChildrenClass(nextFolder,Link.class);

			updateMovedFolderAssets(nextFolder, nextNewFolder, htmlPages, files, links);
			moveRecursiveFolders(nextFolder, nextNewFolder);
		}
	}

	/**
	 * this method updates the asset info for the new paths
	 *
	 * @param theFolder
	 *            the folder moved
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	protected void updateMovedFolderAssets(Folder theFolder) throws DotDataException, DotStateException, DotSecurityException {

		User systemUser;
		Host newHost;
		try {
			systemUser = APILocator.getUserAPI().getSystemUser();
			newHost = APILocator.getHostAPI().findParentHost(theFolder, systemUser, false);
		} catch (DotDataException e) {
			Logger.error(FolderFactoryImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(FolderFactoryImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		List htmlPages = getChildrenClass(theFolder,HTMLPage.class);
		for (HTMLPage page : (List<HTMLPage>) htmlPages) {
			Identifier identifier = APILocator.getIdentifierAPI().find(page);

			if (page.isWorking()) {
				// updating caches
				WorkingCache.removeAssetFromCache(page);
				CacheLocator.getIdentifierCache().removeFromCacheByVersionable(page);
			}

			if (page.isLive()) {
				LiveCache.removeAssetFromCache(page);
			}

			if (page.isWorking()) {
				// gets identifier for this webasset and changes the uri and
				// persists it
				identifier.setHostId(newHost.getIdentifier());
				identifier.setURI(page.getURI(theFolder));
				APILocator.getIdentifierAPI().save(identifier);
			}

			// Add to Preview and Live Cache
			if (page.isLive()) {
				LiveCache.removeAssetFromCache(page);
				LiveCache.addToLiveAssetToCache(page);
			}
			if (page.isWorking()) {
				WorkingCache.removeAssetFromCache(page);
				WorkingCache.addToWorkingAssetToCache(page);
				CacheLocator.getIdentifierCache().removeFromCacheByVersionable(page);

			}

			// republishes the page to reset the VTL_SERVLETURI variable
			if (page.isLive()) {
				PageServices.invalidate(page);
			}

		}

		List<File> files = APILocator.getFolderAPI().getFiles(theFolder, systemUser, false);
		for (File file : files) {
			Identifier identifier = APILocator.getIdentifierAPI().find(file);

			// assets cache
			if (file.isLive())
				LiveCache.removeAssetFromCache(file);

			if (file.isWorking())
				WorkingCache.removeAssetFromCache(file);

			if (file.isWorking()) {
				// gets identifier for this webasset and changes the uri and
				// persists it
				identifier.setHostId(newHost.getIdentifier());
				identifier.setURI(file.getURI(theFolder));
				APILocator.getIdentifierAPI().save(identifier);
			}

			// Add to Preview and Live Cache
			if (file.isLive()) {
				LiveCache.addToLiveAssetToCache(file);
			}
			if (file.isWorking())
				WorkingCache.addToWorkingAssetToCache(file);

		}
		List<FileAsset> fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder(theFolder, APILocator.getUserAPI().getSystemUser(), false);
		for(FileAsset fa : fileAssets){
			Identifier identifier = APILocator.getIdentifierAPI().find(fa);
			Contentlet fileAssetCont = APILocator.getContentletAPI().find(fa.getInode(), APILocator.getUserAPI().getSystemUser(), false);

			// assets cache
			if (fileAssetCont.isLive())
				LiveCache.removeAssetFromCache(fa);

			if (fileAssetCont.isWorking())
				WorkingCache.removeAssetFromCache(fa);

			if (fileAssetCont.isWorking()) {
				// gets identifier for this webasset and changes the uri and
				// persists it
				identifier.setHostId(newHost.getIdentifier());
				Identifier folderIdentifier  = APILocator.getIdentifierAPI().find(theFolder);
				identifier.setParentPath(folderIdentifier.getPath());
				APILocator.getIdentifierAPI().save(identifier);
			}

			// Add to Preview and Live Cache
			if (fileAssetCont.isLive()) {
				LiveCache.addToLiveAssetToCache(fileAssetCont);
			}
			if (fileAssetCont.isWorking())
				WorkingCache.addToWorkingAssetToCache(fileAssetCont);

		}

		List<Link> links = APILocator.getFolderAPI().getLinks(theFolder, systemUser, false);
		for (Link link : links) {
			if (link.isWorking()) {

				Identifier identifier = APILocator.getIdentifierAPI().find(link);

				// gets identifier for this webasset and changes the uri and
				// persists it
				identifier.setHostId(newHost.getIdentifier());
				identifier.setURI(link.getURI(theFolder));
				APILocator.getIdentifierAPI().save(identifier);
			}

		}
		CacheLocator.getIdentifierCache().clearCache();
	}

	/**
	 * this method updates the asset info for the new paths
	 *
	 * @param oldFolder
	 *            the folder moved
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void updateMovedFolderAssets(Folder oldFolder, Folder newFolder, List<HTMLPage> htmlPages, List<File> files, List<Link> links)
			throws DotDataException, DotStateException, DotSecurityException {

		User systemUser;

		try {
			systemUser = APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			Logger.error(FolderFactoryImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		for (HTMLPage page : htmlPages) {
			APILocator.getHTMLPageAPI().movePage(page, newFolder, systemUser, false);
		}

		for (File file : files) {
			APILocator.getFileAPI().moveFile(file, newFolder, systemUser, false);
		}

		for (Link link : links) {
			if (link.isWorking()) {
				LinkFactory.moveLink(link, newFolder);
			}
		}

		List<FileAsset> fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder(oldFolder, APILocator.getUserAPI().getSystemUser(), false);
		for(FileAsset fa : fileAssets){
			boolean appendCopyToFileName = false;
			Contentlet newFileAsset = APILocator.getContentletAPI().copyContentlet(fa, newFolder, systemUser, appendCopyToFileName, false);
			if(fa.isLive()){
				APILocator.getVersionableAPI().setLive(newFileAsset);
				LiveCache.addToLiveAssetToCache(newFileAsset);
			}
		}
		CacheLocator.getIdentifierCache().clearCache();
	}

	protected boolean move(Folder folder, Folder destination) throws DotDataException, DotSecurityException {
		return move(folder, (Object) destination);
	}

	protected boolean move(Folder folder, Host destination) throws DotDataException, DotSecurityException {
		return move(folder, (Object) destination);
	}

	/**
	 * Checks if folder1 is child of folder2
	 *
	 * @param folder1
	 * @param folder2
	 * @return
	 * @throws DotDataException
	 * @throws DotIdentifierStateException
	 * @throws DotSecurityException
	 */
	protected boolean isChildFolder(Folder folder1, Folder folder2) throws  DotDataException, DotSecurityException {
		Folder parentFolder = (Folder) APILocator.getFolderAPI().findParentFolder(folder1,APILocator.getUserAPI().getSystemUser(),false);
		if (parentFolder==null || !InodeUtils.isSet(parentFolder.getInode()))
			return false;
		else {
			if (parentFolder.getInode().equalsIgnoreCase(folder2.getInode())) {
				return true;
			}
			return isChildFolder(parentFolder, folder2);
		}
	}

	@Override
	protected boolean renameFolder(Folder folder, String newName, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		// checking if already exists
		Identifier ident = APILocator.getIdentifierAPI().find(folder);
		String newPath=ident.getParentPath()+newName;
		Host host = APILocator.getHostAPI().find(folder.getHostId(),user,respectFrontEndPermissions);
		Folder nFolder=findFolderByPath(newPath, host);
		if(UtilMethods.isSet(nFolder.getInode()))
			return false;

		// renaming folder

		// first we remove from cache with current name and path
		/*CacheLocator.getFolderCache().removeFolder(folder, ident);

		// get a fresh copy of the object passed to make sure hibernate works ok

        Folder ff=(Folder) HibernateUtil.load(Folder.class, folder.getInode());
		ff.setName(newName);
		ff.setTitle(newName);
		ident.setAssetName(newName);

		APILocator.getIdentifierAPI().save(ident);
		APILocator.getFolderAPI().save(ff, user, respectFrontEndPermissions);
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(ff);
		*/
		return true;
	}

	@SuppressWarnings("unchecked")
	private boolean updateIdentifierUrl(Folder folder, Object destination) throws DotDataException, DotStateException, DotSecurityException {

		IdentifierAPI identAPI = APILocator.getIdentifierAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();
		boolean contains = false;
		String newParentPath;
		String newParentHostId;
		if (destination instanceof Folder) {
			contains = folderContains(folder.getName(), (Folder) destination);
			Identifier destinationId = identAPI.find(((Folder) destination).getIdentifier());
			newParentPath = destinationId.getPath();
			newParentHostId = destinationId.getHostId();
		} else {
			contains = APILocator.getHostAPI().doesHostContainsFolder((Host) destination, folder.getName());
			newParentPath = "/";
			newParentHostId = ((Host)destination).getIdentifier();
		}
		if (contains)
			return false;

		List<Folder> subFolders = getSubFolders(folder);
		List htmlPages = getChildrenClass(folder, HTMLPage.class);
		List files = getChildrenClass(folder, File.class);
		List links = getChildrenClass(folder, Link.class);
		List<FileAsset> fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
		List<Contentlet> contentlets = APILocator.getContentletAPI().findContentletsByFolder(folder, systemUser, false);

		Identifier folderId = identAPI.find(folder.getIdentifier());
		folderId.setParentPath(newParentPath);
		folderId.setHostId(newParentHostId);
		identAPI.save(folderId);

		for (Object page : htmlPages) {
			APILocator.getIdentifierAPI().updateIdentifierURI((Versionable) page, folder);
		}

		for (Object file : files) {
			APILocator.getIdentifierAPI().updateIdentifierURI((Versionable) file, folder);
		}

		for (Object link : links) {
			if (((Link) link).isWorking()) {
				APILocator.getIdentifierAPI().updateIdentifierURI((Versionable) link, folder);
			}
		}

		for(FileAsset fa : fileAssets){
			APILocator.getIdentifierAPI().updateIdentifierURI((Versionable) fa, folder);
		}

		for(Contentlet cont : contentlets){
			boolean isLive = cont.isLive();
			cont.setFolder(folder.getInode());
			cont.setInode(null);
			Contentlet newCont = APILocator.getContentletAPI().checkin(cont, systemUser, false);
			if(isLive){
				APILocator.getIdentifierAPI().updateIdentifierURI((Versionable) cont, folder);
			}
		}

		for(Folder subFolder : subFolders){
			updateIdentifierUrl(subFolder, (Object)folder);
		}

		CacheLocator.getIdentifierCache().clearCache();
		CacheLocator.getFolderCache().clearCache();

		return true;
	}

	protected boolean updateIdentifierUrl(Folder folder, Folder destination) throws DotDataException, DotSecurityException {
		return updateIdentifierUrl(folder, (Object) destination);
	}

	protected boolean matchFilter(Folder folder, String fileName) {
		// return value
		Perl5Matcher p5m = new Perl5Matcher();
		Perl5Compiler p5c = new Perl5Compiler();
		boolean match = false;
		try {
			// Obtain the filters
			String filesMasks = folder.getFilesMasks();
			filesMasks = (filesMasks != null ? filesMasks.trim() : filesMasks);

			if (UtilMethods.isSet(filesMasks)) {
				String[] filesMasksArray = filesMasks.split(",");
				int length = filesMasksArray.length;

				// Try to match de filters
				for (int i = 0; i < length; i++) {
					String regex = filesMasksArray[i];
					regex = regex.replace(".", "\\.");
					regex = regex.replace("*", ".*");
					regex = "^" + regex.trim() + "$";
					Pattern pattern = p5c.compile(regex, Perl5Compiler.CASE_INSENSITIVE_MASK);
					match = match || p5m.matches(fileName, pattern);
					if (match) {
						break;
					}
				}
			} else {
				match = true;
			}
		} catch (Exception ex) {
			Logger.debug(FolderFactoryImpl.class, ex.toString());
		}
		return match;
	}

	/**
	 * Gets the tree containing all the items in the list. If the item is a
	 * folder, gets the files and folders contained by the folder. It is
	 * executed recursively until reaching the depth specified. This method will
	 * change later
	 *
	 * @param items
	 * @param ids
	 * @param level
	 * @param counter
	 * @param depth
	 * @throws DotDataException
	 */
	protected List getNavigationTree(List items, List<Integer> ids, int level, InternalCounter counter, int depth, User user)
			throws DotDataException {
		boolean show = true;
		StringBuffer sb = new StringBuffer();
		List v = new ArrayList<Object>();
		int internalCounter = counter.getCounter();
		String className = "class" + internalCounter;
		String id = "list" + internalCounter;
		ids.add(internalCounter);
		counter.setCounter(++internalCounter);

		sb.append("<ul id='" + id + "' >\n");
		Iterator itemsIter = items.iterator();
		while (itemsIter.hasNext()) {
			Permissionable item = (Permissionable) itemsIter.next();
			String title = "";
			String inode = "";
			if (item instanceof Folder) {
				Folder folder = ((Folder) item);
				title = folder.getTitle();
				title = retrieveTitle(title, user);
				inode = folder.getInode();
				if (folder.isShowOnMenu()) {
					if (!APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
						show = false;
					}
					if (APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false)) {

						sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >\n" + title + "\n");
						List childs = getMenuItems(folder);
						if (childs.size() > 0) {

							int nextLevel = level + 1;

							if (depth > 1) {
								sb.append(getNavigationTree(childs, ids, nextLevel, counter, depth-1, user).get(0));
							}
						}
						sb.append("</li>\n");
					}
				}
			} else {
				if(item instanceof FileAsset){
					FileAsset fa =(FileAsset)item;
					title = fa.getTitle();
					title = retrieveTitle(title, user);
					inode = fa.getInode();
					if (fa.isShowOnMenu()) {
						if (!APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
							show = false;
						}
						if (APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_READ, user, false)) {
							sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
						}
					}
				}else{
					WebAsset asset = ((WebAsset) item);
					title = asset.getTitle();
					title = retrieveTitle(title, user);
					inode = asset.getInode();
					if (asset.isShowOnMenu()) {
						if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
							show = false;
						}
						if (APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ, user, false)) {
							sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
						}
					}
				}
			}
		}
		sb.append("</ul>\n");
		v.add(sb);
		v.add(new Boolean(show));

		return v;
	}

	/**
	 * Builds the navigation tree containing all the items in the list and the
	 * files, HTML pages, links, folders contained recursively by those items
	 * until the specified depth. This method will change later
	 *
	 * @param items
	 * @param depth
	 * @throws DotDataException
	 */
	protected List<Object> buildNavigationTree(List items, int depth, User user) throws DotDataException {
		depth = depth - 1;
		int level = 0;
		List<Object> v = new ArrayList<Object>();
		InternalCounter counter = new FolderFactoryImpl().new InternalCounter();
		counter.setCounter(0);
		List<Integer> ids = new ArrayList<Integer>();
		List l = buildNavigationTree(items, ids, level, counter, depth, user);
		StringBuffer sb = new StringBuffer("");
		if (l != null && l.size() > 0) {
			sb = (StringBuffer) l.get(0);
			sb.append("<script language='javascript'>\n");
			for (int i = ids.size() - 1; i >= 0; i--) {
				int internalCounter = (Integer) ids.get(i);
				String id = "list" + internalCounter;
				String className = "class" + internalCounter;
				String sortCreate = "Sortable.create(\"" + id + "\",{dropOnEmpty:true,tree:true,constraint:false,only:\"" + className
						+ "\"});\n";
				sb.append(sortCreate);
			}

			sb.append("\n");
			sb.append("function serialize(){\n");
			sb.append("var values = \"\";\n");
			for (int i = 0; i < ids.size(); i++) {
				int internalCounter = (Integer) ids.get(i);
				String id = "list" + internalCounter;
				String sortCreate = "values += \"&\" + Sortable.serialize('" + id + "');\n";
				sb.append(sortCreate);
			}
			sb.append("return values;\n");
			sb.append("}\n");

			sb.append("</script>\n");

			sb.append("<style>\n");
			for (int i = 0; i < ids.size(); i++) {
				int internalCounter = (Integer) ids.get(i);
				String className = "class" + internalCounter;
				String style = "li." + className + " { cursor: move;}\n";
				sb.append(style);
			}
			sb.append("</style>\n");
		}
		v.add(sb.toString());
		if (l != null && l.size() > 0) {
			v.add(l.get(1));
		} else {
			v.add(new Boolean(false));
		}

		return v;
	}

	/**
	 * Builds the navigation tree containing all the items and the files, HTML
	 * pages, links, folders contained recursively by those items in the list
	 * until the specified depth. This method will change later
	 *
	 * @param items
	 * @param ids
	 * @param level
	 * @param counter
	 * @param depth
	 * @throws DotDataException
	 */
	protected List buildNavigationTree(List items, List<Integer> ids, int level, InternalCounter counter, int depth, User user)
			throws DotDataException {
		boolean show = true;
		StringBuffer sb = new StringBuffer();
		List v = new ArrayList<Object>();
		int internalCounter = counter.getCounter();
		String className = "class" + internalCounter;
		String id = "list" + internalCounter;
		ids.add(internalCounter);
		counter.setCounter(++internalCounter);

		sb.append("<ul id='" + id + "' >\n");
		if (items != null) {
			Iterator itemsIter = items.iterator();
			while (itemsIter.hasNext()) {
				Permissionable item = (Permissionable) itemsIter.next();
				String title = "";
				String inode = "";
				if (item instanceof Folder) {
					Folder folder = ((Folder) item);
					title = folder.getTitle();
					title = retrieveTitle(title, user);
					inode = folder.getInode();
					if (folder.isShowOnMenu()) {
						if (!APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
							show = false;
						}
						if (APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false)) {

							sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >\n" + title + "\n");
							List childs = getMenuItems(folder);
							if (childs.size() > 0) {
								int nextLevel = level + 1;
								if (depth > 0) {
									List<Object> l = getNavigationTree(childs, ids, nextLevel, counter, depth, user);
									if (show) {
										show = ((Boolean) l.get(1)).booleanValue();
									}
									sb.append((StringBuffer) (l.get(0)));
								}
							}
							sb.append("</li>\n");
						}
					}
				} else {
					if(item instanceof FileAsset){
						FileAsset fa =(FileAsset)item;
						title = fa.getTitle();
						title = retrieveTitle(title, user);
						inode = fa.getInode();
						if (fa.isShowOnMenu()) {
							if (!APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
								show = false;
							}
							if (APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_READ, user, false)) {
								sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
							}
						}
					}else{
						WebAsset asset = ((WebAsset) item);
						title = asset.getTitle();
						title = retrieveTitle(title, user);
						inode = asset.getInode();
						if (asset.isShowOnMenu()) {
							if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
								show = false;
							}
							if (APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ, user, false)) {
								sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
							}
						}
					}
				}
			}
		}

		sb.append("</ul>\n");
		v.add(sb);
		v.add(new Boolean(show));

		return v;
	}

	private String retrieveTitle(String title, User user) {
		try {
			String regularExpressionString = "(.*)\\$glossary.get\\('(.*)'\\)(.*)";
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regularExpressionString);
			Matcher matcher = pattern.matcher(title);
			if (matcher.matches()) {
				String tempTitle = matcher.group(2);
				tempTitle = matcher.group(1) + LanguageUtil.get(user, tempTitle) + matcher.group(3);
				title = tempTitle;
			}
		} catch (Exception ex) {
			String message = ex.getMessage();
		} finally {
			return title;
		}
	}

	private boolean isOpenNode(String[] openNodes, String node) {
		boolean returnValue = false;
		for (String actualNode : openNodes) {
			if (actualNode.equals(node)) {
				returnValue = true;
				break;
			}
		}
		return returnValue;
	}

	// http://jira.dotmarketing.net/browse/DOTCMS-3232
	protected Folder findSystemFolder() throws DotDataException {
		Folder folder = new Folder();
		folder = fc.getFolder(FolderAPI.SYSTEM_FOLDER);
		if (folder!=null && folder.getInode().equalsIgnoreCase(FolderAPI.SYSTEM_FOLDER)) {
			return folder;
		} else {
			folder = find(FolderAPI.SYSTEM_FOLDER);
		}
		if (UtilMethods.isSet(folder.getInode()) && folder.getInode().equalsIgnoreCase(FolderAPI.SYSTEM_FOLDER)) {
			fc.addFolder(folder,APILocator.getIdentifierAPI().find(folder.getIdentifier()));
			return folder;
		} else {
			DotConnect dc = new DotConnect();
			Folder folder1 = new Folder();
			String hostInode = "";
			folder1.setInode(FolderAPI.SYSTEM_FOLDER);
			folder1.setName("system folder");
			folder1.setTitle("System folder");
			try {
				hostInode = APILocator.getHostAPI().findSystemHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier();
			} catch (DotSecurityException e) {
				Logger.error(FolderFactoryImpl.class, e.getMessage(), e);
				throw new DotDataException(e.getMessage(), e);
			}
			folder1.setFilesMasks("");
			folder1.setSortOrder(0);
			folder1.setShowOnMenu(false);

			String IdentifierQuery = "INSERT INTO IDENTIFIER(ID,PARENT_PATH,ASSET_NAME,HOST_INODE,ASSET_TYPE) VALUES(?,?,?,?,?)";
			String uuid = UUIDGenerator.generateUuid();
			dc.setSQL(IdentifierQuery);
			dc.addParam(uuid);
			dc.addParam("/System folder");
			dc.addParam(folder1.getName());
			dc.addParam(hostInode);
			dc.addParam(folder1.getType());
			dc.loadResult();

			String InodeQuery = "INSERT INTO INODE(INODE, OWNER, IDATE, TYPE) VALUES (?,null,?,?)";
			dc.setSQL(InodeQuery);
			dc.addParam(folder1.getInode());
			dc.addParam(folder1.getiDate());
			dc.addParam(folder1.getType());
			dc.loadResult();
			String hostQuery = "INSERT INTO FOLDER(INODE, NAME,TITLE, SHOW_ON_MENU, SORT_ORDER,FILES_MASKS,IDENTIFIER) VALUES (?,?,?,?,?,?,?,?)";
			dc.setSQL(hostQuery);
			dc.addParam(folder1.getInode());
			dc.addParam(folder1.getName());
			dc.addParam(folder1.getTitle());
			dc.addParam(folder1.isShowOnMenu());
			dc.addParam(folder1.getSortOrder());
			dc.addParam(folder1.getFilesMasks());
			dc.addParam(uuid);
			dc.loadResult();
			fc.addFolder(folder1,APILocator.getIdentifierAPI().find(folder1.getIdentifier()));
			return folder1;
		}
	}

	@SuppressWarnings("unchecked")
	protected List<Folder> findFoldersByHost(Host host) throws DotHibernateException {
		HibernateUtil dh = new HibernateUtil(Folder.class);
		dh.setSQLQuery("SELECT {folder.*} from folder folder,identifier ident, inode folder_1_ where folder_1_.inode = folder.inode "
			    + "and folder.identifier = ident.id and ident.host_inode = ? and ident.parent_path='/' order by folder.title");
		dh.setParam(host.getIdentifier());
		List<Folder> folderList=dh.list();
		Collections.sort(folderList,new Comparator<Folder>() {
		    public int compare(Folder o1, Folder o2) {
		        return o1.getTitle().compareToIgnoreCase(o2.getTitle());
		    }
        });
		return folderList;
	}

	protected Identifier createIdentifierForFolder(Folder folder, String parentPath) throws DotDataException {
		Identifier identifier = new Identifier();
		if (InodeUtils.isSet(folder.getIdentifier())) {
			try {
				identifier = APILocator.getIdentifierAPI().find(folder.getIdentifier());
			} catch (Exception e) {
				Logger.error(FolderFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
		identifier.setAssetType(folder.getType());
		identifier.setAssetName(folder.getName());
		identifier.setHostId(folder.getHostId());
		if(InodeUtils.isSet(folder.getIdentifier())){
			identifier.setParentPath(APILocator.getIdentifierAPI().find(folder).getParentPath()+ folder.getName() + "/");
		}else{
			if(parentPath==null)
				identifier.setParentPath("/");
			else
				identifier.setParentPath(parentPath);
		}
		APILocator.getIdentifierAPI().save(identifier);
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(folder);

		return identifier;
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz) throws DotStateException, DotDataException {
		return getChildrenClass(parent, clazz, null, null, 0, 1000);
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond) throws DotStateException, DotDataException {
		return getChildrenClass(parent, clazz, cond, null, 0, 1000);
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition condition, String orderby) throws DotStateException,
			DotDataException {
		return getChildrenClass(parent, clazz, condition, orderby, 0, 1000);
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond, String orderBy, int offset, int limit)
			throws DotStateException, DotDataException {

		Identifier id = APILocator.getIdentifierAPI().find(parent.getIdentifier());

		String tableName = null;

		try {
			Object obj;
			obj = clazz.newInstance();

			if (obj instanceof Treeable) {
				tableName = ((Treeable) obj).getType();
			} else {
				throw new DotStateException("Unable to getType for child asset");
			}
		} catch (InstantiationException e) {
			throw new DotStateException("Unable to getType for child asset");
		} catch (IllegalAccessException e) {
			throw new DotStateException("Unable to getType for child asset");
		}

		String versionTable=UtilMethods.getVersionInfoTableName(tableName);

		HibernateUtil dh = new HibernateUtil(clazz);
		String sql = "SELECT {" + tableName + ".*} " + " from " + tableName + " " + tableName + ",  inode " + tableName
				+ "_1_, identifier " + tableName + "_2_ ";

		if(cond!=null && versionTable!=null && (cond.deleted!=null || cond.working!=null || cond.live!=null))
		    sql+=", "+versionTable;

		sql+=" where " + tableName + "_2_.parent_path = ? " + " and " + tableName
				+ ".identifier = " + tableName + "_2_.id " + " and " + tableName + "_1_.inode = " + tableName + ".inode " + " and ";

		if(cond!=null && cond.deleted!=null)
		    if(versionTable!=null)
		        sql+=versionTable+".deleted="+((cond.deleted)?DbConnectionFactory.getDBTrue():DbConnectionFactory.getDBFalse())+" and ";
		    else
		        sql+=" deleted="+((cond.deleted)?DbConnectionFactory.getDBTrue():DbConnectionFactory.getDBFalse())+" and ";

		if(cond!=null && cond.working!=null)
		    if(versionTable!=null)
		        sql+=versionTable+".working_inode"+(cond.working ? "=":"<>")+tableName+"_1_.inode and ";
		    else
		        sql+=" working="+((cond.working)?DbConnectionFactory.getDBTrue():DbConnectionFactory.getDBFalse())+" and ";

		if(cond!=null && cond.live!=null)
		    if(versionTable!=null)
                sql+=versionTable+".live_inode"+(cond.live ? "=":"<>")+tableName+"_1_.inode and ";
            else
                sql+=" live="+((cond.live)?DbConnectionFactory.getDBTrue():DbConnectionFactory.getDBFalse())+" and ";

		sql+= tableName + "_1_.type = '" + tableName + "' " + " and " + tableName + "_2_.host_inode = ? ";

		if(cond!=null && cond.showOnMenu!=null)
		    sql+=" and "+tableName+".show_on_menu="+(cond.showOnMenu ? DbConnectionFactory.getDBTrue():DbConnectionFactory.getDBFalse());

		if (orderBy != null) {
			sql = sql + " order by " + orderBy;
		}
		dh.setSQLQuery(sql);
		dh.setFirstResult(offset);
		dh.setMaxResults(limit);
		dh.setParam(id.getURI()+'/');
		dh.setParam(id.getHostId());
		return dh.list();

	}


	private class InternalCounter
	{
		private int counter;

		public int getCounter()
		{
			return counter;
		}

		public void setCounter(int counter)
		{
			this.counter = counter;
		}
	}


	@Override
	protected void save(Folder folderInode) throws DotDataException {
		HibernateUtil.saveOrUpdate(folderInode);
	}
}
