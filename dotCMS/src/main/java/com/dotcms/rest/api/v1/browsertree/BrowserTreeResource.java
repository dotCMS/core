package com.dotcms.rest.api.v1.browsertree;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.browser.BrowserQueryForm;
import com.dotcms.util.I18NUtil;
import com.liferay.portal.model.User;

/**
 * Created by jasontesser on 9/28/16.
 * @deprecated see {@link com.dotcms.rest.api.v1.browser.BrowserResource#getFolderContent(HttpServletRequest, HttpServletResponse, BrowserQueryForm)}
 */
@Deprecated
@Path("/v1/browsertree")
public class BrowserTreeResource implements Serializable {
    private final WebResource webResource;
    private final BrowserTreeHelper browserTreeHelper;
    private final I18NUtil i18NUtil;

    public BrowserTreeResource() {
        this(new WebResource(),
                BrowserTreeHelper.getInstance(),
                I18NUtil.INSTANCE);
    }

    @VisibleForTesting
    public BrowserTreeResource(final WebResource webResource,
                               final BrowserTreeHelper browserTreeHelper,
                               final I18NUtil i18NUtil) {

        this.webResource = webResource;
        this.browserTreeHelper  = browserTreeHelper;
        this.i18NUtil    = i18NUtil;
    }

    /**
     * @deprecated see {@link com.dotcms.rest.api.v1.browser.BrowserResource#getFolderContent(HttpServletRequest, HttpServletResponse, BrowserQueryForm)}
     * @param httpRequest
     * @param httpResponse
     * @param sitename
     * @return
     */
    @Deprecated
    @GET
    @Path ("/sitename/{sitename}/uri/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadAssetsUnder(
            @Context final HttpServletRequest  httpRequest,
            @Context final HttpServletResponse httpResponse,
            @PathParam("sitename")   final String sitename) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpRequest, httpResponse, true, null);
        final User user = initData.getUser();
        final List<Map<String, Object>> assetResults;



        return Response.ok().build(); // 200
    }

    /**
     * @deprecated see {@link com.dotcms.rest.api.v1.browser.BrowserResource#getFolderContent(HttpServletRequest, HttpServletResponse, BrowserQueryForm)}
     * @param httpRequest
     * @param httpResponse
     * @param sitename
     * @param uri
     * @return
     */
    @Deprecated
    @GET
    @Path ("/sitename/{sitename}/uri/{uri : .+}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadAssetsUnder(
            @Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @PathParam("sitename")   final String sitename,
            @PathParam("uri") final String uri
    ) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpRequest, httpResponse, true, null);
        final User user = initData.getUser();
        final List<Map<String, Object>> assetResults;

        return Response.ok().build(); // 200
    }

}
