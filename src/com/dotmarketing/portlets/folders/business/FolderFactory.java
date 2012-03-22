package com.dotmarketing.portlets.folders.business;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

/**
 * 
 * @author maria
 */
public abstract class FolderFactory {


	abstract void delete(Folder f) throws DotDataException;

	/*
	 * abstract boolean existsFolder(long folderInode) { return
	 * existsFolder(Long.toString(folderInode)); }
	 */

	abstract Folder find(String folderInode) throws  DotDataException;

	abstract void save(Folder folderInode) throws DotDataException;


	abstract boolean exists(String folderInode) throws DotDataException;
	
	protected boolean isChildFolder(Folder folder1,Folder folder2) throws DotIdentifierStateException, DotDataException, DotSecurityException{
		return false;
	}
	
	protected boolean matchFilter(Folder folder, String fileName){
		return false;
	}
	
	protected List<Treeable> getChildrenClass(Folder parent, Class clazz) throws DotStateException, DotDataException{
		return null;
	}

	protected List<Folder> getSubFoldersTitleSort(Folder folder) throws DotHibernateException, DotStateException, DotDataException {
		return null;
	}
	protected Folder findFolderByPath(String path, Host host) throws DotDataException {
		return null;
	}

	protected List<Folder> getSubFolders(Folder folder) throws DotHibernateException, DotStateException, DotDataException {
		return null;
	}
	
	protected Folder findSystemFolder() throws DotDataException {
		return null;
	}
	
	protected List<Inode> getMenuItems(Folder folder) throws DotStateException, DotDataException{
		return null;
	}
	
	abstract void copy(Folder folder, Host destination) throws DotDataException, DotSecurityException, DotStateException, IOException; 
	
	abstract void copy(Folder folder, Folder destination) throws DotDataException, DotStateException, DotSecurityException, IOException;
	
    abstract boolean move(Folder folder, Host destination) throws DotDataException, DotSecurityException; 
	
	abstract boolean move(Folder folder, Folder destination) throws DotDataException, DotSecurityException;
	
	protected boolean renameFolder(Folder folder, String newName, User ser, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		return false;
	}
	
	protected List getMenuItems(Folder folder, int orderDirection) throws DotStateException, DotDataException{
		return null;
	}
	
	protected List<Object> buildNavigationTree(List items, int depth, User user) throws DotDataException {
		return null;
	}
	
	protected List getMenuItems(Host host) throws DotStateException, DotDataException, DotSecurityException {
		return null;
	}
	
	abstract void updateMovedFolderAssets(Folder folder) throws DotDataException, DotStateException, DotSecurityException;
	
	protected List<Folder> getFoldersByParent(Folder folder, User user, boolean respectFrontendRoles) throws DotDataException{
		return null;
	}
	protected List<Folder> findFoldersByHost(Host host) throws DotHibernateException{
		return null;
	}
	protected List<Folder> findSubFolders(Host host, boolean showOnMenu)throws DotHibernateException  {
		return null;
	}
	protected List<Folder> findSubFolders(Folder folder, boolean showOnMenu) throws DotStateException, DotDataException{
	    return null;	
	}
	
	protected List<String> getEntriesTree(Folder mainFolder, String openNodes, String view, String content, String structureInode, Locale locale,
			TimeZone timeZone, Role[] roles, boolean isAdminUser, User user) throws DotStateException, DotDataException,DotSecurityException {
		return null;
	}
	
	protected List<String> getFolderTree(String openNodes, String view, String content, String structureInode, Locale locale, TimeZone timeZone,
			Role[] roles, boolean isAdminUser, User user) throws DotStateException, DotDataException, DotSecurityException {
		return null;
	}
	
	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond, String orderby) throws DotStateException,DotDataException{
		return null;
	}
	
	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond) throws DotStateException,DotDataException{
		return null;
	}
	
	abstract boolean updateIdentifierUrl(Folder folder, Folder destination) throws DotDataException, DotSecurityException;

}
