package com.dotcms.rendering.velocity.servlet;

import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectWriter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * {@link VelocityModeHandler} to render a page into {@link com.dotmarketing.util.PageMode#NAVIGATE_EDIT_MODE}, this
 * is the default mode when in HttpSession is set EDIT_MODE but not any mode is set into HttpRequest
 */
public class VelocityNavigateEditMode  extends VelocityModeHandler {

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected final String uri;
    private final Host host;
    private final User user;

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

    public VelocityNavigateEditMode(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final String uri,
                                    final Host host) {
        super();
        this.request = request;
        this.response = response;
        this.uri = uri;
        this.host = host;
        this.user = WebAPILocator.getUserWebAPI().getUser(request);
    }

    public VelocityNavigateEditMode(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }

    @Override
    void serve() throws DotDataException, IOException, DotSecurityException {
        serve(response.getOutputStream());
    }

    @Override
    public void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {
        final User user = APILocator.getLoginServiceAPI().getLoggedInUser();

        final PageMode mode = this.getMode();

        final PageView htmlPageAssetRendered = htmlPageAssetRenderedAPI.getPageRendered(this.request,
                this.response, user, this.uri, mode);
        final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        final String renderedPageString = objectWriter.writeValueAsString(htmlPageAssetRendered)
                .replace("</script>", "\\</script\\>");
        this.response.getOutputStream().write(String.format(JS_CODE, renderedPageString).getBytes());
    }

    private PageMode getMode() {
        final PageMode currentMode = PageMode.get(request);
        return currentMode.showLive ? currentMode :
                APILocator.getHTMLPageAssetRenderedAPI().getDefaultEditPageMode(user, request, uri);
    }
}
