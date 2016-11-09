package com.dotmarketing.viewtools;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class FolderWebAPI implements ViewTool{

	private FolderAPI folderAPI;
	public void init(Object arg0) {
		folderAPI = APILocator.getFolderAPI();
	}
	
	public List<Inode> findMenuItems(String path, HttpServletRequest req) throws PortalException, SystemException, DotDataException, DotSecurityException{
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		return findMenuItems(folderAPI.findFolderByPath(path, host, APILocator.getUserAPI().getSystemUser(), true));
	}
	
	public List<Inode> findMenuItems(String folderInode) throws DotStateException, DotHibernateException, DotDataException, DotSecurityException{
		return findMenuItems(folderAPI.find(folderInode, APILocator.getUserAPI().getSystemUser(), true));
	}
	
	@Deprecated
	public List<Inode> findMenuItems(long folderInode) throws DotStateException, DotHibernateException, DotDataException, DotSecurityException{
		return findMenuItems(String.valueOf(folderInode));
	}
	
	public List<Inode> findMenuItems(Folder folder) throws DotStateException, DotDataException{
		return folderAPI.findMenuItems(folder, APILocator.getUserAPI().getSystemUser(), true);
	}
	
	public Folder findCurrentFolder(String path, Host host) throws DotStateException, DotDataException, DotSecurityException{
		return folderAPI.findFolderByPath(path, host.getIdentifier(), APILocator.getUserAPI().getSystemUser(), true);
	}
}
