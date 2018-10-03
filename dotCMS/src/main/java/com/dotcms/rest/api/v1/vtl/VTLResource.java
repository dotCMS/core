package com.dotcms.rest.api.v1.vtl;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache.DotJSONCacheKey;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.*;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;

@Path("/vtl")
public class VTLResource {

    private final WebResource webResource = new WebResource();
    @VisibleForTesting
    static final String VTL_PATH = "/application/apivtl";
    private final HostAPI hostAPI;
    private final IdentifierAPI identifierAPI;
    private final ContentletAPI contentletAPI;
    private final String FILE_EXTENSION = ".vtl";

    public VTLResource() {
        this(APILocator.getHostAPI(), APILocator.getIdentifierAPI(), APILocator.getContentletAPI());
    }

    @VisibleForTesting
    VTLResource(final HostAPI hostAPI, final IdentifierAPI identifierAPI, final ContentletAPI contentletAPI) {
        this.hostAPI = hostAPI;
        this.identifierAPI = identifierAPI;
        this.contentletAPI = contentletAPI;
    }

    @GET
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Path("/{folder}/{path: .*}")
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        @PathParam("path") final String pathParams) {

        final InitDataObject initDataObject = this.webResource.init
                (pathParams, false, request, false, null);
        final User user = initDataObject.getUser();
        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        final DotJSONCacheKey dotJSONCacheKey = getDotJSONCacheKey(request, initDataObject);

        final Optional<DotJSON> dotJSONOptional = CacheLocator.getDotJSONCache()
                .get(dotJSONCacheKey);

        if(dotJSONOptional.isPresent()) {
            return Response.ok(dotJSONOptional.get()).build();
        }

        DotJSON<String, String> dotJSON;

        try {
            final Host site = this.hostAPI.resolveHostName(request.getServerName(), APILocator.systemUser(), false);
            final String getFilePath = VTL_PATH + StringPool.SLASH + folderName + StringPool.SLASH
                    + HTTPMethod.GET.fileName + FILE_EXTENSION;
            final Identifier identifier = identifierAPI.find(site, getFilePath);
            final Contentlet getFileContent = contentletAPI.findContentletByIdentifier(identifier.getId(), true,
                    currentLanguage.getId(), user, true);
            final FileAsset getFileAsset = APILocator.getFileAssetAPI().fromContentlet(getFileContent);

            final org.apache.velocity.context.Context context = VelocityUtil.getInstance().getContext(request, response);
            context.put("urlParams", initDataObject.getParamsMap());
            context.put("queryParams", queryParameters);
            context.put("dotJSON", new DotJSON());

            final StringWriter evalResult = new StringWriter();

            try (final InputStream fileAssetIputStream = getFileAsset.getInputStream()) {
                VelocityUtil.getEngine().evaluate(context, evalResult, "", fileAssetIputStream);
                dotJSON = (DotJSON<String, String>) context.get("dotJSON");

                if(dotJSON.size()==0) { // If dotJSON is not used let's return the raw evaluation of the velocity file
                    return Response.ok(evalResult.toString()).build();
                }

                // let's add it to cache
                CacheLocator.getDotJSONCache().add(dotJSONCacheKey, dotJSON);
            }
        } catch(DotContentletStateException e) {
            final String errorMessage = "Unable to find velocity file '" + HTTPMethod.GET.fileName + FILE_EXTENSION
                    + "' under path '" + VTL_PATH + "'";
            Logger.error(this, errorMessage, e);
            return ResponseUtil.mapExceptionResponse(new DotDataException(errorMessage));
        } catch(Exception e) {
            Logger.error(this,"Exception on VTL endpoint. GET method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return Response.ok(dotJSON).build();
    }

    private DotJSONCacheKey getDotJSONCacheKey(final HttpServletRequest request, final InitDataObject initDataObject) {
        final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        IPersona persona = null;
        final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request, false);

        if (visitor.isPresent() && visitor.get().getPersona() != null) {
            persona = visitor.get().getPersona();
        }

        final String requestURI = request.getRequestURI() + StringPool.QUESTION + request.getQueryString();

        return new DotJSONCacheKey(initDataObject.getUser(), language, requestURI, persona);
    }

    private enum HTTPMethod {
        GET("get"),
        POST("post"),
        PUT("put"),
        PATCH("patch"),
        DELETE("delete");

        private String fileName;

        HTTPMethod(String fileName) {
            this.fileName = fileName;
        }

        public String fileName() {
            return fileName;
        }
    }
}