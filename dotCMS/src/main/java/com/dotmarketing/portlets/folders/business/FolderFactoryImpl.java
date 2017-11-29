package com.dotmarketing.portlets.folders.business;
// 1212

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.oro.text.regex.Pattern;
import com.dotcms.repackage.org.apache.oro.text.regex.Perl5Compiler;
import com.dotcms.repackage.org.apache.oro.text.regex.Perl5Matcher;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.AssetsComparator;
import com.dotmarketing.util.ConvertToPOJOUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 *
 * @author maria 2323
 */
public class FolderFactoryImpl extends FolderFactory {

	private FolderCache fc = CacheLocator.getFolderCache();

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
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(f);
	}

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

	/**
	 *
	 * @param folder
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @deprecated use {@link #getSubFoldersTitleSort(Folder)}
	 */
	@SuppressWarnings("unchecked")
	@Override
	@Deprecated
	protected java.util.List<Folder> getSubFolders(Folder folder) throws DotStateException, DotDataException {

		return getSubFoldersTitleSort(folder);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected java.util.List<Folder> getSubFoldersTitleSort(Folder folder) throws DotDataException  {
		Identifier id = APILocator.getIdentifierAPI().find(folder);

		return getSubFolders(null, id.getPath(), id.getHostId(), "lower(folder.title)");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Folder> findSubFolders(final Folder folder, Boolean showOnMenu)
			throws DotStateException, DotDataException {
		Identifier id = APILocator.getIdentifierAPI().find(folder);
		return getSubFolders(showOnMenu, id.getPath(), id.getHostId(), null);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Folder> findSubFolders(final Host host, Boolean showOnMenu)
			throws DotHibernateException {

		return getSubFolders(showOnMenu, "/", host.getIdentifier(), null);
	}

	private List<Folder> getSubFolders(Boolean showOnMenu, String path, final String hostId, String order) {
		DotConnect dc    = new DotConnect();
		String condition = "";
		StringBuilder orderBy   = new StringBuilder(" order by ");
		StringBuilder query = new StringBuilder();

		if (UtilMethods.isSet(showOnMenu)) {
			condition = "show_on_menu = " + (showOnMenu ? DbConnectionFactory
					.getDBTrue() : DbConnectionFactory.getDBFalse());
		}

		if (!UtilMethods.isSet(order)){
			orderBy.append("sort_order, name");
		}else{
			orderBy.append(order);
		}

		query.append("SELECT folder.* from folder folder, inode folder_1_, identifier identifier ").
				append("where folder.identifier = identifier.id and ").
				append("folder_1_.type = 'folder' and folder_1_.inode = folder.inode and ").
				append("identifier.parent_path = ? and identifier.host_inode = ? ");

		query.append((!condition.isEmpty()?" and " + condition:condition) + orderBy);

		dc.setSQL(query.toString());
		dc.addParam(path);
		dc.addParam(hostId);

		try{
			return ConvertToPOJOUtil.convertDotConnectMapToFolder(dc.loadResults());
		}catch(ParseException | DotDataException e){
			Logger.error(this, e.getMessage(), e);
		}

		return Collections.emptyList();
	}

	@Override
	protected Folder findFolderByPath(String path, Host host) throws DotDataException {

		String originalPath = path;
		Folder folder;
		List<Folder> result;

		if(host == null){
			return null;
		}

		if(path.equals("/") || path.equals("/System folder")) {
			folder = fc.getFolderByPathAndHost(path, APILocator.getHostAPI().findSystemHost());
		} else{
			folder = fc.getFolderByPathAndHost(path, host);
		}

		if(folder == null){
			String parentPath;
			String assetName;
			String hostId;

			try{
				if(path.equals("/") || path.equals("/System folder")) {
					parentPath = "/System folder";
					assetName = "system folder";
					hostId = "SYSTEM_HOST";
				}
				else {
					// trailing / is removed
					if (path.endsWith("/")){
						path = path.substring(0, path.length()-1);
					}
					// split path into parent and asset name
					int idx = path.lastIndexOf('/');
					parentPath = path.substring(0,idx+1);
					assetName = path.substring(idx+1);
					hostId = host.getIdentifier();
				}

				DotConnect dc = new DotConnect();
				dc.setSQL("select folder.* from " + Type.FOLDER.getTableName() + " folder, inode folder_1_, identifier  where asset_name = ? and parent_path = ? and "
						+ "folder_1_.type = 'folder' and folder.inode = folder_1_.inode and folder.identifier = identifier.id and host_inode = ?");
				dc.addParam(assetName.toLowerCase());
				dc.addParam(parentPath.toLowerCase());
				dc.addParam(hostId);

				result = ConvertToPOJOUtil.convertDotConnectMapToFolder(dc.loadResults());

				if (result != null && !result.isEmpty()){
					folder = result.get(0);
				}else{
					folder = new Folder();
				}

				// if it is found add it to folder cache
				if(UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
					Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
					fc.addFolder(folder, id);
				} else {
					String parentFolder = originalPath;

					//If the path ends with a / we assume that the last part of the path is the folder.
					//We just have to remove tha last / and substring from the last / remaining.
					//For example: /application/themes/ => themes
					if(parentFolder.endsWith("/")){
						parentFolder = parentFolder.substring(0, parentFolder.length()-1);
						parentPath = parentFolder.substring(0, parentFolder.lastIndexOf("/")+1);

						if(parentFolder.contains("/")){
							parentFolder = parentFolder.substring(parentFolder.lastIndexOf("/")+1);
						}

						//If the path doesn't end in / we assume the last part is a page or something else.
						//We have to remove the last part plus the / and then remove the path from 0 to /
						//For example: /application/themes/example-page => themes
					} else {
						if (parentFolder.contains("/")) {
							parentFolder = parentFolder.substring(0, parentFolder.lastIndexOf("/"));
							parentPath = parentFolder.substring(0, parentFolder.lastIndexOf("/")+1);
							parentFolder = parentFolder.substring(parentFolder.lastIndexOf("/")+1);
						}
					}

					dc = new DotConnect();
					dc.setSQL("select folder.* from " + Type.FOLDER.getTableName() + " folder, inode folder_1_, identifier"
							+ " where asset_name = ?"
							+ " and parent_path = ?"
							+ " and folder_1_.type = 'folder'"
							+ " and folder.inode = folder_1_.inode"
							+ " and folder.identifier = identifier.id"
							+ " and host_inode = ?");
					dc.addParam(parentFolder.toLowerCase());
					dc.addParam(parentPath.toLowerCase());
					dc.addParam(hostId);

					result = ConvertToPOJOUtil.convertDotConnectMapToFolder(dc.loadResults());

					if (result != null && !result.isEmpty()){
						folder = result.get(0);
					}

					// if it is found add it to folder cache
					if(UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
						Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
						fc.addFolder(folder, id);
					}
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
		List<Folder> subFolders = APILocator.getFolderAPI().findSubFolders(host, APILocator.getUserAPI().getSystemUser(), false);
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

			// gets all files for this folder
			List filesListSubChildren = new ArrayList();


			List<FileAsset> fileAssets = null;
			try {
				fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, "",true,APILocator.getUserAPI().getSystemUser(), false);
				for(FileAsset fileAsset : fileAssets) {

					if(fileAsset.isShowOnMenu() && !fileAsset.isDeleted()){

						filesListSubChildren.add(fileAsset);
					}
				}
			} catch (DotSecurityException e) {}
			
			try {
                for(IHTMLPage page : APILocator.getHTMLPageAssetAPI().getHTMLPages(folder, true, false, APILocator.getUserAPI().getSystemUser(), false)) {
                    if(page.isShowOnMenu()) {
                        filesListSubChildren.add(page);
                    }
                }
                
            } catch (DotSecurityException e) {}

			// gets all subitems
			menuList.addAll(subFolders);
			menuList.addAll(linksListSubChildren);
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
	    List<Folder> dummy=new ArrayList<Folder>();
	    dummy.add((Folder)inode);
	    return getMenuItems(dummy, orderDirection);
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

    @SuppressWarnings ("unchecked")
    private void copy ( Folder folder, Host destination, Hashtable copiedObjects ) throws DotDataException, DotSecurityException, DotStateException, IOException {

        boolean rename = APILocator.getHostAPI().doesHostContainsFolder( destination, folder.getName() );

        Folder newFolder = new Folder();
        newFolder.copy( folder );
        newFolder.setName( folder.getName() );
        while ( rename ) {
            newFolder.setName( newFolder.getName() + "_copy" );
            rename = APILocator.getHostAPI().doesHostContainsFolder( destination, newFolder.getName() );
        }

        newFolder.setHostId( destination.getIdentifier() );

        Identifier newFolderId = createIdentifierForFolder( newFolder, null );
        newFolder.setIdentifier( newFolderId.getId() );
        newFolder.setModDate(new Date());

        save( newFolder );

        saveCopiedFolder( folder, newFolder, copiedObjects );
    }

    @SuppressWarnings ("unchecked")
    private void copy ( Folder folder, Folder destination, Hashtable copiedObjects ) throws DotDataException, DotStateException, DotSecurityException, IOException {

        boolean rename = folderContains( folder.getName(), destination );

        Folder newFolder = new Folder();
        newFolder.copy( folder );
        newFolder.setName( folder.getName() );
        while ( rename ) {
            newFolder.setName( newFolder.getName() + "_copy" );
            rename = folderContains( newFolder.getName(), (Folder) destination );
        }

        newFolder.setHostId( destination.getHostId() );
        Identifier parentId = APILocator.getIdentifierAPI().find( destination.getIdentifier() );
        Identifier newFolderId = createIdentifierForFolder( newFolder, parentId.getPath() );
        newFolder.setIdentifier( newFolderId.getId() );
        newFolder.setModDate(new Date());

        save( newFolder );

        saveCopiedFolder( folder, newFolder, copiedObjects );
    }

	private void saveCopiedFolder(Folder source, Folder newFolder, Hashtable copiedObjects) throws DotDataException, DotStateException, DotSecurityException, IOException {
		User systemUser = APILocator.getUserAPI().getSystemUser();

		if (copiedObjects == null)
			copiedObjects = new Hashtable();

		// Copying folder permissions
		APILocator.getPermissionAPI().copyPermissions(source, newFolder);

		// Copying children html pages
		Map<String, IHTMLPage[]> pagesCopied;
		if (copiedObjects.get("HTMLPages") == null) {
			pagesCopied = new HashMap<String, IHTMLPage[]>();
			copiedObjects.put("HTMLPages", pagesCopied);
		} else {
			pagesCopied = (Map<String, IHTMLPage[]>) copiedObjects.get("HTMLPages");
		}

		// Copying Files
		Map<String, IFileAsset[]> filesCopied;
		if (copiedObjects.get("Files") == null) {
			filesCopied = new HashMap<String, IFileAsset[]>();
			copiedObjects.put("Files", filesCopied);
		} else {
			filesCopied = (Map<String, IFileAsset[]>) copiedObjects.get("Files");
		}

		//Content Files
		List<FileAsset> faConts = APILocator.getFileAssetAPI().findFileAssetsByFolder(source, APILocator.getUserAPI().getSystemUser(), false);
		for(FileAsset fa : faConts){
			if(fa.isWorking() && !fa.isArchived()){
				Contentlet cont = APILocator.getContentletAPI().find(fa.getInode(), APILocator.getUserAPI().getSystemUser(), false);
				APILocator.getContentletAPI().copyContentlet(cont, newFolder, APILocator.getUserAPI().getSystemUser(), false);
				filesCopied.put(cont.getInode(), new IFileAsset[] {fa , APILocator.getFileAssetAPI().fromContentlet(cont)});
			}
		}
		
		//Content Pages
		Set<IHTMLPage> pageAssetList=new HashSet<IHTMLPage>();
		pageAssetList.addAll(APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages(source, APILocator.getUserAPI().getSystemUser(), false));
		pageAssetList.addAll(APILocator.getHTMLPageAssetAPI().getLiveHTMLPages(source, APILocator.getUserAPI().getSystemUser(), false));
		for(IHTMLPage page : pageAssetList) {
		    Contentlet cont = APILocator.getContentletAPI().find(page.getInode(), APILocator.getUserAPI().getSystemUser(), false);
            Contentlet newContent = APILocator.getContentletAPI().copyContentlet(cont, newFolder, APILocator.getUserAPI().getSystemUser(), false);
            List<MultiTree> pageContents = MultiTreeFactory.getMultiTree(cont.getIdentifier());
            for(MultiTree m : pageContents){
            	MultiTree mt = new MultiTree(newContent.getIdentifier(), m.getParent2(), m.getChild());
            	MultiTreeFactory.saveMultiTree(mt);
            }
            pagesCopied.put(cont.getInode(), new IHTMLPage[] {page , APILocator.getHTMLPageAssetAPI().fromContentlet(cont)});
		}
		

		// issues/1736
		APILocator.getContentletAPI().refreshContentUnderFolder(newFolder);

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
		Identifier folderId = identAPI.find(folder.getIdentifier());

        //Clean up the cache
        if ( folder.isShowOnMenu() ) {
            CacheLocator.getNavToolCache().removeNav( folder.getHostId(), folder.getInode() );
        }
        CacheLocator.getNavToolCache().removeNavByPath( folderId.getHostId(), folderId.getParentPath() );
        fc.removeFolder( folder, folderId );
		CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(folderId.getId());

		User systemUser = APILocator.getUserAPI().getSystemUser();
		boolean contains = false;
		String newParentPath;
		String newParentHostId;
		if (destination instanceof Folder) {
			contains = folderContains(folder.getName(), (Folder) destination);
			Identifier destinationId = identAPI.find(((Folder) destination).getIdentifier());
			newParentPath = destinationId.getPath();
			newParentHostId = destinationId.getHostId();
			if(!contains)
			    CacheLocator.getNavToolCache().removeNavByPath(destinationId.getHostId(), destinationId.getPath());
		} else {
			contains = APILocator.getHostAPI().doesHostContainsFolder((Host) destination, folder.getName());
			newParentPath = "/";
			newParentHostId = ((Host)destination).getIdentifier();
			if(!contains)
			    CacheLocator.getNavToolCache().removeNav(newParentHostId, FolderAPI.SYSTEM_FOLDER);
		}
		if (contains)
			return false;

		List<Folder> subFolders = getSubFoldersTitleSort(folder);
		List links = getChildrenClass(folder, Link.class);
		List<Contentlet> contentlets = APILocator.getContentletAPI().findContentletsByFolder(folder, systemUser, false);


		folderId.setParentPath(newParentPath);
		folderId.setHostId(newParentHostId);
		identAPI.save(folderId);

		for (Object link : links) {
			if (((Link) link).isWorking()) {
				LinkFactory.moveLink((Link) link, folder);
			}
		}

		for(Contentlet cont : contentlets){
		    if(cont.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET) {
		        APILocator.getFileAssetAPI().moveFile(cont, folder, systemUser, false);
		    }
		    else {
    			boolean isLive = cont.isLive();
    			cont.setFolder(folder.getInode());
    			cont.setInode(null);
    			Contentlet newCont = APILocator.getContentletAPI().checkin(cont, systemUser, false);
    			if(isLive){
    				APILocator.getContentletAPI().publish(newCont, systemUser, false);
    			}
		    }
		}

		for(Folder subFolder : subFolders){
			move(subFolder, (Object)folder);
		}

		CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(folderId.getId());


		
		folder.setModDate(new Date());
		save(folder);

		return true;
	}

	/***
	 * This method update recursively the inner folders of the specified folder
	 *
	 * @param oldFolder
	 * @param newFolder
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

			List links = getChildrenClass(nextFolder,Link.class);

			updateMovedFolderAssets(nextFolder, nextNewFolder, links);
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

		List<FileAsset> fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder(theFolder, APILocator.getUserAPI().getSystemUser(), false);
		for(FileAsset fa : fileAssets){
			Identifier identifier = APILocator.getIdentifierAPI().find(fa);
			Contentlet fileAssetCont = APILocator.getContentletAPI().find(fa.getInode(), APILocator.getUserAPI().getSystemUser(), false);


			if (fileAssetCont.isWorking()) {
				// gets identifier for this webasset and changes the uri and
				// persists it
				identifier.setHostId(newHost.getIdentifier());
				Identifier folderIdentifier  = APILocator.getIdentifierAPI().find(theFolder);
				identifier.setParentPath(folderIdentifier.getPath());
				APILocator.getIdentifierAPI().save(identifier);
				CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(identifier.getId());
			}

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
				CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(identifier.getId());
			}

		}
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
	private void updateMovedFolderAssets(Folder oldFolder, Folder newFolder, List<Link> links)
			throws DotDataException, DotStateException, DotSecurityException {

		User systemUser;

		try {
			systemUser = APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			Logger.error(FolderFactoryImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
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
			}
		}
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
		Identifier ident = APILocator.getIdentifierAPI().loadFromDb(folder.getIdentifier());
		StringBuilder newPath = new StringBuilder(ident.getParentPath()).append(newName);
		if(!newName.endsWith("/")) newPath.append("/"); // Folders must end with '/'
		Host host = APILocator.getHostAPI().find(folder.getHostId(),user,respectFrontEndPermissions);
		Folder nFolder = findFolderByPath(newPath.toString(), host);
		if(UtilMethods.isSet(nFolder.getInode()) && !folder.getIdentifier().equals(nFolder.getIdentifier()))
			return false;

		CacheLocator.getIdentifierCache().removeFromCacheByInode(folder.getInode());
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(folder);
		CacheLocator.getFolderCache().removeFolder(folder, ident);

		final ArrayList<String> childIdents=new ArrayList<String>();
		DotConnect dc=new DotConnect();
		dc.setSQL("select id from identifier where parent_path like ? and host_inode=?");
		dc.addParam(ident.getPath()+"%");
		dc.addParam(ident.getHostId());
		for(Map<String,Object> rr : (List<Map<String,Object>>)dc.loadResults()) {
		    childIdents.add((String)rr.get("id"));
		}
		HibernateUtil.addCommitListener(new FlushCacheRunnable() {
            public void run() {
                for(String id : childIdents) {
                    CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(id);
                }
            }
		});

        Folder ff=(Folder) HibernateUtil.load(Folder.class, folder.getInode());
		ff.setName(newName.toLowerCase());
		ff.setTitle(newName);
		ff.setModDate(new Date());

		save(ff);

		HibernateUtil.getSession().clear();

        HibernateUtil.addCommitListener(new FlushCacheRunnable() {
            public void run() {
                APILocator.getContentletAPI().refreshContentUnderFolder(ff);
            }
        });

		return true;
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
			String uuid = FolderAPI.SYSTEM_FOLDER_ID;
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
			String hostQuery = "INSERT INTO " + Type.FOLDER.getTableName() + "(INODE, NAME,TITLE, SHOW_ON_MENU, SORT_ORDER,FILES_MASKS,IDENTIFIER) VALUES (?,?,?,?,?,?,?,?)";
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
		List<Folder> folderList = getSubFolders(null, "/", host.getIdentifier(), null);
		Collections.sort(folderList, (Folder folder1, Folder folder2) -> folder1.getName()
				.compareToIgnoreCase(folder2.getName()));
		return folderList;
	}

	@SuppressWarnings("unchecked")
	protected List<Folder> findThemesByHost(Host host) throws DotHibernateException {
		List<Folder> folderList = getSubFolders(null, "/application/themes/", host.getIdentifier(),
				null);
		Collections.sort(folderList, (Folder folder1, Folder folder2) -> folder1.getName()
				.compareToIgnoreCase(folder2.getName()));
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

    protected List<Treeable> getChildrenClass ( Host host, Class clazz ) throws DotStateException, DotDataException {
        Identifier identifier = APILocator.getIdentifierAPI().find( host.getIdentifier() );
        return getChildrenClass( identifier, clazz, null, null, 0, 1000 );
    }

    protected List<Treeable> getChildrenClass ( Host host, Class clazz, ChildrenCondition cond ) throws DotStateException, DotDataException {
        Identifier identifier = APILocator.getIdentifierAPI().find( host.getIdentifier() );
        return getChildrenClass( identifier, clazz, cond, null, 0, 1000 );
    }

    protected List<Treeable> getChildrenClass ( Folder parent, Class clazz, ChildrenCondition cond ) throws DotStateException, DotDataException {
        return getChildrenClass( parent, clazz, cond, null, 0, 1000 );
    }

    protected List<Treeable> getChildrenClass ( Folder parent, Class clazz, ChildrenCondition condition, String orderby ) throws DotStateException,
            DotDataException {
        return getChildrenClass( parent, clazz, condition, orderby, 0, 1000 );
    }

    protected List<Treeable> getChildrenClass ( Folder parent, Class clazz, ChildrenCondition cond, String orderBy, int offset, int limit ) throws DotStateException, DotDataException {
        Identifier identifier = APILocator.getIdentifierAPI().find( parent.getIdentifier() );
        return getChildrenClass( identifier, clazz, cond, orderBy, offset, limit );
    }

    protected List<Treeable> getChildrenClass ( Identifier identifier, Class clazz, ChildrenCondition cond, String orderBy, int offset, int limit ) throws DotStateException, DotDataException {

        String tableName;
		String type;

        try {
            Object obj;
            obj = clazz.newInstance();

            if ( obj instanceof Treeable ) {
				type = ((Treeable) obj).getType();
                tableName = Inode.Type.valueOf(type.toUpperCase()).getTableName();
            } else {
                throw new DotStateException( "Unable to getType for child asset" );
            }
        } catch ( InstantiationException e ) {
            throw new DotStateException( "Unable to getType for child asset" );
        } catch ( IllegalAccessException e ) {
            throw new DotStateException( "Unable to getType for child asset" );
        }

        String versionTable = Inode.Type.valueOf(type.toUpperCase()).getVersionTableName();

        DotConnect dc = new DotConnect();
        String sql = "SELECT " + tableName + ".*" + " from " + tableName + " " + tableName + ",  inode " + tableName
                + "_1_, identifier " + tableName + "_2_ ";

        if ( cond != null && versionTable != null && (cond.deleted != null || cond.working != null || cond.live != null) )
            sql += ", " + versionTable;

        sql += " where " + tableName + "_2_.parent_path = ? " + " and " + tableName
                + ".identifier = " + tableName + "_2_.id " + " and " + tableName + "_1_.inode = " + tableName + ".inode " + " and ";

        if ( cond != null && cond.deleted != null )
            if ( versionTable != null )
                sql += versionTable + ".deleted=" + ((cond.deleted) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";
            else
                sql += " deleted=" + ((cond.deleted) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";

        if ( cond != null && cond.working != null )
            if ( versionTable != null )
                sql += versionTable + ".working_inode" + (cond.working ? "=" : "<>") + tableName + "_1_.inode and ";
            else
                sql += " working=" + ((cond.working) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";

        if ( cond != null && cond.live != null )
            if ( versionTable != null )
                sql += versionTable + ".live_inode" + (cond.live ? "=" : "<>") + tableName + "_1_.inode and ";
            else
                sql += " live=" + ((cond.live) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";

        sql += tableName + "_1_.type = '" + tableName + "' " + " and " + tableName + "_2_.host_inode = ? ";

        if ( cond != null && cond.showOnMenu != null )
            sql += " and " + tableName + ".show_on_menu=" + (cond.showOnMenu ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse());

        if ( orderBy != null ) {
            sql = sql + " order by " + orderBy;
        }

        dc.setSQL( sql );
        dc.setStartRow( offset );
        dc.setMaxRows( limit );
        if ( identifier.getHostId().equals( Host.SYSTEM_HOST ) ) {
            dc.addParam( "/" );
            dc.addParam( identifier.getId() );
        } else {
            dc.addParam( identifier.getURI() + "/" );
            dc.addParam( identifier.getHostId() );
        }

        try {
			return ConvertToPOJOUtil.convertDotConnectMapToPOJO(dc.loadResults(), clazz);
		}catch(Exception e){
        	Logger.warn(this, e.getMessage(), e);
		}

		return Collections.emptyList();
    }




	@Override
	protected void save(Folder folderInode) throws DotDataException {
		HibernateUtil.getSession().clear();
		HibernateUtil.saveOrUpdate(folderInode);
	}

	@Override
	protected void save(Folder folderInode, String existingId) throws DotDataException {
		if(existingId==null){
			Folder folderToSave = folderInode;
			if(UtilMethods.isSet(folderInode.getInode())) {
				folderToSave = (Folder) new HibernateUtil(Folder.class).load(folderInode.getInode());
				try{
					BeanUtils.copyProperties(folderToSave, folderInode);
				}
				catch (Exception e) {
					throw new DotDataException(e.getMessage(), e);
				}
			}
			HibernateUtil.saveOrUpdate(folderToSave);
			fc.removeFolder(folderToSave, APILocator.getIdentifierAPI().find(folderToSave.getIdentifier()));
		}else{
			folderInode.setInode(existingId);
			HibernateUtil.saveWithPrimaryKey(folderInode, existingId);
		}
	}
}
