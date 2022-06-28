package com.dotcms.rest.api.v1.portlet;

import static com.dotcms.util.CollectionsUtils.map;
import static com.liferay.portal.model.Portlet.DATA_VIEW_MODE_KEY;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.portal.DotPortlet;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.JSONP;

/**
 * This Resource is for create custom portlets. These kind of custom portlets are to show diff types
 * or content (content types or base types).
 */
@Path("/v1/portlet")
@SuppressWarnings("serial")
public class PortletResource implements Serializable {

  private final WebResource webResource;
  private final PortletAPI portletApi;

  /**
   * Default class constructor.
   */
  public PortletResource() {
    this(new WebResource(new ApiProvider()), APILocator.getPortletAPI());
  }

  @VisibleForTesting
  public PortletResource(WebResource webResource, PortletAPI portletApi) {
    this.webResource = webResource;
    this.portletApi = portletApi;
  }

    /**
     * Creates a Portlet for a given name content-types and Display view
     * @param request
     * @param formData
     * @return
     */
  @POST
  @Path("/custom")
  @JSONP
  @NoCache
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public final Response createContentPortlet(@Context final HttpServletRequest request, final CustomPortletForm formData) {

    final InitDataObject initData = new WebResource.InitBuilder(webResource)
            .requiredBackendUser(true)
            .requiredFrontendUser(false)
            .requestAndResponse(request, null)
            .rejectWhenNoUser(true)
            .requiredPortlet("roles")
            .init();

    Response response = null;

    try {

      final Portlet contentPortlet = portletApi.findPortlet("content");

      final Map<String, String> initValues = new HashMap<>(contentPortlet.getInitParams());
      initValues.put("name", formData.portletName);
      initValues.put("baseTypes", formData.baseTypes);
      initValues.put("contentTypes", formData.contentTypes);
      initValues.put(DATA_VIEW_MODE_KEY, formData.dataViewMode);

      final Portlet newPortlet = APILocator.getPortletAPI()
          .savePortlet(new DotPortlet(formData.portletId, contentPortlet.getPortletClass(), initValues), initData.getUser());

      return Response.ok(new ResponseEntityView(map("portlet", newPortlet.getPortletId()))).build();

    } catch (Exception e) {
      response = ResponseUtil.mapExceptionResponse(e);
    }

    return response;
  }

    /**
     * This endpoint links a layout with a portlet Security is considered so the user must have
     * roles on the layout otherwise an unauthorized code is returned.
     * @param request
     * @param portletId
     * @param layoutId
     * @return
     * @throws DotDataException
     */
    @PUT
    @Path("/custom/{portletId}/_addtolayout/{layoutId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response addContentPortletToLayout(@Context final HttpServletRequest request,
            @PathParam("portletId") final String portletId,
            @PathParam("layoutId") final String layoutId)
            throws DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet("roles")
                .init();

        final User user = initData.getUser();

        final PortletAPI portletAPI = APILocator.getPortletAPI();
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();

        if (!portletAPI.canAddPortletToLayout(portletId)) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                            user.getUserId(), "custom.content.portlet.add.restricted", portletId);
        }

        final Portlet portlet = portletAPI.findPortlet(portletId);
        if (null == portlet || UtilMethods.isNotSet(portlet.getPortletId())) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Status.NOT_FOUND, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.not.found",user.getUserId(), portletId);
        }

        final Layout layout = layoutAPI.loadLayout(layoutId);
        if (null == layout || UtilMethods.isNotSet(layout.getId())) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Status.NOT_FOUND, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.layout.not.found",user.getUserId(), layoutId);
        }

        final List<Layout> userLayouts = layoutAPI.loadLayoutsForUser(user);
        if (!userLayouts.contains(layout)) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.user.layout.permission",user.getUserId(), layout.getId());
        }

        final List<String> portletIds = new ArrayList<>(layout.getPortletIds());

        if(!portletIds.contains(portletId)){
            portletIds.add(portletId);
        } else {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Status.BAD_REQUEST, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.layout.contains.portletId",layout.getId(),portletId);
        }

        layoutAPI.setPortletIdsToLayout(layout, portletIds);

        return Response.ok(new ResponseEntityView(
                map("portlet", portlet.getPortletId(), "layout", layout.getId())))
                .build();

    }

    /**
     *  Custom Portlet delete endpoint
     * @param request
     * @param portletId
     * @return
     */
  @DELETE
  @Path("/custom/{portletId}")
  @JSONP
  @NoCache
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public final Response deleteCustomPortlet(@Context final HttpServletRequest request, @PathParam("portletId") final String portletId) {

    final InitDataObject initData = new WebResource.InitBuilder(webResource)
            .requiredBackendUser(true)
            .requiredFrontendUser(false)
            .requestAndResponse(request, null)
            .rejectWhenNoUser(true)
            .requiredPortlet("roles")
            .init();

    try {


      APILocator.getPortletAPI().deletePortlet(portletId);

      return Response.ok(new ResponseEntityView(map("message", portletId + " deleted"))).build();

    } catch (Exception e) {
      return ResponseUtil.mapExceptionResponse(e);
    }

  }

    /**
     * Portlet delete
     * @param request
     * @param portletId
     * @return
     */
    @DELETE
    @Path("/portletId/{portletId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deletePersonalPortlet(@Context final HttpServletRequest request,
                    @PathParam("portletId") final String portletId) {
        final User user = new WebResource.InitBuilder(webResource).requiredBackendUser(true)
                        .requestAndResponse(request, null).rejectWhenNoUser(true).requiredPortlet("roles").init()
                        .getUser();

        return deletePortletForRole(request, portletId, user.getUserId());
    }


    /**
     * Delete Portlet For Role
     * @param request
     * @param portletId
     * @param roleId
     * @return
     */
    @DELETE
    @Path("/portletId/{portletId}/roleId/{roleId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deletePortletForRole(@Context final HttpServletRequest request,
                    @PathParam("portletId") final String portletId, @PathParam("roleId") final String roleId) {

        final User user = new WebResource.InitBuilder(webResource).requiredBackendUser(true)
                        .requestAndResponse(request, null).rejectWhenNoUser(true).requiredPortlet("roles").init()
                        .getUser();

        try {


            final Role role = APILocator.getRoleAPI().loadRoleById(roleId);
            final Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);

            if (role == null || portlet == null) {
                return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                                user.getUserId(), "unable to remove role from portlet");
            }

            if(!user.isAdmin() && !user.getUserId().equals(role.getRoleKey())) {
                return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                                user.getUserId(),
                                "Unable to remove portlet for role");
            }
            
            
            

            List<Layout> layouts = APILocator.getLayoutAPI().loadLayoutsForRole(role);
            for (Layout layout : layouts) {
                if (layout.getPortletIds().contains(portletId)) {
                    List<Portlet> portlets = layout.getPortletIds().stream().filter(p -> !p.equals(portletId))
                                    .map(p -> APILocator.getPortletAPI().findPortlet(p)).collect(Collectors.toList());

                    if (portlets.isEmpty()) {
                        Logger.info(this.getClass(), "removing layout " + layout.getName() + " from role " + role.getName());
                        APILocator.getRoleAPI().removeLayoutFromRole(layout, role);
                    } else {
                        APILocator.getLayoutAPI().setPortletsToLayout(layout, portlets);
                    }


                }
            }

            return Response.ok(new ResponseEntityView(map("message", portletId + " deleted"))).build();

        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }

    }

    /**
     * This endpoint returns a portlet's details given its id
     * @param request
     * @param portletId
     * @return
     */
    @GET
    @JSONP
    @Path("/{portletId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findPortlet(@Context final HttpServletRequest request,
            @PathParam("portletId") final String portletId) {

        final User user = new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet(portletId)
                .init().getUser();

        final Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
        if(null == portlet){
            return ResponseUtil.INSTANCE.getErrorResponse(request, Status.NOT_FOUND, user.getLocale(),
                    user.getUserId(),
                    "Unable to find portlet");
        }
        return Response.ok(new ResponseEntityView(
                map("response", portlet))).build();

    }

    /**
     * portlet access permis2sion check
     * @param request
     * @param portletId
     * @return
     */
    @GET
    @JSONP
    @Path("/{portletId}/_doesuserhaveaccess")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response doesUserHaveAccessToPortlet(@Context final HttpServletRequest request,
            @PathParam("portletId") final String portletId) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        try {
            return Response.ok(new ResponseEntityView(map("response", APILocator.getLayoutAPI()
                    .doesUserHaveAccessToPortlet(portletId, initData.getUser())))).build();
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This endpoint is to get the actionURL to fire the create content modal. The content that
     * will be created is the one pass in the contentTypeVariable param.
     *
     * @param request
     * @param httpResponse
     * @param contentTypeVariable - content type variable name
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @Path("/_actionurl/{contentTypeVariable}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getCreateContentURL(@Context final HttpServletRequest request,
            @Context final HttpServletResponse httpResponse,
            @PathParam("contentTypeVariable") String contentTypeVariable)
            throws DotDataException, DotSecurityException {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final User user = initData.getUser();
        final String contentTypeId = APILocator.getContentTypeAPI(user).find(contentTypeVariable).id();
        final String strutsAction = "calendarEvent".equals(contentTypeVariable) ?
                "/ext/calendar/edit_event" :
                "/ext/contentlet/edit_contentlet";

        return Response.ok(
                new ResponseEntityView((
                        ContentTypeUtil.getInstance().getActionUrl(request,contentTypeId,user,strutsAction))))
                .build();
    }
}
