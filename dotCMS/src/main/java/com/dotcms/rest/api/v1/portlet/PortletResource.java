package com.dotcms.rest.api.v1.portlet;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
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
  public final Response createContentPortlet(@Context final HttpServletRequest request,
                                 final CustomPortletForm formData) {
    final InitDataObject init = webResource.init(null, true, request, true, "roles");

    Response response = null;

    try {

      final Portlet contentPortlet = portletApi.findPortlet("content");
      final Map<String, String> initValues = new HashMap<>();

      initValues.putAll(contentPortlet.getInitParams());
      initValues.put("name", formData.portletName);
      initValues.put("baseTypes", formData.baseTypes);
      initValues.put("contentTypes", formData.contentTypes);

      final Portlet newPortlet = APILocator.getPortletAPI().savePortlet(new DotPortlet(formData.portletId, contentPortlet.getPortletClass(), initValues),init.getUser());

      return Response.ok(new ResponseEntityView(map("portlet", newPortlet.getPortletId()))).build();

    } catch (Exception e){
      response = ResponseUtil.mapExceptionResponse(e);
    }

    return response;
  }

}
