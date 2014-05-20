package com.dotmarketing.viewtools.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class NavTool implements ViewTool {
    
    private static NavToolCache navCache = null;
    private static FolderAPI fAPI=null;
    private Host currenthost=null;
    private static User user=null;
    private HttpServletRequest request = null;
    
    static {
        try {
            user=APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            Logger.error(NavTool.class, e.getMessage(), e);
        }
    }
    
    @Override
    public void init(Object initData) {
        ViewContext context = (ViewContext) initData;
        try {
    		this.request = context.getRequest();
            currenthost=WebAPILocator.getHostWebAPI().getCurrentHost(context.getRequest());
            fAPI = APILocator.getFolderAPI();
            navCache = CacheLocator.getNavToolCache();
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
    }
    
    protected static NavResult getNav(Host host, String path) throws DotDataException, DotSecurityException {
        
        if(path != null && path.contains(".")){
        	path = path.substring(0, path.lastIndexOf("/"));
        }
        
        Folder folder=!path.equals("/") ? fAPI.findFolderByPath(path, host, user, true) : fAPI.findSystemFolder();
        if(folder==null || !UtilMethods.isSet(folder.getIdentifier()))
            return null;
        
        NavResult result=navCache.getNav(host.getIdentifier(), folder.getInode());
        
	        if(result != null) {
	        	 if(!folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)) {
	                 Identifier ident=APILocator.getIdentifierAPI().find(folder);
	        	CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());
	        	 }
	        }
	        
            String parentId;
            if(!folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)) {
                Identifier ident=APILocator.getIdentifierAPI().find(folder);
                parentId=ident.getParentPath().equals("/") ? 
                        FolderAPI.SYSTEM_FOLDER : fAPI.findFolderByPath(ident.getParentPath(), host, user, false).getInode();
            }
            else
                parentId=null;
            result=new NavResult(parentId, host.getIdentifier(),folder.getInode());
            Identifier ident=APILocator.getIdentifierAPI().find(folder);
            result.setHref(ident.getURI());
            result.setTitle(folder.getTitle());
            result.setOrder(folder.getSortOrder());
            result.setType("folder");
            result.setPermissionId(folder.getPermissionId());
            List<NavResult> children=new ArrayList<NavResult>();
            List<String> folderIds=new ArrayList<String>();
            result.setChildren(children);
            result.setChildrenFolderIds(folderIds);
        
            List menuItems;
            if(path.equals("/"))
                menuItems = fAPI.findSubFolders(host, true);
            else
                menuItems = fAPI.findMenuItems(folder, user, true);
            
            for(Object item : menuItems) {
                if(item instanceof Folder) {
                    Folder itemFolder=(Folder)item;
                    ident=APILocator.getIdentifierAPI().find(itemFolder);
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier(),itemFolder.getInode());
                    nav.setTitle(itemFolder.getTitle());
                    nav.setHref(ident.getURI());
                    nav.setOrder(itemFolder.getSortOrder());
                    nav.setType("folder");
                    nav.setPermissionId(itemFolder.getPermissionId());
                    // it will load lazily its children
                    folderIds.add(itemFolder.getInode());
                    children.add(nav);
                }
                else if(item instanceof HTMLPage) {
                	final String httpProtocol = "http://";
                	final String httpsProtocol = "https://";
                    HTMLPage itemPage=(HTMLPage)item;
                    ident=APILocator.getIdentifierAPI().find(itemPage);
                    HTMLPage page = null;
                    page = APILocator.getHTMLPageAPI().loadLivePageById(ident.getInode(), APILocator.getUserAPI().getSystemUser(), false);
                    String redirectUri = page.getRedirect();
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier());
                    nav.setTitle(itemPage.getTitle());
                    if(UtilMethods.isSet(redirectUri) && !redirectUri.startsWith("/")){
                        if(redirectUri.startsWith(httpsProtocol) || redirectUri.startsWith(httpProtocol)){
                      	  nav.setHref(redirectUri);	
                        }else{
                      	  	if(page.isHttpsRequired())
                      	  		nav.setHref(httpsProtocol+redirectUri);	
                        		else	
                        			nav.setHref(httpProtocol+redirectUri);
                        }
                      	
                      }else{
                      	nav.setHref(ident.getURI());
                      }
                    nav.setOrder(itemPage.getSortOrder());
                    nav.setType("htmlpage");
                    nav.setPermissionId(itemPage.getPermissionId());
                    children.add(nav);
                }
                else if(item instanceof Link) {
                    Link itemLink=(Link)item;
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier());
                    if(itemLink.getLinkType().equals(LinkType.CODE.toString()) && LinkType.CODE.toString() !=null  ) {
                        nav.setCodeLink(itemLink.getLinkCode());
                    }
                    else {
                        nav.setHref(itemLink.getWorkingURL());
                    }
                    nav.setTitle(itemLink.getTitle());
                    nav.setOrder(itemLink.getSortOrder());
                    nav.setType("link");
                    nav.setPermissionId(itemLink.getPermissionId());
                    children.add(nav);
                }
                else if(item instanceof IFileAsset) {
                    IFileAsset itemFile=(IFileAsset)item;
                    ident=APILocator.getIdentifierAPI().find(itemFile.getPermissionId());
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier());
                    nav.setTitle(itemFile.getFriendlyName());
                    nav.setHref(ident.getURI());
                    nav.setOrder(itemFile.getMenuOrder());
                    nav.setType("file");
                    nav.setPermissionId(itemFile.getPermissionId());
                    children.add(nav);
                }
            }
            
            navCache.putNav(host.getIdentifier(), folder.getInode(), result);
    
        
        return result;
    }
    
    public NavResult getNav() throws DotDataException, DotSecurityException {
    	return getNav((String)request.getAttribute("javax.servlet.forward.request_uri"));
    }
    
    public NavResult getNav(String path) throws DotDataException, DotSecurityException {
        
        Host host=currenthost;
        if(path.startsWith("//")) {
            List<RegExMatch> find = RegEX.find(path, "^//(\\w+)/(.+)");
            if(find.size()==1) {
                String hostname=find.get(0).getGroups().get(0).getMatch();
                path="/"+find.get(0).getGroups().get(1).getMatch();
                host=APILocator.getHostAPI().findByName(hostname, user, false);
            }
        }
        
        return getNav(host,path);
    }
    
}
