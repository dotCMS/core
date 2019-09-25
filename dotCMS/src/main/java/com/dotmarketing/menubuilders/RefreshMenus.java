/*
 * Created on Mar 4, 2005
 *
 */
package com.dotmarketing.menubuilders;



import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

@Deprecated
public class RefreshMenus {




	public static boolean shouldRefreshMenus(IFileAsset oldFile, IFileAsset newFile, boolean isNew){
	    if(isNew || oldFile==null || newFile==null) {
	        return true;
	    }
	    return (isNew || oldFile.isShowOnMenu() != newFile.isShowOnMenu());
    
	}

	public static boolean shouldRefreshMenus(IHTMLPage oldFile, IHTMLPage newFile, boolean isNew){
       if(isNew || oldFile==null || newFile==null) {
            return true;
        }
        return (oldFile.isShowOnMenu() != newFile.isShowOnMenu());
    }




	public static void deleteMenusOnFileSystemOnly(){

	}

	public static void deleteMenus() {

	}

	public static void deleteMenu(Host host)
	{

	}

	public static void deleteMenu(Folder oldFolder,Folder newFolder)
	{

	}

	public static void deleteMenu(WebAsset webAsset)
	{

	}

	public static void deleteMenu(Folder folder)
	{
	
	}
}
