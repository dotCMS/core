package com.dotcms.rest.api.v1.relationships;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.RelationshipPaginator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
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
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;
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

            final Map<String, Object> params = map(RelationshipPaginator.CONTENT_TYPE_PARAM,
                    contentType);
            return paginationUtil.getPage(request, user, null, page, perPage, params);
        } catch (Exception e) {

            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Will return a Contentlet objects. If you are building large pulls and depending on the types
     * of fields on the content this can get expensive especially with large data sets.<br />
     * EXAMPLE:<br /> /api/v1/relationships?relationshipfieldvariable=myRelationship&identifier=asbd-asd-asda-asd&filter=+myField:someValue&per_page=5&page=1&orderby=modDate&direction=desc
     * The method will figure out language, working and
     * live for you if not passed in with the condition Returns empty List if no results are found
     * @param request  - Http Request
     * @param response - Http Response
     * @param relationshipFieldVariable - Name of the relationship as defined in the structure.
     * @param contentletIdentifier - Identifier of the contentlet
     * @param luceneCondition - Extra conditions to add to the query. like +title:Some Title.  Can be
     * @param limit 0 is the dotCMS max limit which is 10000. Be careful when searching for
     * unlimited amount as all content will load into memory
     * @param offset page to request
     * @param orderby - Velocity variable name to sort by.  This is a string and can contain multiple
     * values "sort1 acs, sort2 desc". Can be Null
     * @return Returns empty List if no results are found
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("parents")
    public Response pullRelatedParents(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @QueryParam("fieldvariable")   final String relationshipFieldVariable,
                                @QueryParam("identifier")   final String contentletIdentifier,
                                @QueryParam(PaginationUtil.FILTER)                             final String luceneCondition,
                                @DefaultValue("40") @QueryParam(PaginationUtil.PER_PAGE)       final int limit,
                                @QueryParam(PaginationUtil.PAGE)                               final int offset,
                                @DefaultValue("mod_date desc") @QueryParam(PaginationUtil.ORDER_BY) final String orderby) {

            final InitDataObject initData = this.webResource.init(null, request, response, true, null);

            Logger.debug(this, ()-> "Requesting pull related parents for the contentletIdentifier: " + contentletIdentifier +
                                ", relationshipFieldVariable: " + relationshipFieldVariable + ", luceneCondition: " + luceneCondition +
                                ", limit: " + limit + ", offset: " + offset + ", orderby: " + orderby);

            final User user = initData.getUser();
            final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
            final long langId = UtilMethods.isSet(luceneCondition)
                    && luceneCondition.contains("languageId") ? -1 : language.getId();
            final String tmDate = request.getSession (false) != null?
                    (String) request.getSession (false).getAttribute("tm_date"):null;
            final PageMode mode = PageMode.get(request);
            final boolean editOrPreviewMode = !mode.showLive;
            final List<Contentlet> retrievedContentlets = ContentUtils
                    .pullRelated(relationshipFieldVariable, contentletIdentifier,
                            luceneCondition == null ? luceneCondition : ContentUtils.addDefaultsToQuery(luceneCondition, editOrPreviewMode, request),
                            true,
                            limit, orderby,
                            user, tmDate, langId,
                            editOrPreviewMode? null : true);

            return Response.ok(new ResponseEntityView(retrievedContentlets)).build();
    }
}
