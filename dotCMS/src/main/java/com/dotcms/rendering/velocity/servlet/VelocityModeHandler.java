package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.jersey.repackaged.com.google.common.collect.ImmutableMap;
import com.dotcms.visitor.business.VisitorAPI;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;

public abstract class VelocityModeHandler {

    protected static final String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");
    protected static final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    protected static final VisitorAPI visitorAPI = APILocator.getVisitorAPI();

    private static final Map<PageMode, Function> pageModeVelocityMap =ImmutableMap.<PageMode, VelocityModeHandler.Function>builder()
            .put(PageMode.PREVIEW_MODE, VelocityPreviewMode::new)
            .put(PageMode.EDIT_MODE, VelocityEditMode::new)
            .put(PageMode.LIVE, VelocityLiveMode::new)
            .put(PageMode.ADMIN_MODE, VelocityAdminMode::new)
            .put(PageMode.NAVIGATE_EDIT_MODE, VelocityNavigateEditMode::new)
            .build();

    @FunctionalInterface
    private interface Function {
        VelocityModeHandler apply(HttpServletRequest request, HttpServletResponse response, String uri, Host host);
    }


    abstract void serve() throws DotDataException, IOException, DotSecurityException;

    abstract void serve(OutputStream out) throws DotDataException, IOException, DotSecurityException;


    public final String eval() {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
            serve(out);
            return new String(out.toByteArray());
        } catch (DotDataException | IOException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
    
    public static final VelocityModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        return pageModeVelocityMap.get(mode).apply(request, response, uri, host);
    }
    
    public static final VelocityModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response) {
        return pageModeVelocityMap.get(mode).apply(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }

    public final Template getTemplate(IHTMLPage page, PageMode mode) {

        return VelocityUtil.getEngine().getTemplate(mode.name() + File.separator + page.getIdentifier() + "_"
                + page.getLanguageId() + "." + VelocityType.HTMLPAGE.fileExtension);
    }


}
