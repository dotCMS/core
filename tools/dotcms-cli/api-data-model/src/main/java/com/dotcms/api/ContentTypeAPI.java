package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.contenttype.FilterContentTypesRequest;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import com.dotcms.model.views.CommonViews.ContentTypeExternalView;
import com.dotcms.model.views.CommonViews.ContentTypeInternalView;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Entry point to dotCMS ContentType Rest API
 */
@Path("/v1/contenttype")
@RegisterRestClient(configKey = "legacy-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = { @Tag(name = "Content-Type")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface ContentTypeAPI {

    @GET
    @Operation(
            summary = "Get a list of Content-types for a given set of params"
    )
    @JsonView(ContentTypeInternalView.class)
    ResponseEntityView<List<ContentType>> getContentTypes(
            @QueryParam("filter") String filter,
            @QueryParam("page") Integer page,
            @QueryParam("per_page") Integer perPage,
            @QueryParam("orderby") String orderBy,
            @QueryParam("direction") String direction,
            @QueryParam("type") String type,
            @QueryParam("host") String host);

    @GET
    @Path("/id/{idOrVar}")
    @Operation(
            summary = "Get a specific Content-type for the given id or varName"
    )
    @JsonView(ContentTypeInternalView.class)
    ResponseEntityView<ContentType> getContentType(@PathParam("idOrVar") final String idOrVar,
            @QueryParam("languageId") final Long languageId,
            @QueryParam("live") final Boolean paramLive);


    @POST
    @Operation(
            summary = "Create a brand new CT instance"
    )
    @JsonView(ContentTypeInternalView.class)
    ResponseEntityView<List<ContentType>> createContentTypes(
            @JsonView(ContentTypeExternalView.class) final List<SaveContentTypeRequest> contentTypes);


    @PUT
    @Path("/id/{idOrVar}")
    @Operation(
            summary = "Save/Update a CT instance"
    )
    @JsonView(ContentTypeInternalView.class)
    ResponseEntityView<ContentType> updateContentType(@PathParam("idOrVar") final String idOrVar,
            @JsonView(ContentTypeExternalView.class) final SaveContentTypeRequest contentType);


    @DELETE
    @Path("/id/{idOrVar}")
    @Operation(
            summary = "Save/Update a CT instance"
    )
    ResponseEntityView<String> delete(@PathParam("idOrVar") final String idOrVar);


    @POST
    @Path("/_filter")
    @Operation(
            summary = "Get a list of Content-types for a given set of param"
    )
    @JsonView(ContentTypeInternalView.class)
    ResponseEntityView<List<ContentType>> filterContentTypes(final FilterContentTypesRequest request);

}
