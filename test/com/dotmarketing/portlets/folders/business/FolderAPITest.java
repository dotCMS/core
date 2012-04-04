package com.dotmarketing.portlets.folders.business;

import java.util.ArrayList;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

public class FolderAPITest extends ServletTestCase {

	private FolderAPI folderAPI;
	
	public void setUp () {
		folderAPI = APILocator.getFolderAPI();
	}

	public void testFindSubFolders() {
		
		Host defaultHost;
		List<Folder> folders = new ArrayList<Folder>();
		try {
			defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
			folders = folderAPI.findSubFolders(defaultHost,APILocator.getUserAPI().getSystemUser(),false);
		} catch (DotDataException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
		assertEquals(8, folders.size());
		assertTrue(containsFolderByName(folders, "application"));
		assertTrue(containsFolderByName(folders, "blog"));
		assertTrue(containsFolderByName(folders, "calendar"));
		assertTrue(containsFolderByName(folders, "getting_started"));
		assertTrue(containsFolderByName(folders, "global"));
		assertTrue(containsFolderByName(folders, "home"));
		assertTrue(containsFolderByName(folders, "news"));
		assertTrue(containsFolderByName(folders, "store"));
		
	}
	
	public void testFindSubFoldersRecursivelyByFolder() {
		List<Folder> folders = new ArrayList<Folder>();
		try {
			User systemUser = APILocator.getUserAPI().getSystemUser();
			Host defaultHost = APILocator.getHostAPI().findDefaultHost(systemUser, false);
			Folder f = folderAPI.findFolderByPath("/getting_started",defaultHost,systemUser,false);
			folders = folderAPI.findSubFoldersRecursively(f,systemUser,false);
		} catch (Exception e) {
			
		} 
		assertEquals(4, folders.size());
		assertTrue(containsFolderByName(folders, "macros"));
		assertTrue(containsFolderByName(folders, "samples"));
		assertTrue(containsFolderByName(folders, "professional_support"));
		assertTrue(containsFolderByName(folders, "widgets"));
		
	}
	
	public void testFindSubFoldersRecursivelyByHost() {
		
		Host defaultHost;
		List<Folder> folders = new ArrayList<Folder>();
		try {
			defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
			folders = folderAPI.findSubFoldersRecursively(defaultHost,APILocator.getUserAPI().getSystemUser(),false);
		} catch (DotDataException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
		assertEquals(40, folders.size());
		assertTrue(containsFolderByName(folders, "macros"));
		assertTrue(containsFolderByName(folders, "swf"));
		assertTrue(containsFolderByName(folders, "products"));
		assertTrue(containsFolderByName(folders, "product-images"));
		assertTrue(containsFolderByName(folders, "samples"));
		assertTrue(containsFolderByName(folders, "professional_support"));
		assertTrue(containsFolderByName(folders, "blog"));
		assertTrue(containsFolderByName(folders, "widgets"));
	}

	private boolean containsFolderByName(List<Folder> folders, String name) {
		for(Folder f: folders) {
			if(f.getName().trim().equals(name.trim()))
				return true;
		}
		return false;
	}

}
