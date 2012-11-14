package com.dotmarketing.viewtools.navigation;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class NavTool implements ViewTool {
    
    private Host currenthost=null;
    private User user=null;
    
    @Override
    public void init(Object initData) {
        ViewContext context = (ViewContext) initData;
        try {
            currenthost=WebAPILocator.getHostWebAPI().getCurrentHost(context.getRequest());
            user=APILocator.getUserAPI().getAnonymousUser();
        } catch (Exception e) {
            
        }
    }
    
    protected static List<NavResult> getNav(Host host, String path) throws DotDataException, DotSecurityException {
        List<NavResult> list=new ArrayList<NavResult>();
        User user=APILocator.getUserAPI().getAnonymousUser();
        
        List menuItems;
        if(path.equals("/")) {
            menuItems = APILocator.getFolderAPI().findSubFolders(host, true);
        }
        else {
            Folder folder=APILocator.getFolderAPI().findFolderByPath(path, host, user, true);
            menuItems = APILocator.getFolderAPI().findMenuItems(folder, user, true);
        }
        
        for(Object item : menuItems) {
            if(item instanceof Folder) {
                Folder itemFolder=(Folder)item;
                Identifier ident=APILocator.getIdentifierAPI().find(itemFolder);
                NavResult nav=new NavResult(host.getIdentifier(),ident.getURI());
                nav.setTitle(itemFolder.getTitle());
                nav.setHref(ident.getURI());
                nav.setOrder(itemFolder.getSortOrder());
                list.add(nav);
            }
            else if(item instanceof HTMLPage) {
                HTMLPage itemPage=(HTMLPage)item;
                Identifier ident=APILocator.getIdentifierAPI().find(itemPage);
                NavResult nav=new NavResult();
                nav.setTitle(itemPage.getFriendlyName());
                nav.setHref(ident.getURI());
                nav.setOrder(itemPage.getSortOrder());
                list.add(nav);
            }
            else if(item instanceof Link) {
                Link itemLink=(Link)item;
                NavResult nav=new NavResult();
                if(itemLink.getLinkType().equals(LinkType.CODE.toString())) {
                    nav.setHrefVelocity(itemLink.getLinkCode());
                }
                else {
                    nav.setHref(itemLink.getWorkingURL());
                }
                nav.setTitle(itemLink.getTitle());
                nav.setOrder(itemLink.getSortOrder());
                list.add(nav);
            }
            else if(item instanceof IFileAsset) {
                IFileAsset itemFile=(IFileAsset)item;
                Identifier ident=APILocator.getIdentifierAPI().find(itemFile.getPermissionId());
                NavResult nav=new NavResult();
                nav.setTitle(itemFile.getFriendlyName());
                nav.setHref(ident.getURI());
                nav.setOrder(itemFile.getMenuOrder());
                list.add(nav);
            }
        }
        
        
        return list;
    }
    
    public List<NavResult> getNav() throws DotDataException, DotSecurityException {
        return getNav(null);
    }
    
    public List<NavResult> getNav(String path) throws DotDataException, DotSecurityException {
        
        
        if(!UtilMethods.isSet(path))
            path="/";
        
        Host host=currenthost;
        if(path.startsWith("//")) {
            List<RegExMatch> find = RegEX.find(path, "^//(\\w+)/(.+)");
            if(find.size()==1) {
                String hostname=find.get(0).getGroups().get(0).getMatch();
                path="/"+find.get(0).getGroups().get(1).getMatch();
                host=APILocator.getHostAPI().findByName(hostname, user, true);
            }
        }
        
        return getNav(host,path);
    }
    
}
