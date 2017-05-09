package com.dotcms.rest.api.v1.contenttype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.RESTParams;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.ConversionUtils.toInt;
import java.util.Map;

@Path("/v1/contenttype")
public class ContentTypeResource implements Serializable {
  private final WebResource webResource;
  private final ContentTypeHelper contentTypeHelper;

  public ContentTypeResource() {
    this(ContentTypeHelper.getInstance(), new WebResource());
  }

  @VisibleForTesting
  public ContentTypeResource(final ContentTypeHelper contentletHelper, final WebResource webresource) {
    this.webResource = webresource;
    this.contentTypeHelper = contentletHelper;
  }

  @VisibleForTesting
  public ContentTypeResource(final ContentTypeHelper contentletHelper) {
    this(contentletHelper, new WebResource());
  }

  private static final long serialVersionUID = 1L;


  @POST
  @JSONP
  @NoCache
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public final Response saveType(@Context final HttpServletRequest req, final String json)
      throws DotDataException, DotSecurityException {
    final InitDataObject initData = this.webResource.init(null, true, req, true, null);
    final User user = initData.getUser();
    List<ContentType> typesToSave = new JsonContentTypeTransformer(json).asList();
    
    List<ContentType> retTypes = new ArrayList<>();

    for (ContentType type :typesToSave ) {
      retTypes.add(APILocator.getContentTypeAPI(user, true).save(type, type.fields()));
    }
    
    return Response.ok(new ResponseEntityView(new JsonContentTypeTransformer(retTypes).mapObject())).build();

  }

  @DELETE
  @Path("/id/{id}")
  @JSONP
  @NoCache
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public Response deleteType(@PathParam("id") final String id, @Context final HttpServletRequest req)
      throws DotDataException, DotSecurityException, JSONException {

    final InitDataObject initData = this.webResource.init(null, true, req, true, null);
    final User user = initData.getUser();

    ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);


    ContentType type = null;
    try {
      type = tapi.find(id);
    } catch (NotFoundInDbException nfdb) {
      try {
        type = tapi.find(id);
      } catch (NotFoundInDbException nfdb2) {
        return Response.status(404).build();
      }
    }

    tapi.delete(type);


    JSONObject joe = new JSONObject();
    joe.put("deleted", type.id());


    Response response = Response.ok(joe.toString()).build();
    return response;
  }

  @GET
  @Path("/id/{id}")
  @JSONP
  @NoCache
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public Response getType(@PathParam("id") final String id, @Context final HttpServletRequest req)
      throws DotDataException, DotSecurityException {

    final InitDataObject initData = this.webResource.init(null, false, req, false, null);
    final User user = initData.getUser();
    ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
    Response response = Response.status(404).build();
    try {
      ContentType type = tapi.find(id);
      response = Response.ok(new ResponseEntityView(new JsonContentTypeTransformer(type).mapObject())).build();
    } catch (NotFoundInDbException nfdb2) {
      // nothing to do here, will throw a 404
    }


    return response;
  }


  @GET
  @Path("/basetypes")
  @JSONP
  @InitRequestRequired
  @NoCache
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public final Response getRecentBaseTypes(@Context final HttpServletRequest request) {

    Response response = null;

    try {
      List<BaseContentTypesView> types = contentTypeHelper.getTypes(request);
      response = Response.ok(new ResponseEntityView(types)).build();
    } catch (Exception e) { // this is an unknown error, so we report as a 500.

      response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    return response;
  } // getTypes.

  /**
   * Return a list of {@link ContentType}, entity response syntax:.
   *
   * <code>
   *  {
   *      contentTypes: array of ContentType
   *      total: total number of content types
   *  }
   * <code/>
   *
   * Url sintax: contenttype/query/query-string/limit/n-limit/offset/n-offset/orderby/fieldname-order_direction
   *
   * where:
   *
   * <ul>
   *     <li>query-string: just return ContentTypes who content this pattern</li>
   *     <li>n-limit: limit of items to return</li>
   *     <li>n-offset: offset</li>
   *     <li>fieldname: field to order by</li>
   *     <li>order_direction: asc for upward order and desc for downward order</li>
   * </ul>
   *
   * Url example: v1/contenttype/query/New%20L/limit/4/offset/5/orderby/name-asc
   *
   * @param request
   * @param params
   * @return
   */
  @GET
  @Path ("/{params:.*}")
  @JSONP
  @NoCache
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public final Response getContentTypes(@Context final HttpServletRequest request,
                                        @PathParam ("params") final String params) {

    final InitDataObject initData = webResource.init(params, false, request, true, null);

    Response response = null;

    final User user = initData.getUser();
    /* Limit and Offset Parameters Handling, if not passed, using default */
    final String limitStr = initData.getParamsMap().get(RESTParams.LIMIT.getValue());
    final String offsetStr = initData.getParamsMap().get(RESTParams.OFFSET.getValue());

    int limit  = toInt(limitStr, -1);
    int offset = toInt(offsetStr, -1);

    String orderbyParams = initData.getParamsMap().get(RESTParams.ORDERBY.getValue());
    String[] split = orderbyParams != null ? orderbyParams.split("-") : null;
    String orderby = (split == null || "name".equals(split[0])) ? "upper(name)" : split[0];
    String direction = (split == null || split.length < 2) ? "asc" : split[1];

    String filterBy = initData.getParamsMap().get(RESTParams.QUERY.getValue());
    String queryCondition = filterBy == null ? "" : String.format("(name like '%%%s%%')", filterBy);

    try {
      List<Map<String, Object>> types = contentTypeHelper.getContentTypes(user, queryCondition, offset, limit, orderby, direction);
      long contentTypesCount = contentTypeHelper.getContentTypesCount();
      response = Response.ok(new ResponseEntityView( map("contentTypes", types, "total", contentTypesCount)))
              .build();
    } catch (Exception e) {

      response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    return response;
  }
}
