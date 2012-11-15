package com.dotmarketing.viewtools.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.tools.ViewRenderTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.VelocityServlet;

public class NavResult {
    private String title;
    private String href;
    private int order;
    private boolean hrefVelocity;
    
    private String internalPath;
    private String internalHostId;
    
    public NavResult(String hostId, String path) {
        internalPath=path;
        internalHostId=hostId;
        hrefVelocity=false;
        title=href="";
        order=0;
    }
    
    public NavResult() {
        this(null,null);
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

    public List<NavResult> getChildren() throws DotDataException, DotSecurityException {
        if(internalPath!=null && internalHostId!=null) {
            Host host=APILocator.getHostAPI().find(internalHostId, APILocator.getUserAPI().getAnonymousUser(), true);
            return NavTool.getNav(host, internalPath);
        }
        return new ArrayList<NavResult>();
    }
    
    public NavResult getParent() {
        // TODO: finish this
        return null;
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
}
