package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.asset.SearchByPathRequest;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

/**
 * Entry point to dotCMS Assets Rest API
 */
@Path("/v1/assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = {@Tag(name = "Assets")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface AssetAPI {

    @POST
    @Path("/")
    @Operation(
            summary = "Lists the files and directories in the specified path"
    )
    ResponseEntityView<FolderView> folderByPath(final SearchByPathRequest request);

    @POST
    @Path("/")
    @Operation(
            summary = "Retrieves the asset information of the specified path"
    )
    ResponseEntityView<AssetVersionsView> assetByPath(final SearchByPathRequest request);

    @POST
    @Path("/_download")
    @Operation(
            summary = "Retrieve a specific asset"
    )
    InputStream download(final SearchByPathRequest request);

}
