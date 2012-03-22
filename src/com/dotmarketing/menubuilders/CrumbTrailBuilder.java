package com.dotmarketing.menubuilders;

import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class CrumbTrailBuilder implements ViewTool {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	private FolderAPI fapi = APILocator.getFolderAPI();
	
	public String createCrumbTrail(String pagePath, Host host) throws JspException, DotSecurityException, DotStateException, DotDataException
	{
		StringBuffer stringbuf = new StringBuffer();

		HashSet listItems = new HashSet();
		
		String path = pagePath;
		
		Logger.debug(CrumbTrailBuilder.class, "\n\n\n\n\n\n\n\n\n\n\n");
		Logger.debug(CrumbTrailBuilder.class, "CrumbTrailBuilderTag pagePath=" + path);

		int idx1 = path.indexOf("/");
		int idx2 = path.indexOf("/",idx1+1);


		stringbuf.append("<div id=\"crumbtrail\"><ul>");
		
		String language = null;
		if(language == null)
    		language = (String)request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);	
    	if (language == null)
    		language = String.valueOf(langAPI.getDefaultLanguage().getId());
    	Language lang = langAPI.getLanguage(language);
		String h = APILocator.getLanguageAPI().getStringKey(lang, "home");
		if(!UtilMethods.isSet(h)){
			h= "Home";
		}
		stringbuf.append("<li><a href=\"/\">" + h + "</a> &gt; </li>");
		listItems.add(h);
		
		idx2 = 0;

		String openPath = "";
		idx1 = path.indexOf("/",idx2+1);
		int count = 0;

		while (idx1 != -1 ) {
			openPath = path.substring(0, idx1+1);
			idx2 = idx1;
			idx1 = path.indexOf("/",idx2+1);
	        Logger.debug(CrumbTrailBuilder.class, "main loop: openPath=" + openPath);
			count++;
			stringbuf = getTrail(host, stringbuf,openPath,idx1,path,listItems);
		} 
		
		stringbuf.append("</ul></div>");
		
		return stringbuf.toString();
	}

	private StringBuffer getTrail(Host host, StringBuffer stringbuffer, String openPath,int idxOf, String path, HashSet listItems) throws DotSecurityException, DotStateException, DotDataException {

		Folder folder = fapi.findFolderByPath(openPath,host,APILocator.getUserAPI().getSystemUser(),false);
	    String folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
		
		if (idxOf!=-1) {
	        Logger.debug(CrumbTrailBuilder.class, "getTrail: idxOf");
			if (listItems.add(folder.getTitle())) {
				stringbuffer.append("<li><a href=\"" + folderPath + "\">");
				stringbuffer.append(folder.getTitle() + "</a> &gt; </li>");
			}
		}
		else {

			int idxTrail = path.lastIndexOf("/");

			if (idxTrail!=(path.length()-1)) {
				
				openPath = path.substring(0,idxTrail+1);

				if (path.indexOf(".")!=-1) {

					String pagePath = path.substring(idxTrail+1,path.length());
			        Logger.debug(CrumbTrailBuilder.class, "getTrail: Page Path=" + pagePath);
			        Logger.debug(CrumbTrailBuilder.class, "getTrail: Path=" + openPath);
			        
					if (!pagePath.equals("index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"))) {

				        HTMLPage page = HTMLPageFactory.getLiveHTMLPageByPath(openPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"), host);
				        
					    if (InodeUtils.isSet(page.getInode())) {
							if (listItems.add(folder.getTitle())) {
						        stringbuffer.append("<li><a href=\"" + folderPath + "\">");
						        stringbuffer.append(folder.getTitle() + "</a> &gt; </li>");
							}
					    } 

				        Logger.debug(CrumbTrailBuilder.class, "getTrail: it's not index.html");
						stringbuffer = getPageTrail(host, stringbuffer, openPath, pagePath,listItems);
					}
					else {
				        Logger.debug(CrumbTrailBuilder.class, "getTrail: it's index.html");
						if (listItems.add(folder.getTitle())) {
							stringbuffer.append("<li>" + folder.getTitle() + "</li>");
						}
					}
				}
			}
			else {
		        Logger.debug(CrumbTrailBuilder.class, "getTrail: there isn't a page name");
				if (listItems.add(folder.getTitle())) {
		        	stringbuffer.append("<li>" + folder.getTitle() + " &gt; </li>");
		        }
			}
		}

		return stringbuffer;
	}
	
	private StringBuffer getPageTrail(Host host, StringBuffer stringbuffer, String openPath,String pagePath, HashSet listItems) throws DotSecurityException, DotDataException {
		   return  getPageTrail (host.getIdentifier(), stringbuffer, openPath, pagePath,listItems);
	}

	private StringBuffer getPageTrail(String hostId, StringBuffer stringbuffer, String openPath,String pagePath, HashSet listItems) throws DotSecurityException, DotDataException {
		
	    if (UtilMethods.isSet(request.getParameter("crumbTitle"))) {
	        String title = request.getParameter("crumbTitle");
	        title = UtilHTML.htmlEncode(title);
	        stringbuffer.append("<li>" + title + "</li>");
        } else if (InodeUtils.isSet(request.getParameter("inode"))) {
            String inode = request.getParameter("inode");
            Contentlet cont = new Contentlet();
            try{
            	cont = conAPI.find(inode, user, true);
            }catch(DotDataException e){
            	Logger.error(WebAssetFactory.class,"Unable to find Contentlet with inode " + inode, e);
            }
            try {
				stringbuffer.append("<li>" + conAPI.getName(cont, APILocator.getUserAPI().getSystemUser(), true) + "</li>");
			} catch (Exception e) {
				stringbuffer.append("<li>" + "" + "</li>");
				Logger.error(WebAssetFactory.class,"Unable to get Contentlet", e);
			}
        } else {
        	Host h = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
    		Folder folder = fapi.findFolderByPath(openPath, h,user,true);
    
    		Identifier identifier = APILocator.getIdentifierAPI().find(h, APILocator.getIdentifierAPI().find(folder).getPath() + pagePath);
    		HTMLPage htmlpage = (HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(identifier,APILocator.getUserAPI().getSystemUser(),false);
    		
    		if (htmlpage!=null) {
    			if (listItems.add(htmlpage.getTitle())) {
    	        	stringbuffer.append("<li>" + htmlpage.getTitle() + "</li>");
    	        }
    		}
        }

		return stringbuffer;
	}

    private HttpServletRequest request;
    
  
    Context ctx;
    private User user = null;
    public void init(Object obj) {
        ViewContext context = (ViewContext) obj;
        this.request = context.getRequest();
        ctx = context.getVelocityContext();
        HttpSession ses = request.getSession(false);
    	if (ses != null)
    		user = (User) ses.getAttribute(WebKeys.CMS_USER);
    }
}
