package com.dotmarketing.menubuilders;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;


public class StaticMenuBuilder implements ViewTool {
    
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
	public String createMenu(String path, String pagePath, String divName, int numberOfLevels) throws JspException
	{
		try {
			Host host = hostWebAPI.getCurrentHost(request);
			return createMenu(path, pagePath, host.getIdentifier(),divName,"",true,"","",numberOfLevels);
		} catch (PortalException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotDataException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	public String createMenu(String path, String pagePath, Host host, String divName, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,"",true,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,ulClassName,true,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String imagePrefix, String imageSuffix, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, "", true,imagePrefix, imageSuffix,numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, String imagePrefix, String imageSuffix, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, ulClassName, true, imagePrefix, imageSuffix,numberOfLevels);
	}

	//pass linkFolders folders
	public String createMenu(String path, String pagePath, String divName, boolean linkFolders, int numberOfLevels) throws JspException
	{
		try {
			Host host = hostWebAPI.getCurrentHost(request);
			return createMenu(path, pagePath, host.getIdentifier(),divName,"",linkFolders,"","",numberOfLevels);
		} catch (DotDataException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	public String createMenu(String path, String pagePath, Host host, String divName, boolean linkFolders, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,"",linkFolders,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, boolean linkFolders, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName,ulClassName,linkFolders,"","",numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, boolean linkFolders, String imagePrefix, String imageSuffix, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, "", linkFolders,imagePrefix, imageSuffix,numberOfLevels);
	}

	public String createMenu(String path, String pagePath, Host host, String divName, String ulClassName, boolean linkFolders, String imagePrefix, String imageSuffix, int numberOfLevels) throws JspException
	{
		return createMenu(path, pagePath, host.getIdentifier(),divName, ulClassName, linkFolders, imagePrefix, imageSuffix,numberOfLevels);
	}
	
	
	//// PRIVATE METHOD!!!!!!
	private String createMenu(String path, String pagePath, String hostId, String divName, String ulClassName, boolean linkFolders, String imagePrefix, String imageSuffix, int numberOfLevels) throws JspException
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

	   	Logger.debug(StaticMenuBuilder.class, "\n\n\n\nStaticMenuBuilder begins");
	   	Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder number of levels=" + numberOfLevels);

	   	if ((path==null) || (path.length()==0)) {

            Logger.debug(StaticMenuBuilder.class, "pagePath=" + pagePath);
			
			int idx1 = pagePath.indexOf("/");
			int idx2 = pagePath.indexOf("/",idx1+1);
			
			path = pagePath.substring(idx1,idx2+1);
			
            Logger.debug(StaticMenuBuilder.class, "path=" + path);
		}

		
	   	Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder path=" + path);
	   	Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder hostId=" + hostId);

	   	java.util.List itemsList = new ArrayList();
	   	String folderPath = "";
	   	String fileName = "";
	   	boolean fileExists = true;
	   	
	   	java.io.File fileFolder = new java.io.File(StaticMenuBuilder.MENU_VTL_PATH);
	   	if(!fileFolder.exists()){
	   	    fileFolder.mkdirs(); 
	   	}
	   	java.io.File file = null;
	   	FolderAPI fapi = APILocator.getFolderAPI();
	   	UserAPI uapi = APILocator.getUserAPI();
	   	String menuId = "";
	   	if (path.equals("/")) {
			fileName = hostId + "_static.vtl";
			menuId = String.valueOf(hostId);
			file  = new java.io.File(StaticMenuBuilder.MENU_VTL_PATH + java.io.File.separator + fileName);
			if (!file.exists()) {
				Host host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(),false);
				itemsList = fapi.findSubFolders(host, true);
				folderPath = path;
				fileExists = false;
			}
	   	}
	   	else {
		   	Folder folder = fapi.findFolderByPath(path,hostId,uapi.getSystemUser(),false);
		   	Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder folder=" + APILocator.getIdentifierAPI().find(folder).getPath());
			fileName = folder.getInode() + "_static.vtl";
			menuId = String.valueOf(folder.getInode());
			file  = new java.io.File(StaticMenuBuilder.MENU_VTL_PATH + java.io.File.separator + fileName);
			if (!file.exists()) {
				itemsList = fapi.findMenuItems(folder,uapi.getSystemUser(),false);
				folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
				fileExists = false;
			}
	   	}
	   	String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;
	   	
	   	if (fileExists) {
	   		return filePath;
	   	}
	   	else {
	   	
	   		if (itemsList.size()>0) {
		   		if (divName!=null && divName.length()>0) {
		   			String beforeMenu = "<div id=\"" + divName + "\">";
					stringbuf.append(beforeMenu);
		   		}
				
		   		if (divName!=null && divName.length()>0) {
					stringbuf.append("#if($EDIT_MODE)\n");				
					stringbuf.append("<form action=\"${directorURL}\" method=\"post\" name=\"form_menu_" + menuId + "\" id=\"form_menu_" + menuId + "\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"cmd\" value=\"orderMenu\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"path\" value=\""+path+"\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"hostId\" value=\"" + hostId + "\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"pagePath\" value=\"$VTLSERVLET_URI\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"referer\" value=\"$VTLSERVLET_URI\">\n");
					
                    stringbuf.append("<div class=\"menuReorder\"><a href=\"javascript:submitMenu('form_menu_" + menuId + "');\">Reorder Menu</a></div>");
					stringbuf.append("</form>\n");
					stringbuf.append("#end \n");
				}

				if (ulClassName!=null && ulClassName.length() >0) {
					stringbuf.append("<ul class=\"" + ulClassName + "\">");
				}
				else {
					stringbuf.append("<ul>");
				}
	
				
				//gets menu items for this folder
				java.util.Iterator itemListIterator = itemsList.iterator();
			   	Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder number of items=" + itemsList.size());
		
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
							stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
	                        stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\""  +  ((Link)itemChild).getProtocal() + ((Link)itemChild).getUrl() + "\"))\n");
							stringbuf.append("<li><a href=\"" + ((Link)itemChild).getProtocal() + UtilMethods.encodeURIComponent(((Link)itemChild).getUrl()) + "\" target=\"" + ((Link)itemChild).getTarget() + "\">\n");
							stringbuf.append(((Link)itemChild).getTitle() + "</a></li>\n");
	                        stringbuf.append("#else\n");
							stringbuf.append("<li class=\"active\"><a href=\"" + ((Link)itemChild).getProtocal() + UtilMethods.encodeURIComponent(((Link)itemChild).getUrl()) + "\" target=\"" + ((Link)itemChild).getTarget() + "\">\n");
							stringbuf.append(((Link)itemChild).getTitle() + "</a></li>\n");
	                        stringbuf.append("#end \n");
						}
					}
					else if (itemChild instanceof HTMLPage) {
						if (((HTMLPage)itemChild).isWorking() && !((HTMLPage)itemChild).isDeleted()) {
							stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
	                        stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\""  + path + ((HTMLPage)itemChild).getPageUrl() + "\"))\n");
							stringbuf.append("<li><a href=\"" + UtilMethods.encodeURIComponent(folderPath + ((HTMLPage)itemChild).getPageUrl()) + "\">\n");
							stringbuf.append(((HTMLPage)itemChild).getTitle() + "</a></li>\n");
	                        stringbuf.append("#else\n");
							stringbuf.append("<li class=\"active\"><a href=\"" + UtilMethods.encodeURIComponent(folderPath + ((HTMLPage)itemChild).getPageUrl()) + "\">\n");
							stringbuf.append(((HTMLPage)itemChild).getTitle() + "</a></li>\n");
	                        stringbuf.append("#end \n");
						}
					}
					else if (itemChild instanceof File) {
						if (((File)itemChild).isWorking() && !((File)itemChild).isDeleted()) {
							stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
	                        stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\""  + path + ((File)itemChild).getFileName() + "\"))\n");
							stringbuf.append("<li><a href=\"" + UtilMethods.encodeURIComponent(folderPath + ((File)itemChild).getFileName()) + "\">\n");
							stringbuf.append(((File)itemChild).getTitle() + "</a></li>\n");
	                        stringbuf.append("#else\n");
	                        stringbuf.append("#end \n");
						}
					}				
				}
				stringbuf.append("</ul>");
		
				
		   		if (divName!=null && divName.length()>0) {
		   			stringbuf.append("</div>");
		   		}
	   		}
	   		
			if (stringbuf.length() > 0) {
				FileOutputStream fo = new FileOutputStream(file);
				// Specifying explicitly a proper character set encoding
				OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());
	            out.write(stringbuf.toString());
	            out.flush();
	            out.close();

				fo.close();
			}
			else {
		        Logger.debug(StaticMenuBuilder.class, "Error creating static horizontal menu!!!!!");
			}

	        Logger.debug(StaticMenuBuilder.class, "End of StaticMenuBuilder" + filePath);

	        return filePath;
	   	}
   } catch(Exception e) {
       // Clear the string buffer, and insert only the main hyperlink text to it.
       // Ignore the embedded links.
       stringbuf.delete(0, stringbuf.length());
       Logger.info(StaticMenuBuilder.class,e.getMessage());
       
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
    	
        Host host;
		try {
			host = hostWebAPI.getCurrentHost(request);
		} catch (PortalException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotDataException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(StaticMenuBuilder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
        String folderPath = "";
		try {
			folderPath = APILocator.getIdentifierAPI().find(folderChildChild).getPath();
		} catch (Exception e2) {
			Logger.error(StaticMenuBuilder.class, e2.getMessage(),e2);
		} 
		//gets menu items for this folder
		java.util.List itemsChildrenList2 = new ArrayList();
		try {
			itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(folderChildChild,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e2) {
			Logger.error(StaticMenuBuilder.class, e2.getMessage(),e2);
		} 
		java.util.Iterator itemsChildrenListIter2 = itemsChildrenList2.iterator();
		boolean nextLevelItems = false;		

		
		if (itemsChildrenListIter2.hasNext()) {
			nextLevelItems = true;
		}
		
		String folderChildPath = folderPath.substring(0, folderPath.length() - 1);
		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));
		
		stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
		stringbuf.append("#if ($VTLSERVLET_DECODED_URI != '"  + folderChildPath + "' && $VTLSERVLET_DECODED_URI != '" + folderPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION") + "')\n");
		stringbuf.append("<li id=\"" + folderChildChild.getName()+ "\">");
		
		HTMLPage page = new HTMLPage();
		try {
			page = HTMLPageFactory.getLiveHTMLPageByPath(folderPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"), host);
		} catch (Exception e1) {
			Logger.error(StaticMenuBuilder.class, e1.getMessage(),e1);
		} 
		if (!InodeUtils.isSet(page.getInode())) {
			if (linkFolders) stringbuf.append("<a href=\""+UtilMethods.encodeURIComponent(folderPath) + "\">");
		} else { 
			if (linkFolders) stringbuf.append("<a href=\""+UtilMethods.encodeURIComponent(folderPath) + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION")+"\">");
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
        stringbuf.append("#else\n");
		stringbuf.append("<li class=\"active\" id=\"" + folderChildChild.getName()+ "\"><a href=\"#\">" + folderChildChild.getTitle() + "</a></li>");
        stringbuf.append("#end \n");
		
		if (nextLevelItems) {
			stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
	        stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\""  +  folderPath + "\"))\n");
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
						String path="";
						try {
							path = APILocator.getIdentifierAPI().find(folderChildChild2).getPath();
						} catch (Exception e) {
							 Logger.error(StaticMenuBuilder.class, e.getMessage(),e);
						} 
						stringbuf.append("<li><a href=\"" + UtilMethods.encodeURIComponent(path) + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION") + "\">\n");
						stringbuf.append(folderChildChild2.getTitle() + "</a></li>\n");
					}
					else {
						stringbuf.append("<li>" + folderChildChild2.getTitle() + "</li>\n");
					}
				}
			}
			else if (childChild2 instanceof Link) {
				if (((Link)childChild2).isWorking() && !((Link)childChild2).isDeleted()) {
					stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
					stringbuf.append("#if ($VTLSERVLET_DECODED_URI != '"  +  ((Link)childChild2).getProtocal() + ((Link)childChild2).getUrl() + "')\n");
					stringbuf.append("<li><a href=\"" + ((Link)childChild2).getProtocal() + UtilMethods.encodeURIComponent(((Link)childChild2).getUrl()) + "\" target=\"" + ((Link)childChild2).getTarget() + "\">\n");
					stringbuf.append(((Link)childChild2).getTitle() + "</a></li>\n");
                    stringbuf.append("#else\n");
					stringbuf.append("<li class=\"active\"><a href=\"" + ((Link)childChild2).getProtocal() + UtilMethods.encodeURIComponent(((Link)childChild2).getUrl()) + "\" target=\"" + ((Link)childChild2).getTarget() + "\">\n");
					stringbuf.append(((Link)childChild2).getTitle() + "</a></li>\n");
                    stringbuf.append("#end \n");
				}
			}
			else if (childChild2 instanceof HTMLPage) {
				if (((HTMLPage)childChild2).isWorking() && !((HTMLPage)childChild2).isDeleted()) {
					stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
					stringbuf.append("#if ($VTLSERVLET_DECODED_URI != '"  + folderPath + ((HTMLPage)childChild2).getPageUrl() + "')\n");
					stringbuf.append("<li><a href=\"" + UtilMethods.encodeURIComponent(folderPath + ((HTMLPage)childChild2).getPageUrl()) + "\">\n");
					stringbuf.append(((HTMLPage)childChild2).getTitle() + "</a></li>\n");
                    stringbuf.append("#else\n");
					stringbuf.append("<li class=\"active\"><a href=\"" + UtilMethods.encodeURIComponent(folderPath + ((HTMLPage)childChild2).getPageUrl()) + "\">\n");
					stringbuf.append(((HTMLPage)childChild2).getTitle() + "</a></li>\n");
                    stringbuf.append("#end \n");
				}
			}
			else if (childChild2 instanceof File) {
				if (((File)childChild2).isWorking() && !((File)childChild2).isDeleted()) {
					stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")\n");
					stringbuf.append("#if ($VTLSERVLET_URI != '"  + folderPath + ((File)childChild2).getFileName() + "')\n");
					stringbuf.append("<li><a href=\"" + UtilMethods.encodeURIComponent(folderPath + ((File)childChild2).getFileName()) + "\">\n");
					stringbuf.append(((File)childChild2).getTitle() + "</a></li>\n");
                    stringbuf.append("#else\n");
					stringbuf.append("<li class=\"active\"><a href=\"" + UtilMethods.encodeURIComponent(folderPath + ((File)childChild2).getFileName()) + "\">\n");
					stringbuf.append(((File)childChild2).getTitle() + "</a></li>\n");
                    stringbuf.append("#end \n");
				}
			}
		}
		if (nextLevelItems) {
			stringbuf.append("</ul></li>\n");
	        stringbuf.append("#end\n");
		}
		else {
			stringbuf.append("</li>\n");
		}

		return stringbuf; 
	}
}
