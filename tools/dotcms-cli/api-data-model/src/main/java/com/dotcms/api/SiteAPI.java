package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CopySiteRequest;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

/**
 * Entry point to dotCMS Site Rest API
 */
@Path("/v1/site")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = { @Tag(name = "Site")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface SiteAPI {

    /**
     * Returns the Site that matches the specified search criteria
     * @param filter
     * @param showArchived
     * @param showLive
     * @param showSystem
     * @param page
     * @param perPage
     * @return
     */
    @GET
    @Operation(
            summary = " Returns the Site that matches the specified search criteria"
    )
    ResponseEntityView<List<Site>> getSites(
            @QueryParam("filter") String filter,
            @QueryParam("archive") Boolean showArchived,
            @QueryParam("live") Boolean showLive,
            @QueryParam("system") Boolean showSystem,
            @QueryParam("page") Integer page,
            @QueryParam("per_page") Integer perPage
    );

    @GET
    @Path("/{siteId}")
    @Operation(
            summary = " Returns the Site that matches the specified search criteria"
    )
    ResponseEntityView<SiteView> findById(@PathParam("siteId") String siteId);

    @POST
    @Path("/_byname")
    @Operation(
            summary = " Returns the Site that matches the specified search criteria"
    )
    ResponseEntityView<SiteView> findByName(final GetSiteByNameRequest request);

    @GET
    @Path("/defaultSite")
    @Operation(
            summary = " Returns the Site marked as default"
    )
    ResponseEntityView<Site>defaultSite();


    @POST
    @Operation(
            summary = " Creates a new site"
    )
    ResponseEntityView<SiteView> create(final CreateUpdateSiteRequest request);

    @PUT
    @Operation(
            summary = " Updates an existing site"
    )
    ResponseEntityView<SiteView> update(
            @QueryParam("id") final String  siteIdentifier,
            final CreateUpdateSiteRequest request
    );

    @PUT
    @Path("/{siteId}/_makedefault")
    @Operation(
            summary = " Marks a Site as default"
    )
    ResponseEntityView<Boolean> makeDefault(
            @PathParam("siteId") final String siteId
    );

    @PUT
    @Path("/{siteId}/_archive")
    @Operation(
            summary = " Archives a site"
    )
    ResponseEntityView<SiteView> archive(
            @PathParam("siteId") final String siteId
    );

    @PUT
    @Path("/{siteId}/_unarchive")
    @Operation(
            summary = " un-archives a site"
    )
    ResponseEntityView<SiteView> unarchive(
            @PathParam("siteId") final String siteId
    );

    @PUT
    @Path("/{siteId}/_publish")
    @Operation(
            summary = " Publish a site"
    )
    ResponseEntityView<SiteView> publish(
            @PathParam("siteId") final String siteId
    );

    @PUT
    @Path("/{siteId}/_unpublish")
    @Operation(
            summary = " Unpublish a site"
    )
    ResponseEntityView<SiteView> unpublish(
            @PathParam("siteId") final String siteId
    );

    @DELETE
    @Path("/{siteId}")
    @Operation(
            summary = " Delete site"
    )
    ResponseEntityView<Boolean> delete(
            @PathParam("siteId")  final String  siteId
    );

    @PUT
    @Path("/_copy")
    @Operation(
            summary = " Copy site"
    )
    ResponseEntityView<SiteView> copy(
            CopySiteRequest request
    );


}
