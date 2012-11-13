package com.dotmarketing.viewtools.navigation;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
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
            user=WebAPILocator.getUserWebAPI().getLoggedInFrontendUser(context.getRequest());
        } catch (Exception e) {
            
        }
    }
    
    public List<NavResult> getNav(String path) throws DotDataException, DotSecurityException {
        List<NavResult> list=new ArrayList<NavResult>();
        
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
        
        if(path.startsWith("/")) {
            List<Folder> folders = APILocator.getFolderAPI().findSubFolders(host, true);
        }
        else {
            Folder folder=APILocator.getFolderAPI().findFolderByPath(path, host, user, true);
            List<Inode> menuItems = APILocator.getFolderAPI().findMenuItems(folder, user, true);
            
            
            List<Folder> folders = APILocator.getFolderAPI().findSubFolders(folder, true);
        }
        return list;
    }
    
}
