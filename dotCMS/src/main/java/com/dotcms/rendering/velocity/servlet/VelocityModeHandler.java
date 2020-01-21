package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.beans.Identifier;
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

public abstract class VelocityModeHandler {

    protected static final String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");
    protected static final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    protected static final VisitorAPI visitorAPI = APILocator.getVisitorAPI();

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected PageMode mode;
    protected final IHTMLPage htmlPage;
    protected final Host host;

    private static final Map<PageMode, Function> pageModeVelocityMap =ImmutableMap.<PageMode, VelocityModeHandler.Function>builder()
            .put(PageMode.PREVIEW_MODE, VelocityPreviewMode::new)
            .put(PageMode.EDIT_MODE, VelocityEditMode::new)
            .put(PageMode.LIVE, VelocityLiveMode::new)
            .put(PageMode.ADMIN_MODE, VelocityAdminMode::new)
            .put(PageMode.NAVIGATE_EDIT_MODE, VelocityNavigateEditMode::new)
            .build();

    /**
     * @deprecated use {@link VelocityModeHandler#modeHandler(PageMode, HttpServletRequest, HttpServletResponse, String, Host)} instead
     * @param request
     * @param response
     * @param uri
     * @param host
     */
    @Deprecated
    public VelocityModeHandler(final HttpServletRequest request, final HttpServletResponse response, final String uri, final Host host) {
        this(
                request,
                response,
                VelocityModeHandler.getHtmlPageFromURI(PageMode.get(request), request, uri, host),
                host
        );
    }

    protected VelocityModeHandler(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final IHTMLPage htmlPage,
            final Host host) {
        this.request = request;
        this.response = response;
        this.htmlPage = htmlPage;
        this.host = host;
    }

    public void setMode(final PageMode mode) {
        this.mode = mode;
    }

    protected void processException(final User user, final String name, final ParseErrorException e) {

        Logger.warn(this, "The resource " + name + " has a parse error, msg: " + e.getMessage());
        Logger.warn(this, "ParseErrorException on the page: " + name + ", with the user: " + user.getNickName(), e);
    }

    @FunctionalInterface
    private interface Function {
        VelocityModeHandler apply(HttpServletRequest request, HttpServletResponse response, IHTMLPage htmlPage, Host host);
    }

    abstract void serve() throws DotDataException, IOException, DotSecurityException;

    abstract void serve(OutputStream out) throws DotDataException, IOException, DotSecurityException;


    public final String eval() {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
            serve(out);
            return new String(out.toByteArray());
        } catch (DotDataException | IOException | DotSecurityException e) {
            Logger.debug(VelocityModeHandler.class, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    public static final VelocityModeHandler modeHandler(final PageMode mode, final HttpServletRequest request, final HttpServletResponse response, final String uri, final Host host) {
        // Find the current language
        final IHTMLPage htmlPage= getHtmlPageFromURI(mode, request, uri, host);
        return pageModeVelocityMap.get(mode).apply(request, response, htmlPage, host);
    }

    protected static IHTMLPage getHtmlPageFromURI(final PageMode mode, final HttpServletRequest request, final  String uri, final Host host) {
        final long langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

        try {
            // now we check identifier cache first (which DOES NOT have a 404 cache )
            final Identifier id = APILocator.getIdentifierAPI().find(host, uri);

            return APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(id, langId, mode.showLive,
                    APILocator.systemUser(), mode.respectAnonPerms);

        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    public static final VelocityModeHandler modeHandler(final IHTMLPage htmlPage,
                                                        final PageMode mode,
                                                        final HttpServletRequest request,
                                                        final HttpServletResponse response,
                                                        final Host host) {
        return pageModeVelocityMap.get(mode).apply(request, response, htmlPage, host);
    }

    public static final VelocityModeHandler modeHandler(final PageMode mode, final HttpServletRequest request, final HttpServletResponse response) {
        return modeHandler(mode, request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }

    public final Template getTemplate(final IHTMLPage page, final PageMode mode) {

        return VelocityUtil.getEngine().getTemplate(mode.name() + File.separator + page.getIdentifier() + "_"
                + page.getLanguageId() + "." + VelocityType.HTMLPAGE.fileExtension);
    }

}
