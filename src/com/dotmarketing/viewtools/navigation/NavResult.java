package com.dotmarketing.viewtools.navigation;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.tools.view.tools.ViewRenderTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.VelocityUtil;

public class NavResult {
    private String title;
    private String href;
    private int order;
    private boolean active;
    
    private String internalPath;
    private String internalHostId;
    
    public NavResult(String hostId, String path) {
        internalPath=path;
        internalHostId=hostId;
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

    public void setHref(String href) {
        this.href = href;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<NavResult> getChildren() throws DotDataException, DotSecurityException {
        if(internalPath!=null && internalHostId!=null) {
            Host host=APILocator.getHostAPI().find(internalHostId, APILocator.getUserAPI().getAnonymousUser(), true);
            return NavTool.getNav(host, internalPath);
        }
        return new ArrayList<NavResult>();
    }


    public String toString() {
        String titleToShow;
        try {
            titleToShow=getTitle();
        } catch (Exception e) {
            titleToShow=title;
        }
        return "<a href='"+getHref()+"' title='"+titleToShow+"'>"+titleToShow+"</a>";
    }    
}
