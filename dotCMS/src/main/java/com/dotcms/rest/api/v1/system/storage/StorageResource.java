package com.dotcms.rest.api.v1.system.storage;

import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.storage.StorageType;
import com.dotmarketing.business.Role;
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
    /**
     * Returns the providers associated to a group
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     * @param group {@link String}
     * @return Response
     */
    @NoCache
    @GET
    @Path("/chain/_replicate/{segment}/to/{segment}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response replicateStorages(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @PathParam("segment") final List<PathSegment> pathSegmentsStorageTypes) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Doing storage replication " + pathSegmentsStorageTypes);

        if (!UtilMethods.isSet(pathSegmentsStorageTypes)) {

            throw new IllegalArgumentException("The storage types are required");
        }

        if (pathSegmentsStorageTypes.size() >= 2) {

            throw new IllegalArgumentException("Have to have at least two storage types, for example: v1/storages/chain/FILE_SYSTEM/to/DB/S3");
        }

        final StorageType fromStorageType = StorageType.valueOf(pathSegmentsStorageTypes.get(0).getPath());
        final List<StorageType> toStorageType = pathSegmentsStorageTypes.subList(1, pathSegmentsStorageTypes.size()).stream().
                map(segment -> StorageType.valueOf(segment.getPath())).collect(Collectors.toList());

        // todo: call a job with the from and to storages to slowly replicate the elements from one to the others
        return Response.ok(new ResponseEntityBooleanView(true)).build();
    }
} // E:O:F:StorageResource.
