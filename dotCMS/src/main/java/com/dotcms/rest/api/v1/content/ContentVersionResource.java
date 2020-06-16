package com.dotcms.rest.api.v1.content;

import static com.dotcms.rest.api.v1.authentication.ResponseUtil.getFormattedMessage;

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
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.JSONP;

@Path("/v1/content/versions")
public class ContentVersionResource {

    private static final String FIND_BY_ID_ERROR_MESSAGE_KEY = "Unable-to-find-contentlet-by-id";
    private static final String FIND_BY_INODE_ERROR_MESSAGE_KEY = "Unable-to-find-contentlet-by-inode";
    private static final String DATATYPE_MISSMATCH_ERROR_MESSAGE_KEY = "Data-Type-Missmatch";
    private static final String BAD_REQUEST_ERROR_MESSAGE_KEY = "Bad-Request";

    private static final String VERSIONS = "versions";

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

    /**
     * This method retrieves all versions for a piece content either by a given identifier or a set of inodes.
     * You can also provide the optional param (groupByLang) to organize the resulting list by language
     * @param request The ServletRequest
     * @param inodes The set of (comma separated) inodes to look for.
     * @param identifier The Content identifier. You must provide an Identifier or a set of inodes but not both. If you do so the identifier will take precendence.
     * @param groupByLangParam If this param happens to have a value that could be interpreted as true (groupByLangParam=1, groupByLangParam=true), it'll cause the output to be arranged in bunches of the same language.
     * @param limit Numeric param that will limit the output.
     * @return
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findVersions(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                 @QueryParam("inodes") final String inodes,
            @QueryParam("identifier") final String identifier, @QueryParam("groupByLang")final String groupByLangParam, @QueryParam("limit") final int limit)
            throws DotDataException, DotStateException, DotSecurityException {

        final boolean groupByLang = "1".equals(groupByLangParam) || BooleanUtils.toBoolean(groupByLangParam);
        final int showing = limit > MAX ? MAX : limit < MIN ? MIN : limit;
        final InitDataObject auth = webResource.init(request, response, true);
        final boolean respectFrontendRoles = PageMode.get(request).respectAnonPerms;
        final User user = auth.getUser();
        try {

           final Identifier identifierObj = getIdentifier(identifier, user);

           ResponseEntityView responseEntityView = null;
           if(null != identifier){

               Logger.debug(this,
                       ()->"Getting versions for identifier: " + identifier + " grouping by language: '" + BooleanUtils.toStringYesNo(groupByLang)+ "' and limit: "+limit);

               if(groupByLang){
                   final Map<String, List<Map<String, Object>>> versionsByLang = mapVersionsByLang(contentletAPI
                           .findAllVersions(identifierObj, user, respectFrontendRoles), showing);
                   responseEntityView = new ResponseEntityView(ImmutableMap.of(VERSIONS, versionsByLang));
               } else {
                   final List<Map<String, Object>> versions = mapVersions(contentletAPI
                            .findAllVersions(identifierObj, user, respectFrontendRoles), showing);

                   responseEntityView = new ResponseEntityView(ImmutableMap.of(VERSIONS, versions));
               }
           } else {
               final Set<String> inodesSet =
                       UtilMethods.isSet(inodes) ? Arrays.stream(inodes.split(",")).map(String::trim)
                               .limit(showing).collect(Collectors.toSet()) : null;

               if(null != inodesSet) {

                   Logger.debug(this,
                           ()->"Getting versions for inodes: " + StringUtils.join(inodesSet, ',') + " grouping by language: '" + BooleanUtils.toStringYesNo(groupByLang)+ "' and limit: "+limit);

                   if(groupByLang){
                       final Map<String, List<Map<String, Object>>> versionsByLang = mapVersionsByLang(findByInodes(user, inodesSet, respectFrontendRoles), showing);
                       responseEntityView = new ResponseEntityView(ImmutableMap.of(VERSIONS, versionsByLang));
                   } else {
                       final Map<String,Map<String,Object>> versions = mapVersionsByInode(findByInodes(user, inodesSet, respectFrontendRoles), showing);
                       responseEntityView = new ResponseEntityView(ImmutableMap.of(VERSIONS, versions));
                   }


               } else {
                    throw new BadRequestException(getFormattedMessage(user.getLocale(), BAD_REQUEST_ERROR_MESSAGE_KEY));
               }
           }

            return Response.ok(responseEntityView).build();

        } catch (Exception ex) {
            Logger.error(this.getClass(),
                    "Exception on method findAllVersions with exception message: " + ex
                            .getMessage(), ex);
            return ResponseUtil.mapExceptionResponse(ex);
        }

    }

    /**
     * Utility method to get an identifier object. Given a full identifier string or a shorty
     * @param identifier The input string
     * @param user Current user
     * @return An Identifier object
     * @throws DotDataException A Wrapping Data Exception
     */
    private Identifier getIdentifier(final String identifier, final User user ) throws DotDataException{
        if(!UtilMethods.isSet(identifier)){
          return null;
        }
        final ShortyId shorty = APILocator
                .getShortyAPI().getShorty(identifier)
                .orElseThrow(() -> new DoesNotExistException(
                        getFormattedMessage(user.getLocale(), FIND_BY_ID_ERROR_MESSAGE_KEY,
                                identifier)));
        return
                (shorty.type == ShortType.IDENTIFIER) ? APILocator.getIdentifierAPI()
                        .find(shorty.longId)
                        : APILocator.getIdentifierAPI().findFromInode(shorty.longId);
    }

    /**
     * Given a Set of inodes This will return the respective Contentlets
     * If the inode does not belong into the database an exception will be raised.
     * @param user current user
     * @param inodes Set of inodes
     * @param respectFrontendRoles We're in the FrontEnd or Backend
     * @return list of contentlets
     * @throws DotStateException
     */
    private List<Contentlet> findByInodes(final User user, final Set<String> inodes, final boolean respectFrontendRoles)
            throws DotStateException {

        final Set<String> notFound = new HashSet<>();
        final Set<ShortyId> mappedShorties = inodes.stream().map(inode -> {
            final Optional<ShortyId> shortyOptional = APILocator.getShortyAPI().getShorty(inode);
            return shortyOptional.orElseGet(() -> {
                notFound.add(inode);
                return null;
            });
        }).collect(Collectors.toSet());

        if (!notFound.isEmpty()) {
            throw new DoesNotExistException(
                    getFormattedMessage(user.getLocale(), FIND_BY_INODE_ERROR_MESSAGE_KEY,
                            StringUtils.join(notFound, ',')));
        }

        return mappedShorties.stream().map(shortyId -> {
            try {
                return APILocator.getContentletAPI().find(shortyId.longId, user, respectFrontendRoles);
            } catch (DotDataException | DotSecurityException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Map an input list of contentlets into a list of Maps (Using a transformer)
     * @param input list of contentlets
     * @param limit cuts the number of items to be processed
     * @return a List of Maps
     */
    private List<Map<String, Object>> mapVersions(final List<Contentlet> input, final int limit){
        return input.stream().limit(limit).map(this::contentletToMap).collect(Collectors.toList());
    }

    /**
     * Maps an input list of contentlets into a list of Maps (Using a transformer)
     * @param input list of contentlets
     * @param limit cuts the number of items to be processed
     * @return a Map of Maps (Organized by inode)
     */
    private Map<String,Map<String,Object>> mapVersionsByInode(final List<Contentlet> input, final int limit){
        return input.stream().limit(limit).collect(Collectors.toMap(Contentlet::getInode,this::contentletToMap));
    }

    /**
     * Arrange content grouping by language
     * @param input list of contentlets
     * @param limit cuts the number of items to be processed
     * @return A Map o lists, on which each entry (Mapped by language) is a list of contents.
     */
    private Map<String, List<Map<String, Object>>> mapVersionsByLang(final List<Contentlet> input, final int limit){
        final Map<String, List<Map<String, Object>>> versionsByLang = new HashMap<>();
        final Map<Long, List<Contentlet>> contentByLangMap = input.stream().limit(limit).collect(Collectors.groupingBy(Contentlet::getLanguageId));
        contentByLangMap.forEach((langId, contentlets) -> {
            final Language lang = languageAPI.getLanguage(langId);
            final List<Map<String, Object>> asMaps = contentlets.stream()
                    .map(this::contentletToMap).collect(Collectors.toList());
            versionsByLang.put(lang.toString(), asMaps);
        });
        return versionsByLang;
    }


    /**
     * Simple method to be used specifically with only one inode
     * @param request ServletRequest
     * @param inode The inode it-self
     * @return A ServletResponse
     * @throws DotStateException
     */
    @GET
    @JSONP
    @NoCache
    @Path("/{inode}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("inode") final String inode)
            throws DotStateException {
        final boolean respectFrontendRoles = PageMode.get(request).respectAnonPerms;
        final InitDataObject auth = webResource.init(request, response,true);
        final User user = auth.getUser();
        try {
            Logger.debug(this,
                    ()->"Getting version for inode: " + inode );
            final ShortyId shorty = APILocator
                    .getShortyAPI().getShorty(inode)
                    .orElseThrow(() -> new DoesNotExistException(getFormattedMessage(user.getLocale(),
                            FIND_BY_INODE_ERROR_MESSAGE_KEY, inode)));

            if (shorty.type != ShortType.INODE) {
                throw new BadRequestException(
                        getFormattedMessage(user.getLocale(), DATATYPE_MISSMATCH_ERROR_MESSAGE_KEY));
            }

            final Contentlet contentlet = APILocator.getContentletAPI().find(inode, user, respectFrontendRoles);
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

    /**
     * Used to call and transform a Contentlet
     * @param contentlet A Contentlet
     * @return Transformed Map
     */
    private Map<String, Object> contentletToMap( final Contentlet contentlet) {
        return new DotTransformerBuilder().defaultOptions().content(contentlet).build().toMaps().get(0);
    }
}
