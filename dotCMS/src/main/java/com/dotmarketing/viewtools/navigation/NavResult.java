package com.dotmarketing.viewtools.navigation;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.VelocityServlet;
import com.liferay.portal.model.User;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.tools.ViewRenderTool;

import javax.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NavResult implements Iterable<NavResult>, Permissionable, Serializable {

    private Long languageId;
    private String title;
    private String href;
    private int order;
    private String codeLink;
    private String parent;
    private String type;
    private String permissionId;
    private String target;
    private boolean showOnMenu;
    public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	private String hostId;
    private String folderId;
    private List<String> childrenFolderIds;
    private List<NavResult> children;
    private User sysuser=null;
    private boolean checkPermissions;
    
    public NavResult(String parent, String hostId, String folderId, Long languageId) {
        this.hostId=hostId;
        this.folderId=folderId;
        this.parent=parent;
        this.languageId=languageId;

        title=href="";
        order=0;
        checkPermissions=Config.getBooleanProperty("ENABLE_NAV_PERMISSION_CHECK",false);
        try {
            sysuser=APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            Logger.warn(this, e.getMessage(), e);
        }
        
    }
    
    public NavResult(String parent, String host, Long languageId) {
        this(parent,host,null,languageId);
    }
    
    public String getTitle() throws Exception {
        if(title.startsWith("$text.")) {
            ViewRenderTool render=new ViewRenderTool();
            render.setVelocityEngine(VelocityUtil.getEngine());
            return render.eval(title);
        }
        else {
            return title;
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getHref() {
        return href;
    }

    public boolean isShowOnMenu() {
		return showOnMenu;
	}

	public void setShowOnMenu(boolean showOnMenu) {
		this.showOnMenu = showOnMenu;
	}

	public void setHref(String href) {
        this.href = href;
    }
    
    public boolean isCodeLink() {
    	return this.codeLink !=null;
    }
    
    
    public String getCodeLink() {
    	if(this.codeLink !=null  && (this.codeLink.contains("$") || this.codeLink.contains("#"))){ 
    		return   UtilMethods.evaluateVelocity(codeLink, VelocityServlet.velocityCtx.get());
    	}else{
			return codeLink;
		}
	}

	public void setCodeLink(String codeLink) {
		this.codeLink = codeLink;
	}



    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActive() {
        Context ctx=(VelocityContext) VelocityServlet.velocityCtx.get();
        HttpServletRequest req=(HttpServletRequest) ctx.get("request");
        if(req!=null){
            //We exclude the page name from the Request URI so we can check if page's parent object is the real active object
            String reqURI = req.getRequestURI();
            String parentPath = reqURI.substring(0,reqURI.lastIndexOf("/"));
            if(!parentPath.endsWith("/"))
                //Adding a slash at the end of the path, so it avoids false positives
                //when two or more paths from the same level starts with the same name
                parentPath = parentPath + "/";
            //If the current item is a folder, we check if it's part of current URI
            if(isFolder() && !href.endsWith("/")){
                String tempHref = href + "/";
                return parentPath.startsWith(tempHref);
            } else {
                //If it's a page, we check instead if it's the current URI 
                return !isCodeLink() && href.equalsIgnoreCase(reqURI);
            }
        }
        else
            return false;
    }

    public void setChildren(List<NavResult> children) {
        this.children=children;
    }
    
    public boolean isFolder() {
        return folderId!=null;
    }
    
    public List<NavResult> getChildren() throws Exception {
        if(children==null && hostId!=null && folderId!=null) {
            // lazy loadinge children
            Host host=APILocator.getHostAPI().find(hostId, sysuser, true);
            Folder folder=APILocator.getFolderAPI().find(folderId, sysuser, true);
            Identifier ident=APILocator.getIdentifierAPI().find(folder);
            NavResult lazyMe=NavTool.getNav(host, ident.getPath(), languageId, sysuser);
            children=lazyMe.getChildren();
            childrenFolderIds=lazyMe.getChildrenFolderIds();
        }
        if(children!=null) {
            ArrayList<NavResult> list=new ArrayList<NavResult>();
            for(NavResult nn : children) {
                if(nn.isFolder()) {
                    // for folders we avoid returning the same instance
                    // it could be changed elsewhere and we need it to
                    // load its children lazily
                    NavResult ff=new NavResult(folderId,nn.hostId,nn.folderId,nn.languageId);
                    ff.setTitle(nn.getTitle());
                    ff.setHref(nn.getHref());
                    ff.setOrder(nn.getOrder());
                    ff.setType(nn.getType());
                    ff.setPermissionId(nn.getPermissionId());
                    ff.setShowOnMenu(nn.isShowOnMenu());
                    list.add(ff);
                }
                else {
                    list.add(nn);
                }
            }
            
            if(checkPermissions) {
                // now filtering permissions
                List<NavResult> allow=new ArrayList<NavResult>(list.size());
                Context ctx=(VelocityContext) VelocityServlet.velocityCtx.get();
                HttpServletRequest req=(HttpServletRequest) ctx.get("request");
                User currentUser=WebAPILocator.getUserWebAPI().getLoggedInUser(req);
                if(currentUser==null) currentUser=APILocator.getUserAPI().getAnonymousUser();
                for(NavResult nv : list) {
                    try {
                    if(APILocator.getPermissionAPI().doesUserHavePermission(nv, PermissionAPI.PERMISSION_READ, currentUser)) {
                        allow.add(nv);
                    }
                    }catch(Exception ex) {
                        Logger.error(this, ex.getMessage(), ex);
                    }
                }
                return allow;
            }
            else
                return list;
        }
        else {
            return new ArrayList<NavResult>();
        }
    }
    
    public String getParentPath() throws DotDataException, DotSecurityException {
        if(parent==null) return null; // no parent! I'm the root folder
        if(parent.equals(FolderAPI.SYSTEM_FOLDER)) return "/";
        Folder folder=APILocator.getFolderAPI().find(parent, sysuser, true);
        Identifier ident=APILocator.getIdentifierAPI().find(folder);
        return ident.getURI();
    }

    public NavResult getParent() throws DotDataException, DotSecurityException {
        String path=getParentPath();
        if(path!=null) {
            return NavTool.getNav(APILocator.getHostAPI().find(hostId,sysuser,true), path, languageId, sysuser);
        }
        else return null;
    }
    
    public String toString() {
        if(!isCodeLink()) {
            String titleToShow;
            try {
                titleToShow=getTitle();
            } catch (Exception e) {
                titleToShow=title;
            }
            return "<a href='"+getHref()+"' title='"+titleToShow+"'>"+titleToShow+"</a>";
        }
        else {
            return getCodeLink();
        }
    }    
    
    public List<String> getChildrenFolderIds() {
        return childrenFolderIds!=null ? childrenFolderIds : new ArrayList<String>();
    }

    public void setChildrenFolderIds(List<String> childrenFolderIds) {
        this.childrenFolderIds = childrenFolderIds;
    }

    @Override
    public Iterator<NavResult> iterator() {
        try {
            return getChildren().iterator();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
    	if(type.equals("link"))
            return target;
    	else
    		return "_self";
    }

    public void setTarget(String target) {
    	
        this.target = target;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    
    /// Permissionable methods ///
    
    @Override
    public String getOwner() {
        return sysuser.getUserId();        
    }

    @Override
    public void setOwner(String owner) {}

    @Override
    public List<PermissionSummary> acceptedPermissions() {
        return Arrays.asList(new PermissionSummary[] {new PermissionSummary("READ", "READ", PermissionAPI.PERMISSION_READ)});
    }

    @Override
    public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return null;
    }

    @Override
    public Permissionable getParentPermissionable() throws DotDataException {
        try {
            if(type.equals("htmlpage")){
                Identifier ident=APILocator.getIdentifierAPI().find(permissionId);
                if(Identifier.ASSET_TYPE_CONTENTLET.equalsIgnoreCase(ident.getAssetType()))
                    return APILocator.getContentletAPI().findContentletByIdentifier(permissionId, true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), sysuser, false).getParentPermissionable();
                else
                    return APILocator.getHTMLPageAPI().loadLivePageById(permissionId, sysuser, false).getParentPermissionable();
            }
            if(type.equals("folder"))
                return APILocator.getFolderAPI().find(folderId, sysuser, false).getParentPermissionable();
            if(type.equals("link"))
                return APILocator.getMenuLinkAPI().findWorkingLinkById(permissionId, sysuser, false).getParentPermissionable();
            if(type.equals("file")) {
                Identifier ident=APILocator.getIdentifierAPI().find(permissionId);
                if(Identifier.ASSET_TYPE_CONTENTLET.equalsIgnoreCase(ident.getAssetType()))
                    return APILocator.getContentletAPI().findContentletByIdentifier(permissionId, true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), sysuser, false).getParentPermissionable();
                else
                    return APILocator.getFileAPI().getWorkingFileById(permissionId, sysuser, false).getParentPermissionable();
            }
            return null;
        } catch (DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPermissionType() {
        return parent!=null ? Folder.class.getCanonicalName() : Host.class.getCanonicalName();
    }

    @Override
    public boolean isParentPermissionable() {
        return isFolder();
    }
    
    public String getEnclosingPermissionClassName() {
        if(type.equals("htmlpage"))
            return IHTMLPage.class.getCanonicalName();
        if(type.equals("link"))
            return Link.class.getCanonicalName();
        if(type.equals("folder"))
            return Folder.class.getCanonicalName();
        if(type.equals("file"))
            return File.class.getCanonicalName();
        throw new IllegalStateException("unknow internal type "+type); // we shouldn't reach this point
    }

}
