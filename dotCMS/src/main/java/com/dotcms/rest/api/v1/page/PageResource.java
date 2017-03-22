package com.dotcms.rest.api.v1.page;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.liferay.portal.model.User;

@Path("/v1/pages")
public class PageResource {

    private final HTMLPageAssetAPI pageAPI;
    private final HostAPI hostAPI;

    private final WebResource webResource;


    public PageResource() {
        this(APILocator.getHTMLPageAssetAPI(), APILocator.getHostAPI(),new WebResource());
    }



    @VisibleForTesting
    protected PageResource(HTMLPageAssetAPI pageApi, HostAPI hostApi, WebResource webResource) {
        this.pageAPI = pageApi;
        this.hostAPI = hostApi;
        this.webResource = webResource;
    }

    /**
     * <p>Returns a JSON representation of a page
     * <p/>
     * Usage: /page/{hostOrFolderIdentifier}
     */
    @GET
    @JSONP
    @Path("/page/{pageId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Map<String, String> list(@Context HttpServletRequest request, @PathParam("pageId") String pageId) {

        Map<String, String> hash = Maps.newHashMap();


        return hash;
    }

    /**
     * <p>Returns a JSON representation of the Rule with the given ruleId
     * <p/>
     * Usage: GET api/rules-engine/sites/{siteId}/rules/{ruleId}
     */
    @GET
    @JSONP
    @Path("/rules/{ruleId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public String self(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId) {
        Map<String, String> hash = Maps.newHashMap();


        return "asd";
    }


    private User getUser(@Context HttpServletRequest request) {
        return webResource.init(true, request, true).getUser();
    }

}
