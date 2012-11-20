package com.dotmarketing.viewtools.navigation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.tools.ViewRenderTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.VelocityServlet;
import com.liferay.portal.model.User;

public class NavResult implements Iterable<NavResult> {
    private String title;
    private String href;
    private int order;
    private boolean hrefVelocity;
    private String parent;
    
    private String hostId;
    private String folderId;
    private List<String> childrenFolderIds;
    private List<NavResult> children;
    
    public NavResult(String parent, String hostId, String folderId) {
        this.hostId=hostId;
        this.folderId=folderId;
        this.parent=parent;
        hrefVelocity=false;
        title=href="";
        order=0;
    }
    
    public NavResult(String parent) {
        this(parent,null,null);
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
        return hrefVelocity ?
            UtilMethods.evaluateVelocity(href, VelocityServlet.velocityCtx.get())
            : 
            href;
    }

    public void setHref(String href) {
        this.href = href;
        this.hrefVelocity=false;
    }
    
    public void setHrefVelocity(String vtl) {
        this.href=vtl;
        this.hrefVelocity=true;
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
        if(req!=null)
            return !hrefVelocity && req.getRequestURI().contains(href);
        else
            return false;
    }

    public void setChildren(List<NavResult> children) {
        this.children=children;
    }
    
    public boolean isFolder() {
        return hostId!=null && folderId!=null;
    }
    
    public List<NavResult> getChildren() throws Exception {
        if(children==null && hostId!=null && folderId!=null) {
            // lazy loadinge children
            User user=APILocator.getUserAPI().getAnonymousUser();
            Host host=APILocator.getHostAPI().find(hostId, user, true);
            Folder folder=APILocator.getFolderAPI().find(folderId, user, true);
            Identifier ident=APILocator.getIdentifierAPI().find(folder);
            NavResult lazyMe=NavTool.getNav(host, ident.getPath());
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
                    NavResult ff=new NavResult(folderId,nn.hostId,nn.folderId);
                    ff.setTitle(nn.getTitle());
                    ff.setHref(nn.getHref());
                    ff.setOrder(nn.getOrder());
                    list.add(ff);
                }
                else {
                    list.add(nn);
                }
            }
            return list;
        }
        else
            return new ArrayList<NavResult>();
    }
    
    public String getParentPath() throws DotDataException, DotSecurityException {
        if(parent==null) return null; // no parent! I'm the root folder
        User user=APILocator.getUserAPI().getAnonymousUser();
        Folder folder=APILocator.getFolderAPI().find(parent, user, true);
        Identifier ident=APILocator.getIdentifierAPI().find(folder);
        return ident.getURI();
    }
    
    public String toString() {
        if(!hrefVelocity) {
            String titleToShow;
            try {
                titleToShow=getTitle();
            } catch (Exception e) {
                titleToShow=title;
            }
            return "<a href='"+getHref()+"' title='"+titleToShow+"'>"+titleToShow+"</a>";
        }
        else {
            return getHref();
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

}
