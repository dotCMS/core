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
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class NavTool implements ViewTool {

    private Host currenthost=null;
    private static User systemUser=null;
    private HttpServletRequest request = null;
    private long currentLanguage = 0;
    
    static {

        try {
        	systemUser=APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            Logger.error(NavTool.class, e.getMessage(), e);
        }
    }
    
    @Override
    public void init(Object initData) {
        ViewContext context = (ViewContext) initData;

        try {
    		this.request = context.getRequest();
            this.currenthost=WebAPILocator.getHostWebAPI().getCurrentHost(context.getRequest());
            this.currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(this.request).getId();
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
    }
    
    protected NavResult getNav(Host host, String path) throws DotDataException, DotSecurityException {
        return getNav(host, path, this.currentLanguage, this.systemUser);
    }
    
    protected static NavResult getNav(Host host, String path, long languageId, User systemUserParam) throws DotDataException, DotSecurityException {
        
        if(path != null && path.contains(".")){
        	path = path.substring(0, path.lastIndexOf("/"));
        }

        Folder folder=!path.equals("/") ? APILocator.getFolderAPI().findFolderByPath(path, host, systemUserParam, true) : APILocator.getFolderAPI().findSystemFolder();
        if(folder==null || !UtilMethods.isSet(folder.getIdentifier()))
            return null;
        
        NavResult result=CacheLocator.getNavToolCache().getNav(host.getIdentifier(), folder.getInode(), languageId);

        if(result != null) {
        	
        	return result;
        	
        } else {
        	String parentId;
            if(!folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)) {
                Identifier ident=APILocator.getIdentifierAPI().find(folder);
                parentId=ident.getParentPath().equals("/") ? 
                        FolderAPI.SYSTEM_FOLDER : APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, systemUserParam, false).getInode();
            } else {
                parentId=null;
            }
            result=new NavResult(parentId, host.getIdentifier(),folder.getInode(),languageId);
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
            result.setShowOnMenu(folder.isShowOnMenu());

            List menuItems;
            if(path.equals("/"))
                menuItems = APILocator.getFolderAPI().findSubFolders(host, true);
            else
                menuItems = APILocator.getFolderAPI().findMenuItems(folder, systemUserParam, true);
            
            for(Object item : menuItems) {
                if(item instanceof Folder) {
                    Folder itemFolder=(Folder)item;
                    ident=APILocator.getIdentifierAPI().find(itemFolder);
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier(),itemFolder.getInode(),languageId);
                    nav.setTitle(itemFolder.getTitle());
                    nav.setHref(ident.getURI());
                    nav.setOrder(itemFolder.getSortOrder());
                    nav.setType("folder");
                    nav.setPermissionId(itemFolder.getPermissionId());
                    nav.setShowOnMenu(itemFolder.isShowOnMenu());
                    
                    // it will load lazily its children
                    folderIds.add(itemFolder.getInode());
                    children.add(nav);
                }
                else if(item instanceof IHTMLPage) {
                	final String httpProtocol = "http://";
                	final String httpsProtocol = "https://";
                    IHTMLPage itemPage=(IHTMLPage)item;
                    ident=APILocator.getIdentifierAPI().find(itemPage);

                    String redirectUri = itemPage.getRedirect();
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier(),languageId);
                    nav.setTitle(itemPage.getTitle());
                    if(UtilMethods.isSet(redirectUri) && !redirectUri.startsWith("/")){
                        if(redirectUri.startsWith(httpsProtocol) || redirectUri.startsWith(httpProtocol)){
                      	  nav.setHref(redirectUri);	
                        }else{
                      	  	if(itemPage.isHttpsRequired())
                      	  		nav.setHref(httpsProtocol+redirectUri);	
                    		else	
                    			nav.setHref(httpProtocol+redirectUri);
                        }
                      	
                      }else{
                      	nav.setHref(ident.getURI());
                      }
                    nav.setOrder(itemPage.getMenuOrder());
                    nav.setType("htmlpage");
                    nav.setPermissionId(itemPage.getPermissionId());
                    nav.setShowOnMenu(itemPage.isShowOnMenu());
                    if(!itemPage.isContent() || (itemPage.isContent() && (itemPage.getLanguageId() == languageId) )) {
                    	children.add(nav);
                    }
                }
                else if(item instanceof Link) {
                    Link itemLink=(Link)item;
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier(),languageId);
                    if(itemLink.getLinkType().equals(LinkType.CODE.toString()) && LinkType.CODE.toString() !=null  ) {
                        nav.setCodeLink(itemLink.getLinkCode());
                    }
                    else {
                        nav.setHref(itemLink.getWorkingURL());
                    }
                    nav.setTitle(itemLink.getTitle());
                    nav.setOrder(itemLink.getSortOrder());
                    nav.setType("link");
                    nav.setTarget(itemLink.getTarget());
                    nav.setPermissionId(itemLink.getPermissionId());
                    nav.setShowOnMenu(itemLink.isShowOnMenu());
                    children.add(nav);
                }
                else if(item instanceof IFileAsset) {
                    IFileAsset itemFile=(IFileAsset)item;
                    ident=APILocator.getIdentifierAPI().find(itemFile.getPermissionId());
                    NavResult nav=new NavResult(folder.getInode(),host.getIdentifier(),languageId);
                    nav.setTitle(itemFile.getFriendlyName());
                    nav.setHref(ident.getURI());
                    nav.setOrder(itemFile.getMenuOrder());
                    nav.setType("file");
                    nav.setPermissionId(itemFile.getPermissionId());
                    nav.setShowOnMenu(itemFile.isShowOnMenu());
                    children.add(nav);
                }
            }

            CacheLocator.getNavToolCache().putNav(host.getIdentifier(), folder.getInode(), result, languageId);
            
            return result;
        }
    }
    
    /**
     * Pass the level of the nav you wish to
     * retrieve, based on the current path, 
     * level 0 being the root
     * @param level
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public NavResult getNav(int level) throws DotDataException, DotSecurityException {
        if(level<1) return getNav("/");
        

        String reqPath = getNav().getHref();

    	String[] levels = reqPath.split("/");
    	
    	
    	if(level+1>levels.length)return null;

    	StringBuffer sw = new StringBuffer();
    	
    	for(int i=1;i<=level;i++){
    		sw.append("/");
    		sw.append(levels[i]);
    	}
    	String path=sw.toString();

        return getNav(path);
    }
    
    public NavResult getNav() throws DotDataException, DotSecurityException {
    	return getNav((String) request.getAttribute("javax.servlet.forward.request_uri"));
    }
    
    public NavResult getNav(String path) throws DotDataException, DotSecurityException {
        
        Host host=getHostFromPath(path);
        
        if(host==null)
        	host = currenthost;
        
        return getNav(host,path);
    }
    
    public NavResult getNav(String path, long languageId) throws DotDataException, DotSecurityException {
        
    	Host host=getHostFromPath(path);

    	if(host==null)
    		host = currenthost;

        return getNav(host,path,languageId, systemUser);
    }
    
    private Host getHostFromPath(String path) throws DotDataException, DotSecurityException{
    	if(path.startsWith("//")) {
            List<RegExMatch> find = RegEX.find(path, "^//(\\w+)/(.+)");
            if(find.size()==1) {
                String hostname=find.get(0).getGroups().get(0).getMatch();
                path="/"+find.get(0).getGroups().get(1).getMatch();
                return APILocator.getHostAPI().findByName(hostname, systemUser, false);
            }
        }
    	
    	return null;
    }
}
