package com.dotcms.rendering.proxy.rendermode;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.rendering.RenderModeHandler;
import com.dotcms.rendering.RenderModeHandler.Function;
import com.dotcms.rendering.RenderModeMapper;
import com.dotcms.repackage.jersey.repackaged.com.google.common.collect.ImmutableMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class ProxyRenderModeMapper implements RenderModeMapper {

    private static final Map<PageMode, Function> pageModeMap =ImmutableMap.<PageMode, RenderModeHandler.Function>builder()
            .put(PageMode.PREVIEW_MODE, ProxyEditMode::new)
            .put(PageMode.EDIT_MODE, ProxyEditMode::new)
            .put(PageMode.LIVE, ProxyEditMode::new)
            .put(PageMode.ADMIN_MODE, ProxyEditMode::new)
            .put(PageMode.NAVIGATE_EDIT_MODE, ProxyEditMode::new)
            .build();
    
    
    
    
    @Override
    public Map<PageMode, Function> getModMop() {

        return pageModeMap;
    }

    @Override
    public boolean useModes(HttpServletRequest request) {
        
        try {
            Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            return(host!=null && host.getHostname()!=null && host.getHostname().contains("spa."));
            
        } catch (DotDataException | PortalException | SystemException | DotSecurityException e) {
            Logger.warn(this.getClass(), "unable to find host:" + e.getMessage());
        }
        return false;
    }

}
