package com.dotmarketing.menubuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.WebKeys;

public class CrumbTrailListBuilder implements ViewTool {

	protected HttpServletRequest request;
    protected HttpServletResponse response;

    public List getCrumbTrail(Host host) throws Exception {
        
        // if we have a crumbtail already, use it
        if (request.getAttribute(WebKeys.CRUMB_TRAIL) != null) {
            return (List) request.getAttribute(WebKeys.CRUMB_TRAIL);
        }
        // set up the home
        List list = new ArrayList();
        Map map = new HashMap();
        map.put("title", "Home");
        map.put("url", "/");
        list.add(map);
        FolderAPI folderAPI = APILocator.getFolderAPI();
        
        Identifier id = APILocator.getIdentifierAPI().find((String) request.getAttribute("idInode"));
        HTMLPage htmlPage = (HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(id,APILocator.getUserAPI().getSystemUser(),false);
        Folder folder = folderAPI.findParentFolder(htmlPage, APILocator.getUserAPI().getSystemUser(), false);
        
        String folderPath = APILocator.getIdentifierAPI().find(folder).getPath(); 


        if(!InodeUtils.isSet(folder.getInode()) ||!InodeUtils.isSet(htmlPage.getInode())){
            map = new HashMap();
            map.put("title", "Page Not Found");
            map.put("url", "");
            map.put("theEnd", "true");
            list.add(map);
            return list;
        }
        
        map = new HashMap();
        map.put("title", htmlPage.getTitle());
        map.put("url", folderPath + htmlPage.getPageUrl());
        map.put("theEnd", "true");
        list.add(map);
        
        // if we are an index page, skip to a folder below
        if (htmlPage.getPageUrl().startsWith("index") || folderPath.startsWith("/global")) {
            folder = folderAPI.findParentFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
        }

        while (!InodeUtils.isSet(folder.getInode())) {
            if (folder.getInode().equalsIgnoreCase(host.getInode()) || folderPath.startsWith("/home")) {
                break;
            }
            map = new HashMap();
            map.put("title", folder.getTitle());
            map.put("url", folderPath);
            if (folder.isShowOnMenu()) {
                list.add(1, map);
            }
            folder = folderAPI.findParentFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
        }

        return list;
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
    }
}