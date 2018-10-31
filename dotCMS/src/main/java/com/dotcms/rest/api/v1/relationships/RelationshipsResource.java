package com.dotcms.rest.api.v1.relationships;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.toImmutableList;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.ws.rs.DefaultValue;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.JsonProcessingRuntimeException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.RelationshipPaginator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * This resource provides all the different end-points associated to information and actions that
 * the front-end can perform on relationships.
 *
 * @author nollymar
 */
@Path("/v1/relationships")
public class RelationshipsResource {

    private final WebResource webResource;

    public RelationshipsResource() {
        this(new WebResource());
    }


    RelationshipsResource(final WebResource webResource) {

        this.webResource = webResource;

    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("cardinalities")
    public final Response gerCardinality() throws Throwable {
        Logger.debug(this, "Getting relationships cardinality");

        return Response.ok(new ResponseEntityView(
                Arrays.stream(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.values())
                      .map(cardinality -> {
                          String label;

                          try {
                              label = LanguageUtil.get(String.format(
                                      "contenttypes.field.properties.relationships.cardinality.%s.label", cardinality.name()));
                          } catch (LanguageException e) {
                              label = cardinality.name();
                          }

                          return map(
                                  "name", cardinality.name(),
                                  "id", cardinality.ordinal(),
                                  "label", label
                                 );
                      })
                      .collect(toImmutableList())
        )).build();
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getOneSidedRelationships(
            @QueryParam("contentTypeId") final String contentTypeId,
            @QueryParam(PaginationUtil.PAGE) final int page,
            @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("0") final int perPage,
            @Context final HttpServletRequest request) throws Throwable {
        Logger.debug(this,
                "Getting the possible relationships for content type " + contentTypeId);

        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final User user = initData.getUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        ContentType contentType;

        if (UtilMethods.isSet(contentTypeId)) {
            //Validate if contentTypeId is valid
            try {
                contentType = contentTypeAPI.find(contentTypeId);

            } catch (DotSecurityException | DotDataException e) {
                Logger.error(this, e.getMessage(), e);
                return ExceptionMapperUtil
                        .createResponse(map("message", "Invalid Content Type"),
                                "Invalid Content Type",
                                Status.BAD_REQUEST);
            }
        } else {
            return ExceptionMapperUtil
                    .createResponse(map("message", "Content Type ID is required"),
                            "Content Type ID is required",
                            Status.BAD_REQUEST);
        }

        final PaginationUtil paginationUtil = new PaginationUtil(new RelationshipPaginator());

        try {

            final Map<String, Object> params = map(RelationshipPaginator.CONTENT_TYPE_PARAM,
                    contentType);
            return paginationUtil.getPage(request, user, null, page, perPage, params);
        } catch (JsonProcessingRuntimeException e) {
            Logger.error(this, e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

}
