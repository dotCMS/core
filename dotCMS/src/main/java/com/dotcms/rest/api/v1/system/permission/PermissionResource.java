package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulate the expose functionality for permissions
 * @author jsanca
 */
@Path("/v1/permissions")
public class PermissionResource {

    private final WebResource      webResource;
    private final PermissionHelper permissionHelper;
    private final UserAPI          userAPI;

    public PermissionResource() {

        this(new WebResource(), PermissionHelper.getInstance(), APILocator.getUserAPI());
    }
    @VisibleForTesting
    public PermissionResource(final WebResource      webResource,
                              final PermissionHelper permissionHelper,
                              final UserAPI          userAPI) {

        this.webResource      = webResource;
        this.permissionHelper = permissionHelper;
        this.userAPI          = userAPI;
    }

    /**
     * Load a map indexed by permissionable types (optional all if not passed any) and permissions (READ, WRITE)
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @param userid      {@link String}
     * @param permissions {@link String}
     * @param permissionableTypes {@link String}
     * @return Response
     * @throws DotDataException
     */
    @GET
    @Path("/_permissionsbypermissiontype/userid/{userid}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getPermissionsByPermissionType(final @Context HttpServletRequest request,
                                                   final @Context HttpServletResponse response,
                                                   final @PathParam("userid") String userid,
                                                   final @QueryParam("permission") String permissions,
                                                   final @QueryParam("permissiontype") String permissionableTypes)
            throws DotDataException, DotSecurityException {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "GetPermissionsByPermissionType, permission: " +
                permissions + "permissiontype: " + permissionableTypes);

        final User user = this.userAPI.loadUserById(userid);

        final Map<String, Map<String, Boolean>> permissionsMap = this.permissionHelper.getPermissionsByPermissionType(user,
                null != permissions? Stream.of(permissions.split(StringPool.COMMA)).
                        map(this.permissionHelper::fromStringToPermissionInt).collect(Collectors.toList()): null,
                null != permissionableTypes? Arrays.asList(permissionableTypes.split(StringPool.COMMA)): null
                );

        return Response.ok(new ResponseEntityView(permissionsMap)).build();
    }
}
