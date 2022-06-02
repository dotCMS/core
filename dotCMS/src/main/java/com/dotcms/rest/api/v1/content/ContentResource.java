package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.DotPreconditions;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Version 1 of the Content resource, to interact and retrieve contentlets
 * @author jsanca
 */
@Path("/v1/content")
public class ContentResource {

    private final WebResource    webResource;
    private final ContentletAPI  contentletAPI;
    private final IdentifierAPI  identifierAPI;

    public ContentResource() {

        this(new WebResource(),
                APILocator.getContentletAPI(),
                APILocator.getIdentifierAPI());
    }

    @VisibleForTesting
    public ContentResource(final WebResource     webResource,
                           final ContentletAPI   contentletAPI,
                           final IdentifierAPI  identifierAPI) {

        this.webResource    = webResource;
        this.contentletAPI  = contentletAPI;
        this.identifierAPI  = identifierAPI;
    }
    /**
     * Retrieve a contentlet if exists, 404 otherwise
     * @param request
     * @param response
     * @return Retrieve single contentlet
     */
    @GET
    @Path("/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContent(@Context HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                               @DefaultValue("-1") @QueryParam("language") final String language) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .init();

        Logger.debug(this, () -> "Finding the contentlet: " + inodeOrIdentifier);

        final Tuple2<String, String> idOrInode = this.getIdentifierOrInode(inodeOrIdentifier);
        final String id       = idOrInode._1();
        final String inode    = idOrInode._2();
        final PageMode mode   = PageMode.get(request);
        final long languageId = LanguageUtil.getLanguageId(language);

        return Response.ok(new ResponseEntityView(
                WorkflowHelper.getInstance().contentletToMap(
                        this.getContentlet(inode, id, languageId,
                                ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                                initDataObject, mode)))).build();
    }

    @GET
    @Path("/_canlock/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response canLockContent(@Context HttpServletRequest request, @Context final HttpServletResponse response,
                                   @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                                   @DefaultValue("-1") @QueryParam("language") final String language)
            throws DotDataException, JSONException, DotSecurityException {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(this.webResource)
                        .requestAndResponse(request, response)
                        .requiredAnonAccess(AnonymousAccess.READ)
                        .init();

        Logger.debug(this, () -> "Can lock Contentlet: " + inodeOrIdentifier);

        final Tuple2<String, String> idOrInode = this.getIdentifierOrInode(inodeOrIdentifier);
        final String id       = idOrInode._1();
        final String inode    = idOrInode._2();
        final PageMode mode   = PageMode.get(request);
        final long languageId = LanguageUtil.getLanguageId(language);

        final Map<String, Object> resultMap = new HashMap<>();
        final Contentlet contentlet = this.getContentlet(inode, id, languageId,
                ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                initDataObject, mode);

        final boolean canLock = Try.of(()->this.contentletAPI.canLock(
                contentlet, initDataObject.getUser())).getOrElse(false);

        resultMap.put("canLock", canLock);
        resultMap.put("locked", contentlet.isLocked());

        final Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI()
                .getContentletVersionInfo(id, contentlet.getLanguageId());

        if (contentlet.isLocked() && cvi.isPresent()) {
            resultMap.put("lockedBy", cvi.get().getLockedBy());
            resultMap.put("lockedOn", cvi.get().getLockedOn());
            resultMap.put("lockedByName", APILocator.getUserAPI().loadUserById(cvi.get().getLockedBy()));
        }

        resultMap.put("inode", contentlet.getInode());
        resultMap.put("id",    contentlet.getIdentifier());

        return Response.ok(new ResponseEntityView(resultMap)).build();
    }

    private Tuple2<String, String> getIdentifierOrInode (final String inodeOrIdentifier) throws DotDataException {

        String inode = null;
        String id    = null;
        //Check if is an inode
        final String type = Try.of(() -> InodeUtils.getAssetTypeFromDB(inodeOrIdentifier)).getOrNull();

        //Could mean 2 things: it's an identifier or uuid does not exists
        if (null == type) {
            final Identifier identifier = this.identifierAPI.find(inodeOrIdentifier);

            if (null == identifier || UtilMethods.isNotSet(identifier.getId())) {
                throw new DoesNotExistException(
                        "The contentlet " + inodeOrIdentifier + " does not exists");
            }

            id    = identifier.getId();
        } else {

            inode = inodeOrIdentifier;  // look for by inode
        }

        return Tuple.of(id, inode);
    }

    private Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        PageMode mode = pageMode;

        if(UtilMethods.isSet(inode)) {

            Logger.debug(this, ()-> "Looking for content by inode: " + inode);

            contentlet = this.contentletAPI.find
                    (inode, initDataObject.getUser(), mode.respectAnonPerms);
        } else if (UtilMethods.isSet(identifier)) {

            Logger.debug(this, ()-> "Looking for content by identifier: " + identifier
                    + " and language id: " + language);

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<Contentlet> contentletOpt =  language <= 0?
                    WorkflowHelper.getInstance().getContentletByIdentifier(identifier, mode, initDataObject.getUser(), sessionLanguage):
                    this.contentletAPI.findContentletByIdentifierOrFallback
                            (identifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms);

            if (contentletOpt.isPresent()) {
                contentlet = contentletOpt.get();
            }
        }

        DotPreconditions.notNull(contentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);
        return contentlet;
    }

    /**
     * Will return a Contentlet objects. If you are building large pulls and depending on the types
     * of fields on the content this can get expensive especially with large data sets.<br />
     * EXAMPLE:<br /> /api/v1/content/related<br />
     *
     * {
     *     "identifier":"d66309a7378bbad381fda3accd7b2e80",
     *     "fieldVariable":"myRelationship",
     *     "condition":"+title:child5",
     *     "orderBy":"title desc"
     * }
     * The method will figure out language, working and
     * live for you if not passed in with the condition Returns empty List if no results are found
     * @param request  - Http Request
     * @param response - Http Response
     * @param pullRelatedForm {@link PullRelatedForm}
     * @return Returns empty List if no results are found
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("related")
    public Response pullRelated(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               final PullRelatedForm pullRelatedForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Requesting pull related parents for the contentletIdentifier: " + pullRelatedForm.getIdentifier() +
                ", relationshipFieldVariable: " + pullRelatedForm.getFieldVariable() + ", luceneCondition: " + pullRelatedForm.getCondition() +
                ", limit: " + pullRelatedForm.getLimit() + ", offset: " + pullRelatedForm.getOffset() + ", orderby: " + pullRelatedForm.getOrderBy());

        final User user = initData.getUser();
        final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final long langId = UtilMethods.isSet(pullRelatedForm.getCondition())
                && pullRelatedForm.getCondition().contains("languageId") ? -1 : language.getId();
        final String tmDate = request.getSession (false) != null?
                (String) request.getSession (false).getAttribute("tm_date"):null;
        final PageMode mode = PageMode.get(request);
        final boolean editOrPreviewMode = !mode.showLive;
        final Contentlet contentlet = APILocator.getContentletAPI().
                findContentletByIdentifier(pullRelatedForm.getIdentifier(), mode.showLive, langId, user, mode.respectAnonPerms);

        if (null ==  contentlet || !InodeUtils.isSet(contentlet.getIdentifier())) {

            Logger.debug(this, ()-> "The identifier:" + pullRelatedForm.getIdentifier() + " does not exists");
            throw new DoesNotExistException("The identifier:" + pullRelatedForm.getIdentifier() + " does not exists");
        }

        final Field field = contentlet.getContentType().fieldMap().get(pullRelatedForm.getFieldVariable());
        if (field instanceof RelationshipField) {

            final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field, user);
            final boolean pullParents = APILocator.getRelationshipAPI().isParentField(relationship, field);
            final List<Contentlet> retrievedContentlets = ContentUtils.getPullResults(relationship,
                    pullRelatedForm.getIdentifier(),
                    pullRelatedForm.getCondition() != null?
                            pullRelatedForm.getCondition():
                            ContentUtils.addDefaultsToQuery(pullRelatedForm.getCondition(), editOrPreviewMode, request),
                    pullRelatedForm.getLimit(), pullRelatedForm.getOffset(), pullRelatedForm.getOrderBy(),
                    user, tmDate, pullParents, langId, editOrPreviewMode? null : true);

            return Response.ok(new ResponseEntityView(retrievedContentlets.stream()
                        .map(contentItem -> WorkflowHelper.getInstance().contentletToMap(contentItem)).collect(Collectors.toList())
                    )).build();
        }

        Logger.debug(this, ()-> "The field:" + pullRelatedForm.getFieldVariable() + " is not a relationship");
        throw new IllegalArgumentException("The field:" + pullRelatedForm.getFieldVariable() + " is not a relationship");
    }
}
