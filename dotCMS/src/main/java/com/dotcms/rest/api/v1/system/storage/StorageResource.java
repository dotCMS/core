package com.dotcms.rest.api.v1.system.storage;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.storage.StorageType;
import com.dotmarketing.business.Role;
import com.dotmarketing.quartz.job.DeleteUserJob;
import com.dotmarketing.quartz.job.ReplicateStoragesJob;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;

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
 * Api for Storages
 * @author jsanca
 */
@Path("/v1/storages")
public class StorageResource {

    private final WebResource          webResource;

    public StorageResource() {

        this(new WebResource());
    }

    @VisibleForTesting
    public StorageResource(final WebResource webResource) {

        this.webResource = webResource;
    }

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
     * Fires a replication from one storage to others
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     * @param fromStorageType {@link StorageType}
     * @param pathSegmentsStorageTypes {@link List} of {@link PathSegment}
     * @return Response
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

        Logger.debug(this, ()-> "Doing storage replication from: " +
                fromStorageType + ", to: " + pathSegmentsStorageTypes);

        if (!UtilMethods.isSet(fromStorageType)) {

            throw new IllegalArgumentException("The from storage is required, for example FILE_SYSTEM on this: v1/storages/chain/FILE_SYSTEM/to/DB/S3");
        }

        if (!UtilMethods.isSet(pathSegmentsStorageTypes) || pathSegmentsStorageTypes.isEmpty()) {

            throw new IllegalArgumentException("Have to have at least one storage type, for example: v1/storages/chain/FILE_SYSTEM/to/DB/S3");
        }

        final List<StorageType> toStorageType = pathSegmentsStorageTypes.stream().
                map(segment -> StorageType.valueOf(segment.getPath())).collect(Collectors.toList());

        ReplicateStoragesJob.triggerReplicationStoragesJob(fromStorageType, toStorageType, initDataObject.getUser());
        return Response.ok(new ResponseEntityBooleanView(true)).build();
    }
} // E:O:F:StorageResource.
