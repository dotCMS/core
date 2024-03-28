package com.dotcms.rendering.velocity.servlet;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link VelocityModeHandler} to render a page into {@link com.dotmarketing.util.PageMode#NAVIGATE_EDIT_MODE}, this
 * is the default mode when in HttpSession is set EDIT_MODE but not any mode is set into HttpRequest
 */
public class VelocityNavigateEditMode  extends VelocityModeHandler {

    private final User user;
    final String uri;
    private final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI = APILocator.getHTMLPageAssetRenderedAPI();

    private final static String JS_CODE =
            "<script type=\"text/javascript\">\n" +
                "var customEvent = window.top.document.createEvent('CustomEvent');\n" +
                "customEvent.initCustomEvent('ng-event', false, false,  {\n" +
                "            name: 'load-edit-mode-page',\n" +
                "            data: %s" +
                "});\n" +
                "window.top.document.dispatchEvent(customEvent);" +
            "</script>";


    @Deprecated
    public VelocityNavigateEditMode(final HttpServletRequest request, final HttpServletResponse response, final String uri, final Host host) {
        this(
                request,
                response,
                VelocityModeHandler.getHtmlPageFromURI(PageMode.get(request), request, uri, host),
                host
        );
    }

    protected VelocityNavigateEditMode(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final IHTMLPage htmlPage,
            final Host host) {

        super(request, response, htmlPage, host);
        uri = CMSUrlUtil.getCurrentURI(request);
        this.setMode(this.getMode());
        this.user = WebAPILocator.getUserWebAPI().getUser(request);
    }

    @Override
    void serve() throws DotDataException, IOException, DotSecurityException {
        serve(response.getOutputStream());
    }

    @Override
    public void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {
        final User user = APILocator.getLoginServiceAPI().getLoggedInUser();

        final PageMode mode = this.getMode();

        final PageView htmlPageAssetRendered = htmlPageAssetRenderedAPI.getPageRendered(
                PageContextBuilder.builder()
                    .setUser(user)
                    .setPageUri(uri)
                    .setPageMode(mode)
                    .build(),
                request,
                response
        );
        final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        final String renderedPageString = objectWriter.writeValueAsString(htmlPageAssetRendered)
                .replaceAll("(?i)</script>", "\\\\</script\\\\>");
        out.write(String.format(JS_CODE, renderedPageString).getBytes());
    }

    private PageMode getMode() {
        final PageMode currentMode = PageMode.get(request);
        return currentMode.showLive ? currentMode :
                // The page could be created not only on the page render uri, but also in any other uri, this means the request uri is meaning less b/c does not represents a page
                Try.of(()->APILocator.getHTMLPageAssetRenderedAPI().getDefaultEditPageMode(user, request, uri))
                        .getOrElse(()->APILocator.getHTMLPageAssetRenderedAPI().getDefaultEditPageMode(user, request, this.getPageURI()));
    }

    private String getPageURI () {

        return Try.of(()-> this.htmlPage.getURI()).getOrElseThrow(DotRuntimeException::new);
    }
    
}
