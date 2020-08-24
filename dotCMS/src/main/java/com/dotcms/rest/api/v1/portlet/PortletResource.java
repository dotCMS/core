package com.dotcms.rest.api.v1.portlet;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotDataException;
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
import com.dotmarketing.business.portal.DotPortlet;
import com.dotmarketing.business.portal.PortletAPI;
import com.liferay.portal.model.Portlet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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

    @GET
    @JSONP
    @Path("/permissions/{portletId}")
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
