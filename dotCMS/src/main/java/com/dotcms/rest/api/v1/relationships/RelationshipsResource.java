package com.dotcms.rest.api.v1.relationships;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.RelationshipPaginator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.toImmutableList;

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
    public final Response getCardinality() throws Throwable {
        Logger.debug(this, "Getting relationships cardinality");

        return Response.ok(new ResponseEntityView<>(
                Arrays.stream(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.values())
                      .map(cardinality -> {
                          String label;

                          try {
                              label = LanguageUtil.get(String.format(
                                      "contenttypes.field.properties.relationships.cardinality.%s.label", cardinality.name()));
                          } catch (LanguageException e) {
                              label = cardinality.name();
                          }

                          return Map.of(
                                  "name", cardinality.name(),
                                  "id", cardinality.ordinal(),
                                  "label", label
                                 );
                      })
                      .collect(toImmutableList())
        )).build();
    }

    /**
     * Returns orphan relationships (those defined in the parent or children but not in both) given a content type.
     * @param contentTypeId
     * @param page
     * @param perPage
     * @param request
     * @return
     * @throws Throwable
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getOneSidedRelationships(
            @QueryParam("contentTypeId") final String contentTypeId,
            @QueryParam(PaginationUtil.PAGE) final int page,
            @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("0") final int perPage,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) throws Throwable {
        Logger.debug(this,
                "Getting the possible relationships for content type " + contentTypeId);

        final InitDataObject initData = this.webResource.init(null, request, response, true, null);
        final User user = initData.getUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        final PaginationUtil paginationUtil = new PaginationUtil(new RelationshipPaginator());

        try {
            final ContentType contentType = contentTypeAPI.find(contentTypeId);

            final Map<String, Object> params = new HashMap<>();
            params.put(RelationshipPaginator.CONTENT_TYPE_PARAM,
                    contentType);
            return paginationUtil.getPage(request, user, null, page, perPage, params);
        } catch (Exception e) {

            return ResponseUtil.mapExceptionResponse(e);
        }
    }
}
