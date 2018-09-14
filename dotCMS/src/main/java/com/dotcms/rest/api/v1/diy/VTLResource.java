package com.dotcms.rest.api.v1.diy;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.*;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Path("/vtl")
public class VTLResource {

    private final WebResource webResource = new WebResource();
    private final String VTLPath = "/application/apivtl";
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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/{folder}/{path: .*}")
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                             @PathParam("path") final String pathParams) {

        final InitDataObject initDataObject = this.webResource.init
                (pathParams, false, request, false, null);
        final User user = initDataObject.getUser();
        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        Map<String, String> dotJSON = null;

        try {
            Host site = this.hostAPI.resolveHostName(request.getServerName(), user, false);
            final String getFilePath = VTLPath + "/" + folderName + "/" + HTTPMethod.GET.fileName + FILE_EXTENSION;
            Identifier identifier = identifierAPI.find(site, getFilePath);
            Contentlet getFileContent = contentletAPI.findContentletByIdentifier(identifier.getId(), true,
                    currentLanguage.getId(), user, false);
            FileAsset getFileAsset = APILocator.getFileAssetAPI().fromContentlet(getFileContent);

            org.apache.velocity.context.Context context = VelocityUtil.getInstance().getContext(request, response);
            context.put("urlParams", initDataObject.getParamsMap());
            context.put("queryParams", queryParameters);
            context.put("dotJSON", new HashMap());

            StringWriter evalResult = new StringWriter();

            try (final InputStream fileAssetIputStream = getFileAsset.getInputStream()) {
                VelocityUtil.getEngine().evaluate(context, evalResult, "", fileAssetIputStream);
                dotJSON = (Map<String, String>) context.get("dotJSON");
            }
        } catch(DotContentletStateException e) {
            final String errorMessage = "Unable to find velocity file '" + HTTPMethod.GET.fileName + FILE_EXTENSION
                    + "' under path '" + VTLPath + "'";
            Logger.error(this, errorMessage, e);
            return ResponseUtil.mapExceptionResponse(new DotDataException(errorMessage));
        } catch(Exception e) {
            Logger.error(this,"Exception on DIY endpoint. GET method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return Response.ok(new ResponseEntityView(dotJSON)).build(); //todo don't return like this
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