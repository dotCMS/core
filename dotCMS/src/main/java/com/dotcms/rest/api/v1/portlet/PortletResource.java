package com.dotcms.rest.api.v1.portlet;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.portal.DotPortlet;
import com.dotmarketing.business.portal.PortletAPI;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
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
      final Map<String, String> initValues = new HashMap<>();

      initValues.putAll(contentPortlet.getInitParams());
      initValues.put("name", formData.portletName);
      initValues.put("baseTypes", formData.baseTypes);
      initValues.put("contentTypes", formData.contentTypes);

      final Portlet newPortlet = APILocator.getPortletAPI()
          .savePortlet(new DotPortlet(formData.portletId, contentPortlet.getPortletClass(), initValues), initData.getUser());

      return Response.ok(new ResponseEntityView(map("portlet", newPortlet.getPortletId()))).build();

    } catch (Exception e) {
      response = ResponseUtil.mapExceptionResponse(e);
    }

    return response;
  }

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
  
  
  
  
  
    @GET
    @JSONP
    @Path("/{portletId}/_doesuserhaveaccess")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response doesUserHaveAccessToPortlet(@Context final HttpServletRequest request,
            @PathParam("portletId") final String portletId) throws DotDataException {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        return Response.ok(new ResponseEntityView(map("response", APILocator.getLayoutAPI()
                .doesUserHaveAccessToPortlet(portletId, initData.getUser())))).build();
    }
}
