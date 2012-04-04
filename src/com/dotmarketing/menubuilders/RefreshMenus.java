/*
 * Created on Mar 4, 2005
 *
 */
package com.dotmarketing.menubuilders;

import java.io.File;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.velocity.DotResourceCache;

/**
 * @author maria
 */
public class RefreshMenus {

	private static String velocityRootPath = ConfigUtils.getDynamicVelocityPath() + java.io.File.separator;
	private static String MENU_VTL_PATH = velocityRootPath + "menus" + java.io.File.separator;
	
	public static void deleteMenusOnFileSystemOnly(){
		java.io.File directory = new java.io.File(MENU_VTL_PATH);

		if (directory.isDirectory()) {
			//get all files for this directory
			java.io.File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				//deletes all files on the directory
				((java.io.File)files[i]).delete();
			}
		}
	}
	
	public static void deleteMenus() {
		java.io.File directory = new java.io.File(MENU_VTL_PATH);

		if (directory.isDirectory()) {
			//get all files for this directory
			java.io.File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				//deletes all files on the directory
				((java.io.File)files[i]).delete();
			}
		}
		//http://jira.dotmarketing.net/browse/DOTCMS-1873
		//To clear velocity cache
		//http://jira.dotmarketing.net/browse/DOTCMS-2435
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.clearMenuCache();
	}
	
	public static void deleteMenu(Host host) 
	{
		java.io.File directory = new java.io.File(MENU_VTL_PATH);

		if (directory.isDirectory()) {
			//get all files for this directory
			java.io.File[] files = directory.listFiles();
			String hostId = host.getIdentifier();
			for (int i = 0; i < files.length; i++) 
			{
				File file = files[i];
				String fileName = file.getName();				
				if(fileName.startsWith(hostId))
				{
					file.delete();
				}		
			}
		}
		//http://jira.dotmarketing.net/browse/DOTCMS-1873
		//To clear velocity cache
		//http://jira.dotmarketing.net/browse/DOTCMS-2435
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.clearMenuCache();
	}

	public static void deleteMenu(Folder oldFolder,Folder newFolder)
	{
		deleteMenu(oldFolder);
		deleteMenu(newFolder);
	}

	public static void deleteMenu(WebAsset webAsset)
	{	
		Folder folder = new Folder();
		try {
			folder = APILocator.getFolderAPI().findParentFolder(webAsset, APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(RefreshMenus.class,e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		} 
		
		if(folder!=null && InodeUtils.isSet(folder.getInode()))
		{
			deleteMenu(folder);
		}
	}

	public static void deleteMenu(Folder folder)
	{				
		java.io.File directory = new java.io.File(MENU_VTL_PATH);

		//Delete the menu's file in the directory
		if(directory.isDirectory())
		{
			String folderInode = folder.getInode();
			Folder auxFolder = null;
			while(InodeUtils.isSet(folderInode))
			{
				//get all files for the menu directory
				java.io.File[] files = directory.listFiles();
				//String folderInodeString = Long.toString(folderInode);
				for (int i = 0; i < files.length; i++) 
				{
					File file = files[i];
					String fileName = file.getName();				
					if(fileName.startsWith(folderInode))
					{
						file.delete();
					}				
				}
				auxFolder = folder;
				try {
					folder = APILocator.getFolderAPI().findParentFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
				} catch (Exception e) {
					Logger.error(RefreshMenus.class,e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(),e);
				} 
				if(folder==null) folderInode=null;
				else folderInode = folder.getInode();
			}
			if(auxFolder != null)
			{
				HostAPI hostAPI = APILocator.getHostAPI();
				Host host;
				try {
					host = (Host) hostAPI.findParentHost(auxFolder, APILocator.getUserAPI().getSystemUser(), false);
				} catch (DotDataException e) {
					Logger.error(RefreshMenus.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				} catch (DotSecurityException e) {
					Logger.error(RefreshMenus.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				} 
				if(host == null){
					Logger.error(RefreshMenus.class, "Folder id :" + auxFolder.getInode() + " has no host");
					return;
				}
				java.io.File[] files = directory.listFiles();
				for (int i = 0; i < files.length; i++) 
				{
					File file = files[i];
					String fileName = file.getName();				
					if(fileName.startsWith(host.getIdentifier()))
					{
						file.delete();
					}				
				}				
			}			
		}
		//http://jira.dotmarketing.net/browse/DOTCMS-1873
		//To clear velocity cache
		//http://jira.dotmarketing.net/browse/DOTCMS-2435
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.clearMenuCache();
	}
}
