package com.dotcms.rendering.velocity.servlet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.personas.model.Persona;
import com.google.common.collect.ImmutableMap;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.velocity.Template;
import org.apache.velocity.exception.ParseErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class VelocityModeHandler {

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected final String uri;
    protected final Host host;
    protected final String personaTagToIncludeContent;

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

    public VelocityModeHandler(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String uri,
            final Host host,
            final String personaTagToIncludeContent) {

        this.request = request;
        this.response = response;
        this.uri = uri;
        this.host = host;
        this.personaTagToIncludeContent = personaTagToIncludeContent;
    }

    protected void processException(final User user, final String name, final ParseErrorException e) {

        Logger.warn(this, "The resource " + name + " has a parse error, msg: " + e.getMessage());
        Logger.warn(this, "ParseErrorException on the page: " + name + ", with the user: " + user.getNickName(), e);
    }

    @FunctionalInterface
    private interface Function {
        VelocityModeHandler apply(HttpServletRequest request, HttpServletResponse response, String uri, Host host, String personaTagToIncludeContent);
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
    
    public static final VelocityModeHandler modeHandler(
            final PageMode mode,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String uri,
            final Host host,
            final String personaTagToIncludeContent) {

        return pageModeVelocityMap.get(mode).apply(request, response, uri, host, personaTagToIncludeContent);
    }
    
    public static final VelocityModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response) {
        return pageModeVelocityMap.get(mode).apply(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request), MultiTree.DOT_PERSONALIZATION_DEFAULT);
    }

    public final Template getTemplate(final IHTMLPage page, final PageMode mode, final String personaTagToIncludeContent) {
        return VelocityUtil.getEngine().getTemplate(mode.name() + File.separator + page.getIdentifier() + "_"
                + page.getLanguageId() + File.separator + personaTagToIncludeContent + "." + VelocityType.HTMLPAGE.fileExtension);
    }
}
