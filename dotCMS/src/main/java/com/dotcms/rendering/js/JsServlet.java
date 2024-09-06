package com.dotcms.rendering.js;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotcms.rendering.velocity.servlet.VelocityServlet;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.UserWebAPIImpl;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles files with extension .js and runs those when accessed
 * @author jsanca
 */
public class JsServlet extends HttpServlet {

    private final UserWebAPIImpl userApi;

    @VisibleForTesting
    public JsServlet(final UserWebAPI userApi) {
        this.userApi = (UserWebAPIImpl)userApi;
    }

    public JsServlet() {
        this(WebAPILocator.getUserWebAPI());
    }

    @Override
    protected final void service(final HttpServletRequest request,
                                 final HttpServletResponse response) throws ServletException, IOException {

        final String currentUri = CMSUrlUtil.getCurrentURI(request);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

        final User user = (userApi.getLoggedInUser(request)!=null)
                ? userApi.getLoggedInUser(request)
                : userApi.getLoggedInFrontendUser(request) !=null
                ? userApi.getLoggedInFrontendUser(request)
                : userApi.getAnonymousUserNoThrow();

        final PageMode mode = VelocityServlet.processPageMode(user, request);

        // if you are hitting the servlet without running through the other filters
        if (currentUri == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JsServlet called without running through the CMS Filter");
            Logger.error(this.getClass(),
                    "You cannot call the JsServlet without passing the requested url via a requestAttribute called  "
                            + Constants.CMS_FILTER_URI_OVERRIDE);
            return;
        }

        // if you are not running ee
        if (!LicenseUtil.isASAllowed()) {
            Logger.error(this, "Enterprise License is required");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(request);

        try (final Reader reader = FileJavascriptReader.getJavascriptReaderFromPath(currentUri, site, currentLanguage, mode, user)) {

            final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ScriptEngineFactory.JAVASCRIPT_ENGINE);
            final Object result       = engine.eval(request, response, reader,
                    new HashMap<>(Map.of("mode", mode,
                            "user", user,
                            "uri", currentUri)));

            final String output = result.toString();
            response.getOutputStream().write(output.getBytes());
        } catch (IllegalStateException state) {
            // Eat this, client disconnect noise
            Logger.debug(this, ()-> "IllegalStateException" + state);
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(),e);
            if(!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Exception Error on template");
            }
        }
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        Logger.info(this.getClass(), "Initializing JavascriptServlet");
    }

}
