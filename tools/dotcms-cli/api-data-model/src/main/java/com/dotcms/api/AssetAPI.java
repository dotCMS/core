package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.asset.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
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
    ResponseEntityView<FolderView> folderByPath(final ByPathRequest request);

    @POST
    @Path("/")
    @Operation(
            summary = "Retrieves the asset information of the specified path"
    )
    ResponseEntityView<AssetVersionsView> assetByPath(final ByPathRequest request);

    @POST
    @Path("/_download")
    @Operation(
            summary = "Retrieve a specific asset"
    )
    InputStream download(final AssetRequest request);

    @PUT
    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            summary = "Pushes a file to a specific path"
    )
    ResponseEntityView<AssetView> push(@MultipartForm FileUploadData form);

    @POST
    @Path("/_archive")
    @Operation(
            summary = "Archives a specific asset"
    )
    ResponseEntityView<Boolean> archive(final ByPathRequest request);

    @POST
    @Path("/folders/_delete")
    @Operation(
            summary = "Deletes a specific folder"
    )
    ResponseEntityView<Boolean> deleteFolder(final ByPathRequest request);

}
