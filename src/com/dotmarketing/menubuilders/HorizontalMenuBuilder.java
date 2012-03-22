package com.dotmarketing.menubuilders;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;


public class HorizontalMenuBuilder implements ViewTool {
    
	
	protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected HostWebAPI hostWebAPI;
    
    private static String velocityRootPath = ConfigUtils.getDynamicVelocityPath() + java.io.File.separator;
	private static String MENU_VTL_PATH = velocityRootPath + "menus" + java.io.File.separator;
    
    //if we only have one host we can call this method
    // call this method
	//path == folder path 
	//pagePath == $VTLSERVLET_URI where the user is at that moment
    //links Folders as default
	public String createMenu(String path, String pagePath, String divName, int numberOfLevels) 
	{
		try {
			Host host = hostWebAPI.getCurrentHost(request);
			return createMenu(path, pagePath, host.getIdentifier(),divName,"",true,"","",numberOfLevels);
		} catch (DotDataException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} 
	}

	public String createMenu(String path, String pagePath, Host host, String divName, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,"",true,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,ulClassName,true,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String imagePrefix, String imageSuffix, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, "", true,imagePrefix, imageSuffix,numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, String imagePrefix, String imageSuffix, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, ulClassName, true, imagePrefix, imageSuffix,numberOfLevels);
	}

	//pass linkFolders folders
	public String createMenu(String path, String pagePath, String divName, boolean linkFolders, int numberOfLevels)
	{
		try {
			Host host = hostWebAPI.getCurrentHost(request);
			return createMenu(path, pagePath, host.getIdentifier(),divName,"",linkFolders,"","",numberOfLevels);
		} catch (SystemException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotDataException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(HorizontalMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}  
	}

	public String createMenu(String path, String pagePath, Host host, String divName, boolean linkFolders, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,"",linkFolders,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, boolean linkFolders, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,ulClassName,linkFolders,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, boolean linkFolders, String imagePrefix, String imageSuffix, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, "", linkFolders,imagePrefix, imageSuffix,numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, boolean linkFolders, String imagePrefix, String imageSuffix, int numberOfLevels)
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, ulClassName, linkFolders, imagePrefix, imageSuffix,numberOfLevels);
	}
	
	
	//// PRIVATE METHOD!!!!!!
	private String createMenu(String path, String pagePath, String hostId, String divName, String ulClassName, boolean linkFolders, String imagePrefix, String imageSuffix, int numberOfLevels)
	//path == folder path
	//pagePath == $VTLSERVLET_URI where the user is at that moment
	//hostid == for multiple hosts
	//addImage == if you want to display images instead of text on the first level
	//imagePrefix == folder name and prefix for the images to use
	//imageSuffix == suffix for the images to use including extension
    {
	   if (numberOfLevels == 0) {
	   		numberOfLevels = Integer.parseInt(Config.getStringProperty("number_levels"));
	   }
	   StringBuffer stringbuf = new StringBuffer();
	   try {

	   	Logger.debug(HorizontalMenuBuilder.class, "\n\n\n\nHorizontalMenuBuilder begins");
	   	Logger.debug(HorizontalMenuBuilder.class, "HorizontalMenuBuilder number of levels=" + numberOfLevels);

	   	if ((path==null) || (path.length()==0)) {

            Logger.debug(HorizontalMenuBuilder.class, "pagePath=" + pagePath);
			
			int idx1 = pagePath.indexOf("/");
			int idx2 = pagePath.indexOf("/",idx1+1);
			
			path = pagePath.substring(idx1,idx2+1);
			
            Logger.debug(HorizontalMenuBuilder.class, "path=" + path);
		}



		
		
	   	Logger.debug(HorizontalMenuBuilder.class, "HorizontalMenuBuilder path=" + path);
	   	Logger.debug(HorizontalMenuBuilder.class, "HorizontalMenuBuilder hostId=" + hostId);

	   	java.util.List itemsList = new ArrayList();
	   	String folderPath = "";
	   	String fileName = "";
	   	boolean fileExists = true;
	   	FolderAPI folderAPI = APILocator.getFolderAPI();
	   	
	   	java.io.File fileFolder = new java.io.File(MENU_VTL_PATH);
	   	if(!fileFolder.exists()){
	   	    fileFolder.mkdirs(); 
	   	}
	   	java.io.File file = null;
	   	
	   	String menuId = "";
	   	
	   	if (path.equals("/")) {
			fileName = hostId + "_horz.vtl";
			menuId = String.valueOf(hostId);
			Host host = new Host();
			try {
				file  = new java.io.File(MENU_VTL_PATH + java.io.File.separator + fileName);
				if (!file.exists()) {
					FileOutputStream fo = new FileOutputStream(file);
					fo.close();
					host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
					itemsList = folderAPI.findSubFolders(host, true);
					folderPath = path;
					fileExists = false;
				}
			} catch (java.io.FileNotFoundException e) {
				file  = new java.io.File(MENU_VTL_PATH + java.io.File.separator + fileName);
				if (!file.exists()) {
					itemsList = folderAPI.findSubFolders(host, true);
					folderPath = path;
					fileExists = false;
				}
			}
	   	}
	   	else {
		   	Folder folder = folderAPI.findFolderByPath(path,hostId,APILocator.getUserAPI().getSystemUser(),false);
		   	Logger.debug(HorizontalMenuBuilder.class, "HorizontalMenuBuilder folder=" +  APILocator.getIdentifierAPI().find(folder).getPath());
			fileName = folder.getInode() + "_horz.vtl";
			menuId = String.valueOf(folder.getInode());
			
			try {
				file  = new java.io.File(MENU_VTL_PATH + java.io.File.separator + fileName);
				if (!file.exists()) {
					FileOutputStream fo = new FileOutputStream(file);
					fo.close();
					itemsList = folderAPI.findMenuItems(folder, APILocator.getUserAPI().getSystemUser(), false);
					folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
					fileExists = false;
				}
			} catch (java.io.FileNotFoundException e) {
				file  = new java.io.File(MENU_VTL_PATH + java.io.File.separator + fileName);
				if (!file.exists()) {
					itemsList = folderAPI.findMenuItems(folder, APILocator.getUserAPI().getSystemUser(), false);
					folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
					fileExists = false;
				}
			}
	   	}
	   	String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;
	   	
	   	if (fileExists) {
	   		return filePath;
	   	}
	   	else {
	   	
	   		if (divName!=null && divName.length()>0) {
	   			String beforeMenu = "<div id=\"" + divName + "\">";
				stringbuf.append(beforeMenu);
	   		}
			if (ulClassName!=null && ulClassName.length() >0) {
				stringbuf.append("<ul class=\"" + ulClassName + "\">");
			}
			else {
				stringbuf.append("<ul>");
			}

			if (divName!=null && divName.length()>0) {
				stringbuf.append("#if($EDIT_MODE)\n");				
				stringbuf.append("<form action=\"${directorURL}\" method=\"post\" name=\"form_menu_" + menuId + "\" id=\"form_menu_" + menuId + "\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"cmd\" value=\"orderMenu\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"path\" value=\"" + path + "\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"openAll\" value=\"true\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"hostId\" value=\"" + hostId + "\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"pagePath\" value=\"" + path + "\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"referer\" value=\"$VTLSERVLET_URI\">\n");
				stringbuf.append("<a href=\"javascript:submitMenu('form_menu_" + menuId + "');\">Reorder Menu\n");
				stringbuf.append("</a></form>\n");
				stringbuf.append("#end \n");
			}
			
			//gets menu items for this folder
			java.util.Iterator itemListIterator = itemsList.iterator();
		   	Logger.debug(HorizontalMenuBuilder.class, "HorizontalMenuBuilder number of items=" + itemsList.size());
	
			///FIRST LEVEL MENU ITEMS!!!!
			while (itemListIterator.hasNext()) {
	
				Inode itemChild = (Inode) itemListIterator.next();
				
				if (itemChild instanceof Folder) {
					
					Folder folderChild = (Folder) itemChild;
					
					//recursive method here
					stringbuf = getMenuItems(stringbuf, folderChild, linkFolders, numberOfLevels, 1, imagePrefix, imageSuffix);
					
				}
				else if (itemChild instanceof Link) {
					if (((Link)itemChild).isWorking() && !((Link)itemChild).isDeleted()) {
						stringbuf.append("<li><a href=\"" + ((Link)itemChild).getProtocal() + ((Link)itemChild).getUrl() + "\" target=\"" + ((Link)itemChild).getTarget() + "\">\n");
						stringbuf.append(((Link)itemChild).getTitle() + "</a></li>\n");
					}
				}
				else if (itemChild instanceof HTMLPage) {
					if (((HTMLPage)itemChild).isWorking() && !((HTMLPage)itemChild).isDeleted()) {
						stringbuf.append("<li><a href=\"" + folderPath + ((HTMLPage)itemChild).getPageUrl() + "\">\n");
						stringbuf.append(((HTMLPage)itemChild).getTitle() + "</a></li>\n");
					}
				}
				else if (itemChild instanceof File) {
					if (((File)itemChild).isWorking() && !((File)itemChild).isDeleted()) {
						stringbuf.append("<li><a href=\"" + folderPath + ((File)itemChild).getFileName() + "\">\n");
						stringbuf.append(((File)itemChild).getTitle() + "</a></li>\n");
					}
				}				
			}
			stringbuf.append("</ul>");
	
			
			stringbuf.append("<br style=\"clear: both;\" />");
			
	   		if (divName!=null && divName.length()>0) {
	   			stringbuf.append("</div>");
	   		}
			
			FileOutputStream fo = new FileOutputStream(file);
            //Specifying explicitly a proper character encoding
            OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());
            

			if (stringbuf.toString().getBytes().length>0) {
	            out.write(stringbuf.toString());
			}
			else {
		        Logger.debug(HorizontalMenuBuilder.class, "Error creating static horizontal menu!!!!!");
			}
	
            out.flush();
            out.close();
			fo.close();
	        Logger.debug(HorizontalMenuBuilder.class, "End of HorizontalMenuBuilder" + filePath);

	        return filePath;
	   	}
   } catch(Exception e) {
       // Clear the string buffer, and insert only the main hyperlink text to it.
       // Ignore the embedded links.
       stringbuf.delete(0, stringbuf.length());
       Logger.debug(HorizontalMenuBuilder.class,e.getMessage());
       
   } 
  	return "";
   }

	/**
     * Initializes this instance for the current request.
     * 
     * @param obj
     *            the ViewContext of the current request
     */
    public void init(Object obj) {
        ViewContext context = (ViewContext) obj;
        this.request = context.getRequest();
        this.response = context.getResponse();
        this.hostWebAPI = WebAPILocator.getHostWebAPI();
    }

    private StringBuffer getMenuItems(StringBuffer stringbuf, Folder folderChildChild, boolean linkFolders, int numberOfLevels, int currentLevel, String imagePrefix, String imageSuffix) throws DotStateException, DotDataException, DotSecurityException {
    	
		//gets menu items for this folder
		java.util.List itemsChildrenList2 = new ArrayList();
		String folderPath= "";
		try {
			itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(folderChildChild, APILocator.getUserAPI().getSystemUser(), false);
			folderPath = APILocator.getIdentifierAPI().find(folderChildChild).getPath();
		} catch (Exception e) {
			Logger.error(HorizontalMenuBuilder.class,e.getMessage());
		} 
		java.util.Iterator itemsChildrenListIter2 = itemsChildrenList2.iterator();
		boolean nextLevelItems = false;		

		
		if (itemsChildrenListIter2.hasNext()) {
			stringbuf.append("<li id=\"" + folderChildChild.getName()+ "\">");
			if (linkFolders) stringbuf.append("<a href=\"#\">");
			nextLevelItems = true;
		}
		else {
			stringbuf.append("<li id=\"" + folderChildChild.getName()+ "\">");
			if (linkFolders) stringbuf.append("<a href=\"" + folderPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION") + "\">"); 
		}

		//if it uses an image or text.
		if ((imagePrefix!=null && imagePrefix.length()>0) ||
			(imageSuffix!=null && imageSuffix.length() >0)) {
			stringbuf.append("<img src=\"" + imagePrefix + folderChildChild.getName() + imageSuffix + "\" alt=\"" + folderChildChild.getTitle() + "\" />");
		}
		else {
			stringbuf.append(folderChildChild.getTitle());
		}
		if (linkFolders) {
			stringbuf.append("</a>\n");
		}
		
		if (nextLevelItems) {
			stringbuf.append("<ul>\n");
		}
		
		while (itemsChildrenListIter2.hasNext()) {
			
			Inode childChild2 = (Inode) itemsChildrenListIter2.next();

			if (childChild2 instanceof Folder) {
				Folder folderChildChild2 = (Folder) childChild2;
				
				Logger.debug(this, "folderChildChild2= " + folderChildChild2.getTitle() + " currentLevel="+ currentLevel + " numberOfLevels=" + numberOfLevels);
				if (currentLevel <= numberOfLevels) {
					stringbuf = getMenuItems(stringbuf, folderChildChild2, linkFolders, numberOfLevels, currentLevel + 1, imagePrefix, imageSuffix);
				}
				else {
					if (linkFolders) {
						String path = "";
						try {
							path = APILocator.getIdentifierAPI().find(folderChildChild).getPath();
						} catch (Exception e) {
							Logger.debug(HorizontalMenuBuilder.class,e.getMessage());
						} 
						stringbuf.append("<li><a href=\"" + path + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION") + "\">\n");
						stringbuf.append(folderChildChild2.getTitle() + "</a></li>\n");
					}
					else {
						stringbuf.append("<li>" + folderChildChild2.getTitle() + "</li>\n");
					}
				}
			}
			else if (childChild2 instanceof Link) {
				if (((Link)childChild2).isWorking() && !((Link)childChild2).isDeleted()) {
					stringbuf.append("<li><a href=\"" + ((Link)childChild2).getProtocal() + ((Link)childChild2).getUrl() + "\" target=\"" + ((Link)childChild2).getTarget() + "\">\n");
					stringbuf.append(((Link)childChild2).getTitle() + "</a></li>\n");
				}
			}
			else if (childChild2 instanceof HTMLPage) {
				if (((HTMLPage)childChild2).isWorking() && !((HTMLPage)childChild2).isDeleted()) {
					stringbuf.append("<li><a href=\"" + folderPath + ((HTMLPage)childChild2).getPageUrl() + "\">\n");
					stringbuf.append(((HTMLPage)childChild2).getTitle() + "</a></li>\n");
				}
			}
			else if (childChild2 instanceof File) {
				if (((File)childChild2).isWorking() && !((File)childChild2).isDeleted()) {
					stringbuf.append("<li><a href=\"" + folderPath + ((File)childChild2).getFileName() + "\">\n");
					stringbuf.append(((File)childChild2).getTitle() + "</a></li>\n");
				}
			}
			
		}
		if (nextLevelItems) {
			stringbuf.append("</ul></li>\n");
		}
		else {
			stringbuf.append("</li>\n");
		}
		return stringbuf; 
	}
}
