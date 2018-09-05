package com.dotcms.rest.api.v1.diy;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.javax.ws.rs.*;
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
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

@Path("/v1/diy")
public class DIYResource {

    private final WebResource webResource = new WebResource();
    private final String DIYPath = "/diy";
    private final HostAPI hostAPI;
    private final IdentifierAPI identifierAPI;
    private final ContentletAPI contentletAPI;
    private final String GET_FILE_NAME = "get";
    private final String FILE_EXTENSION = ".vtl";

    public DIYResource() {
        this(APILocator.getHostAPI(), APILocator.getIdentifierAPI(), APILocator.getContentletAPI());
    }

    @VisibleForTesting
    DIYResource(final HostAPI hostAPI, final IdentifierAPI identifierAPI, final ContentletAPI contentletAPI) {
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
                (pathParams, true, request, true, null);

        final User user = initDataObject.getUser();
        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(request);

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        Map<String, String> dotJSON;

        try {
            Host site = this.hostAPI.resolveHostName(request.getServerName(), user, false);
            final String getFilePath = DIYPath + "/" + folderName + "/" + GET_FILE_NAME + FILE_EXTENSION;
            Identifier identifier = identifierAPI.find(site, getFilePath);
            Contentlet getFileContent = contentletAPI.findContentletByIdentifier(identifier.getId(), true, currentLanguage.getId(), user, false);
            FileAsset getFileAsset = APILocator.getFileAssetAPI().fromContentlet(getFileContent);

            org.apache.velocity.context.Context context = VelocityUtil.getInstance().getContext(request, response);
            context.put("urlParams", initDataObject.getParamsMap());
            context.put("queryParams", queryParameters);
            context.put("dotJSON", new HashMap());

            StringWriter evalResult = new StringWriter();
            VelocityUtil.getEngine().evaluate(context, evalResult, "", getFileAsset.getInputStream());

            dotJSON = (Map<String, String>) context.get("dotJSON");
        } catch(Exception e) {
            Logger.error(this.getClass(),"Exception on DIY endpoint. GET method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return Response.ok(new ResponseEntityView(dotJSON)).build();
    }
}
