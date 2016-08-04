package com.dotcms.rest.api.v1.site;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Collections.sort;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.util.Logger.error;
import static com.dotcms.rest.api.v1.site.SiteBrowserHelper.checkArchived;
import static com.dotcms.rest.api.v1.site.SiteBrowserHelper.getHostManagerUrl;

/**
 * Site Browser Resource, retrieve the sites and change sites.
 * @author jsanca
 */
@Path("/v1/site")
public class SiteBrowserResource implements Serializable {

    private final static HostNameComparator HOST_NAME_COMPARATOR =
            new HostNameComparator();
    private static final String NO_FILTER = "*";

    private final WebResource webResource;
    private final HostAPI hostAPI;
    private final LayoutAPI layoutAPI;
    private final I18NUtil i18NUtil;

    public SiteBrowserResource() {
        this(new WebResource(),
                APILocator.getHostAPI(),
                APILocator.getLayoutAPI(),
                I18NUtil.INSTANCE);
    }

    @VisibleForTesting
    public SiteBrowserResource(final WebResource webResource,
                               final HostAPI hostAPI,
                               final LayoutAPI layoutAPI,
                               final I18NUtil i18NUtil) {

        this.webResource = webResource;
        this.hostAPI     = hostAPI;
        this.layoutAPI   = layoutAPI;
        this.i18NUtil    = i18NUtil;
    }



    @GET
    @Path ("/filter/{filter}/archived/{archived}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response sites(
                                @Context final HttpServletRequest req,
                                @PathParam("filter")   final String filterParam,
                                @PathParam("archived") final boolean showArchived
                                ) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final List<Map<String, Object>> hostResults;
        final User user = initData.getUser();
        Locale locale = LocaleUtil.getLocale(req);
        final String filter;

        try {

            locale = (null != user && null == locale)?
                    user.getLocale():locale;

            filter = (null != filterParam && filterParam.endsWith(NO_FILTER))?
                    filterParam.substring(0, filterParam.length() - 1):
                    (null != filterParam)? filterParam: StringUtils.EMPTY;

            hostResults = this.hostAPI.findAll(user, Boolean.TRUE)
                        .stream().sorted(HOST_NAME_COMPARATOR)
                        .filter (host ->
                                    !host.isSystemHost() && checkArchived(showArchived, host) &&
                                    (host.getHostname().toLowerCase().startsWith(filter.toLowerCase())))
                        .map    (host -> host.getMap())
                        .collect(Collectors.toList());

            response = Response.ok(new ResponseEntityView
                    (map(   "result",         hostResults
                            //,"hostManagerUrl", getHostManagerUrl(req, this.layoutAPI.loadLayoutsForUser(user)) // NOTE: this is not needed yet.
                            ),
                     this.i18NUtil.getMessagesMap(locale, "select-host",
                         "select-host-nice-message", "Invalid-option-selected",
                         "manage-hosts", "cancel", "Change-Host"))
                    ).build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // sites.


    @PUT
    @Path ("/id/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response switchSite(
            @Context final HttpServletRequest req,
            @PathParam("id")   final String hostId
    ) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, req, true, null); // should logged in
        final HttpSession session = req.getSession();
        final User user = initData.getUser();
        boolean switchDone = false;
        Optional<Host> hostFound = null;

        try {

            if (UtilMethods.isSet(hostId)) {

                // we verified if the host id pass by parameter is one of the user's hosts
                hostFound = this.hostAPI.findAll(user, Boolean.TRUE)
                        .stream().filter (host -> !host.isSystemHost() && hostId.equals(host.getIdentifier()) )
                        .findFirst();

                if (hostFound.isPresent()) {

                    session.setAttribute(
                            com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, hostId);
                    session.removeAttribute(WebKeys.CONTENTLET_LAST_SEARCH);

                    switchDone = true;
                }
            }

            response = (switchDone) ?
                    Response.ok(new ResponseEntityView(map("hostSwitched",
                            switchDone))).build(): // 200
                    Response.status(Response.Status.NOT_FOUND).build();

        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // sites.

} // E:O:F:SiteBrowserResource.
