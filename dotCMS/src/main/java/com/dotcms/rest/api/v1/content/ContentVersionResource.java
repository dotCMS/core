package com.dotcms.rest.api.v1.content;

import static com.dotcms.rest.api.v1.authentication.ResponseUtil.getFormattedMessage;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;

@Path("/v1/content/versions")
public class ContentVersionResource {

    private static final String FIND_BY_ID_ERROR_MESSAGE_KEY = "Unable-to-find-contentlet-by-id";
    private static final String FIND_BY_INODE_ERROR_MESSAGE_KEY = "Unable-to-find-contentlet-by-inode";
    private static final String DATATYPE_MISSMATCH_ERROR_MESSAGE_KEY = "Data-Type-Missmatch";
    private static final String DATATYPE_MISSMATCH_2_ERROR_MESSAGE_KEY = "Data-Type-Missmatch2";

    private static final int MIN = 20;
    private static final int MAX = 100;

    private final WebResource webResource;
    private final ContentletAPI contentletAPI;
    private final LanguageAPI languageAPI;

    public ContentVersionResource() {
        this(APILocator.getContentletAPI(), APILocator.getLanguageAPI(), new WebResource());
    }

    @VisibleForTesting
    ContentVersionResource(final ContentletAPI contentletAPI, final LanguageAPI languageAPI,
            final WebResource webResource) {
        this.contentletAPI = contentletAPI;
        this.languageAPI = languageAPI;
        this.webResource = webResource;
    }

    @GET
    @JSONP
    @NoCache
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findVersions(@Context final HttpServletRequest request,
            @QueryParam("identifier") final String identifier, @QueryParam("groupByLang")final String groupByLangParam, @QueryParam("limit") final int limit)
            throws DotDataException, DotStateException, DotSecurityException {

        final boolean groupByLang = "1".equals(groupByLangParam) || BooleanUtils.toBoolean(groupByLangParam);
        final int showing = limit > MAX ? MAX : limit < MIN ? MIN : limit;
        final InitDataObject auth = webResource.init(true, request, true);

        final User user = auth.getUser();
        try {

            Logger.debug(this,
                    "Getting versions for identifier: " + identifier + " grouping by language: '" + BooleanUtils.toStringYesNo(groupByLang)+ "' and limit: "+limit);

            final ShortyId shorty = APILocator
                    .getShortyAPI().getShorty(identifier)
                    .orElseThrow(() -> new DoesNotExistException(
                            getFormattedMessage(user.getLocale(), FIND_BY_ID_ERROR_MESSAGE_KEY,
                                    identifier)));
            final Identifier identifierObj =
                    (shorty.type == ShortType.IDENTIFIER) ? APILocator.getIdentifierAPI()
                            .find(shorty.longId)
                            : APILocator.getIdentifierAPI().findFromInode(shorty.longId);

           ResponseEntityView responseEntityView = null;
           if(groupByLang){
               final Map<String, List<Map<String, Object>>> versionsByLang = new HashMap<>();
               final Map<Long, List<Contentlet>> contentByLangMap = contentletAPI
                       .findAllVersions(identifierObj, user, false).stream()
                       .limit(showing)
                       .collect(Collectors.groupingBy(Contentlet::getLanguageId));
               contentByLangMap.forEach((langId, contentlets) -> {
                   final Language lang = languageAPI.getLanguage(langId);
                   final List<Map<String, Object>> asMaps = contentlets.stream()
                           .map(this::contentletToMap).collect(Collectors.toList());
                   versionsByLang.put(lang.toString(), asMaps);
               });

               responseEntityView = new ResponseEntityView(ImmutableMap.of("versions", versionsByLang));

           } else {
               final List<Map<String, Object>> versions = contentletAPI
                       .findAllVersions(identifierObj, user, false).stream()
                       .limit(showing)
                       .map(this::contentletToMap).collect(Collectors.toList());

               responseEntityView = new ResponseEntityView(ImmutableMap.of("versions", versions));
           }

            return Response.ok(responseEntityView).build();

        } catch (Exception ex) {
            Logger.error(this.getClass(),
                    "Exception on method findAllVersions with exception message: " + ex
                            .getMessage(), ex);
            return ResponseUtil.mapExceptionResponse(ex);
        }

    }

    @GET
    @JSONP
    @NoCache
    @Path("/{inode}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findByInode(@Context final HttpServletRequest request,
            @PathParam("inode") final String inode)
            throws DotStateException {
        final InitDataObject auth = webResource.init(true, request, true);
        final User user = auth.getUser();
        try {
            Logger.debug(this,
                    "Getting version for inode: " + inode );
            final ShortyId shorty = APILocator
                    .getShortyAPI().getShorty(inode)
                    .orElseThrow(() -> new DoesNotExistException(getFormattedMessage(user.getLocale(),
                            FIND_BY_INODE_ERROR_MESSAGE_KEY, inode)));

            if (shorty.type != ShortType.INODE) {
                throw new BadRequestException(
                        getFormattedMessage(user.getLocale(), DATATYPE_MISSMATCH_ERROR_MESSAGE_KEY));
            }

            final Contentlet contentlet = APILocator.getContentletAPI().find(inode, user, true);
            if (null == contentlet) {
                throw new DoesNotExistException(
                        getFormattedMessage(user.getLocale(), FIND_BY_INODE_ERROR_MESSAGE_KEY,
                                inode));
            }
            final Response.ResponseBuilder responseBuilder = Response.ok(
                    new ResponseEntityView(contentletToMap(contentlet))
            );
            return responseBuilder.build();
        } catch (Exception ex) {
            Logger.error(this.getClass(),
                    "Exception on method findByInode with exception message: " + ex
                            .getMessage(), ex);
            return ResponseUtil.mapExceptionResponse(ex);
        }

    }

    @GET
    @JSONP
    @NoCache
    @Path("/diff/{inode1}/{inode2}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response diff(@Context final HttpServletRequest request,
            @PathParam("inode1") final String inode1, @PathParam("inode2") final String inode2)
            throws DotStateException {

        final InitDataObject auth = webResource.init(true, request, true);
        final User user = auth.getUser();
        try {

            Logger.debug(this,
                    "Building diff between version '" + inode1 + "' and '" + inode2 + "'"  );

            final ShortyId shorty1 = APILocator
                    .getShortyAPI().getShorty(inode1)
                    .orElseThrow(() -> new DoesNotExistException(
                            getFormattedMessage(user.getLocale(), FIND_BY_ID_ERROR_MESSAGE_KEY,
                                    inode1)));
            final ShortyId shorty2 = APILocator
                    .getShortyAPI().getShorty(inode2)
                    .orElseThrow(() -> new DoesNotExistException(
                            getFormattedMessage(user.getLocale(), FIND_BY_ID_ERROR_MESSAGE_KEY,
                                    inode2)));
            if (shorty1.type != ShortType.INODE
                    || shorty2.type != ShortType.INODE) {
                throw new BadRequestException(
                        getFormattedMessage(user.getLocale(), DATATYPE_MISSMATCH_2_ERROR_MESSAGE_KEY));
            }

            final Contentlet contentlet1 = APILocator
                    .getContentletAPI().find(shorty1.longId, user, false);

            final Contentlet contentlet2 = APILocator
                    .getContentletAPI().find(shorty2.longId, user, false);

            final Response.ResponseBuilder responseBuilder = Response.ok(new ResponseEntityView(
                    ImmutableMap.of(
                            shorty1.longId, contentletToMap(contentlet1),
                            shorty2.longId, contentletToMap(contentlet2)
                    ))
            );
            return responseBuilder.build();
        } catch (Exception ex) {
            Logger.error(this.getClass(),
                    "Exception on method diff with exception message: " + ex
                            .getMessage(), ex);
            return ResponseUtil.mapExceptionResponse(ex);
        }
    }

    private Map<String, Object> contentletToMap( final Contentlet con) {
        return new ContentletToMapTransformer(con).toMaps().get(0);
    }
}
