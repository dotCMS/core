package com.dotcms.rest.api.v1.system.storage;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.storage.StorageType;
import com.dotmarketing.business.Role;
import com.dotmarketing.quartz.job.StorageReplicationJob;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This REST Endpoint allows users to interact with dotCMS Storage Providers and perform operations
 * such as data replication from one Provider to one or more Providers.
 *
 * @author jsanca
 */
@Path("/v1/storages")
@Tag(name = "System Storage")
public class StorageResource {

    private final WebResource webResource;

    @VisibleForTesting
    public StorageResource() {
        this(new WebResource());
    }

    @VisibleForTesting
    public StorageResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * This method is just a test method to check if the endpoint is up and running.
     *
     * @param request  The current instance of the {@link HttpServletRequest} object.
     * @param response The current instance of the {@link HttpServletResponse} object.
     *
     * @return A {@link Response} object with dummy confirmation message.
     */
    @NoCache
    @GET
    @Path("/hello")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response hello(@Context final HttpServletRequest request,
                          @Context final HttpServletResponse response) {
        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();
        return Response.ok(new ResponseEntityView<>("hello")).build();
    }

    /**
     * Fires the data replication process from one Storage Provider to one or more Providers. Here's
     * an example of what a GET request to replicate File System metadata to Redis would look like:
     * <pre>
     *   http://localhost:8080/api/v1/storages/chain/_replicate/FILE_SYSTEM/to/MEMORY
     * </pre>
     * If you want to replicate to the database as well, just add it to the path:
     * <pre>
     *   http://localhost:8080/api/v1/storages/chain/_replicate/FILE_SYSTEM/to/MEMORY/DB
     * </pre>
     *
     * @param request                  The current instance of the {@link HttpServletRequest}.
     * @param response                 The current instance of the  {@link HttpServletResponse}.
     * @param fromStorageType          The source {@link StorageType} containing the metadata.
     * @param pathSegmentsStorageTypes The {@link List} of {@link PathSegment} representing the
     *                                 Storage Types that the metadata will be replicated to.
     *
     * @return Response The Status 200 response in case no errors were found.
     */
    @NoCache
    @GET
    @Path("/chain/_replicate/{from}/to/{to: .+}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response replicateStorages(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @PathParam("from") final StorageType fromStorageType,
                                      @PathParam("to") final List<PathSegment> pathSegmentsStorageTypes) {
        final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, "Scheduling Storage Provider replication from: " +
                fromStorageType + ", to: " + pathSegmentsStorageTypes);

        if (!UtilMethods.isSet(fromStorageType)) {
            throw new IllegalArgumentException("The 'from' Storage Type is required");
        }
        if (UtilMethods.isNotSet(pathSegmentsStorageTypes)) {
            throw new IllegalArgumentException("At least one 'to' Storage Type is required");
        }

        final List<StorageType> toStorageType = pathSegmentsStorageTypes.stream().
                map(segment -> StorageType.valueOf(segment.getPath())).collect(Collectors.toList());

        StorageReplicationJob.triggerReplicationStoragesJob(fromStorageType, toStorageType, initDataObject.getUser());
        return Response.ok(new ResponseEntityBooleanView(true)).build();
    }

} // E:O:F:StorageResource.
