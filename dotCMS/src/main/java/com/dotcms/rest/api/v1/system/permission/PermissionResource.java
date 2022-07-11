package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.system.role.ResponseEntityRoleMapView;
import com.dotcms.rest.api.v1.system.role.RoleView;
import com.dotcms.rest.api.v1.user.RestUser;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
     * Load a map of permission type indexed by permissionable types (optional all if not passed any) and permissions (READ, WRITE)
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @param userid      {@link String}
     * @param permissions {@link String}
     * @param permissionableTypes {@link String}
     * @return Response
     * @throws DotDataException
     */
    @GET
    @Path("/_bypermissiontype")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getPermissionsByPermissionType(final @Context HttpServletRequest request,
                                                   final @Context HttpServletResponse response,
                                                   final @QueryParam("userid")         String userid,
                                                   final @QueryParam("permission")     String permissions,
                                                   final @QueryParam("permissiontype") String permissionableTypes)
            throws DotDataException, DotSecurityException {

        final User userInvoker = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

        Logger.debug(this, ()-> "GetPermissionsByPermissionType, permission: " +
                permissions + "permissiontype: " + permissionableTypes);

        //
        if (!userInvoker.getUserId().equals(userid) && !userInvoker.isAdmin()) {

            throw new DotSecurityException("Only admin user can retrieve other users permissions");
        }

        final User user = this.userAPI.loadUserById(userid);

        final Map<String, Map<String, Boolean>> permissionsMap = this.permissionHelper.getPermissionsByPermissionType(user,
                null != permissions? Stream.of(permissions.split(StringPool.COMMA)).
                        map(this.permissionHelper::fromStringToPermissionInt).collect(Collectors.toList()): null,
                null != permissionableTypes? Arrays.asList(permissionableTypes.split(StringPool.COMMA)): null
                );

        return Response.ok(new ResponseEntityView(permissionsMap)).build();
    }

    /**
     * Load a map of permission type indexed by permissionable types (optional all if not passed any) and permissions (READ, WRITE)
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @param contentletId {@link String}
     * @param type      {@link String}
     * @return Response
     * @throws DotDataException
     */
    @GET
    @Path("/_bycontent")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Get permission for a Contentlet",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityPermissionView.class))),
                    @ApiResponse(responseCode = "403", description = "If not admin user")})
    public Response getByContentlet(final @Context HttpServletRequest request,
                                    final @Context HttpServletResponse response,
                                    final @QueryParam("contentletId")   String contentletId,
                                    final @DefaultValue("READ") @QueryParam("type")   String type)
            throws DotDataException, DotSecurityException {

        final User userInvoker = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

        Logger.debug(this, ()-> "getByContentlet, contentlet: " +
                contentletId + "type: " + type);

        if (!userInvoker.isAdmin()) {

            throw new DotSecurityException("Only admin user can retrieve other users permissions");
        }

        PermissionAPI.Type permissionType = "ALL".equalsIgnoreCase(type)?
                null:PermissionAPI.Type.valueOf(type);

        final List<Permission> permissions = APILocator.getPermissionAPI().getPermissions(
                APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletId));

        return Response.ok(new ResponseEntityPermissionView(permissions.stream()
                .filter(permission -> this.filter(permissionType, permission))
                .map(PermissionResource::from)
                .collect(Collectors.toList()))).build();
    }

    /**
     * Reset permissions for the given asset
     * @param assetId    {@link String}
     * @param languageId {@link Long}
     * @return
     * @throws DotDataException
     * @throws SystemException
     * @throws PortalException
     * @throws DotRuntimeException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_byasset")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Reset permissions for an asset",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBooleanView.class)))
    })
    public Response resetAssetPermissions (final @Context HttpServletRequest request,
                                       final @Context HttpServletResponse response,
                                       final @QueryParam("assetId")   String assetId,
                                       final @QueryParam("languageId")   Long languageId) throws Exception {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

        Logger.debug(this, ()-> "Doing reset of the asset: " + assetId + ", lang: " + languageId);

        final PageMode pageMode = PageMode.get(request);
        final boolean respectFrontendRoles = pageMode.respectAnonPerms;
        final Permissionable asset = retrievePermissionable(assetId, languageId, user, respectFrontendRoles);
        APILocator.getPermissionAPI().removePermissions(asset);

        return Response.ok(new ResponseEntityBooleanView(true)).build();
    }

    /**
     * Retrieves a list of roles and its associated permissions for the given asset
     * @param assetId    {@link String}
     * @param languageId {@link Long}
     * @return
     * @throws DotDataException
     * @throws SystemException
     * @throws PortalException
     * @throws DotRuntimeException
     * @throws DotSecurityException
     */
    @GET
    @Path("/_byasset")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Get permission for a Contentlet",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityRoleMapView.class))),
                    @ApiResponse(responseCode = "403", description = "If not admin user"),})
    public Response getAssetPermissions(final @Context HttpServletRequest request,
                                                         final @Context HttpServletResponse response,
                                                         final @QueryParam("assetId")   String assetId,
                                                         final @QueryParam("languageId")   Long languageId) throws Exception {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

        Logger.debug(this, ()-> "getAssetPermissions, assetId: " +
                assetId + "languageId: " + languageId);

        final PageMode pageMode = PageMode.get(request);
        final boolean respectFrontendRoles = pageMode.respectAnonPerms;
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final List<Map<String, Object>> toReturn     = new ArrayList<>();
        final Map<String, Map<String, Object>> roles = new HashMap<>();
        final Permissionable permissionable     = retrievePermissionable(assetId, languageId, user, respectFrontendRoles);
        final List<Permission> assetPermissions = permissionAPI.getPermissions(permissionable, true);
        for(final Permission permission : assetPermissions) {

            addPermissionToRoleList(permissionable, permission, roles, false);
        }

        if(permissionable.isParentPermissionable()) {

            final List<Permission> inheritablePermissions = permissionAPI.getInheritablePermissions(permissionable, true);
            for(final Permission permission : inheritablePermissions) {

                addPermissionToRoleList(permissionable, permission, roles, true);
            }
        }

        toReturn.addAll(roles.values());
        Collections.sort(toReturn, Comparator.comparing(o -> ((String) o.get("name"))));

        return Response.ok(new ResponseEntityRoleMapView(toReturn)).build();
    }

    private void addPermissionToRoleList(final Permissionable permissionable,
                                         final Permission permission,
                                         final Map<String, Map<String, Object>> roles,
                                         final boolean inheritable) throws DotDataException, DotSecurityException {

        final Map<String, Permissionable> inodeCache = new HashMap<>();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final String roleId = permission.getRoleId();
        Map<String, Object> roleMap = roles.get(roleId);
        if(roleMap == null) {

            final Role role = roleAPI.loadRoleById(roleId);
            if(role == null) {

                return;
            }

            roleMap = role.toMap();
            roles.put(role.getId(), roleMap);
            if(!inheritable) {
                if(permission.getInode().equals(permissionable.getPermissionId())) {

                    roleMap.put("inherited", false);
                } else {

                    roleMap.put("inherited", true);
                    final String assetInode = permission.getInode();

                    //try from the cache
                    Permissionable permissionableParent = inodeCache.get(permission.getInode());

                    if (permissionableParent == null){
                        //let's check if it is a folder
                        permissionableParent = CacheLocator.getFolderCache().getFolder(permission.getInode());
                    }

                    if(permissionableParent == null) {

                        // because identifiers are not Inodes, we need to do a double lookup
                        permissionableParent = Host.SYSTEM_HOST.equals(assetInode)?
                            hostAPI.find(assetInode, systemUser, false):InodeUtils.getInode(assetInode);

                        if(permissionableParent != null || InodeUtils.isSet(permissionableParent.getPermissionId())) {

                            inodeCache.put(permissionableParent.getPermissionId(), permissionableParent);
                        } else {

                            permissionableParent = APILocator.getIdentifierAPI().find(assetInode);
                            if(permissionableParent != null && InodeUtils.isSet(permissionableParent.getPermissionId())){
                                inodeCache.put(permissionableParent.getPermissionId(), permissionableParent);
                            }
                        }
                    }
                    // this should be abstract
                    if(permissionableParent instanceof Folder) {

                        final Folder folder = (Folder)permissionableParent;
                        roleMap.put("inheritedFromType", "folder");
                        roleMap.put("inheritedFromPath", APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath());
                        roleMap.put("inheritedFromId", folder.getInode());
                    } else if (permissionableParent instanceof Structure) {
                        final Structure structure = (Structure) permissionableParent;
                        roleMap.put("inheritedFromType", "structure");
                        roleMap.put("inheritedFromPath", structure.getName());
                        roleMap.put("inheritedFromId",   structure.getInode());
                    } else if (permissionableParent instanceof ContentType) {
                        final Structure contentType = new StructureTransformer(ContentType.class.cast(permissionableParent)).asStructure();
                        APILocator.getContentletAPI().refresh(contentType);
                        roleMap.put("inheritedFromType", "structure");
                        roleMap.put("inheritedFromPath", contentType.getName());
                        roleMap.put("inheritedFromId",   contentType.getInode());
                    } else if (permissionableParent instanceof Category) {
                        final Category category = (Category)permissionableParent;
                        roleMap.put("inheritedFromType", "category");
                        roleMap.put("inheritedFromPath", category.getCategoryName());
                        roleMap.put("inheritedFromId",   category.getInode());
                    } else if (permissionableParent instanceof Host) {
                        final Host host = (Host) permissionableParent;
                        roleMap.put("inheritedFromType", "host");
                        roleMap.put("inheritedFromPath", host.getHostname());
                        roleMap.put("inheritedFromId",   host.getIdentifier());
                    } else {
                        final Host host = hostAPI.find(assetInode, systemUser, false);
                        if(host != null) {
                            roleMap.put("inheritedFromType", "host");
                            roleMap.put("inheritedFromPath", host.getHostname());
                            roleMap.put("inheritedFromId", host.getIdentifier());
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> rolePermissions = (List<Map<String, Object>>) roleMap.get("permissions");
        if(rolePermissions == null) {
            rolePermissions = new ArrayList<>();
            roleMap.put("permissions", rolePermissions);
        }

        final Map<String, Object> permissionMap = permission.getMap();
        if(!inheritable) {

            permissionMap.put("type", PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        }

        Logger.info(this, "##=> permissionMap: " + permissionMap.toString());
        rolePermissions.add(permissionMap);
    }

    // todo: this method should be on an API or at least an UTIL
    @CloseDBIfOpened
    private Permissionable retrievePermissionable (final String assetId,
                                                   final Long language,
                                                   final User user,
                                                   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        //Determining the type
        // Host?
        Permissionable permissionable = Try.of(()->APILocator.getHostAPI().find(assetId, user, respectFrontendRoles)).getOrNull();
        if (null == permissionable) {
            //Content?
            permissionable = Try.of(() -> APILocator.getContentletAPI().findContentletByIdentifier(assetId, false,
                    ((language == null || language <= 0) ? APILocator.getLanguageAPI().getDefaultLanguage().getId() : language), user, respectFrontendRoles)).getOrNull();
        }

        if (permissionable == null) {
            //we check if it is a folder
            permissionable = APILocator.getFolderAPI().find(assetId, user, respectFrontendRoles);
        }

        if(permissionable == null) {

            ArrayList results = new ArrayList();
            String assetType ="";
            ArrayList assetResult = new DotConnect().setSQL("Select asset_type from identifier where id =?").addParam(assetId).loadResults();

            if(assetResult.size()>0){
                // It could be:
                // 1. contentlet
                // 2. htmlpage
                // 3. template
                // 4. links
                // 5. containers: table has different name: dot_containers
                assetType = (String) ((Map)assetResult.get(0)).get("asset_type");
            }

            if(UtilMethods.isSet(assetType)){

                results = new DotConnect().setSQL("select i.inode, type from inode i," +
                        Inode.Type.valueOf(assetType.toUpperCase()).getTableName() +
                        " a where i.inode = a.inode and a.identifier = ?").addParam(assetId).loadResults();

                if(results.size() > 0) {

                    final Map resultMap = (Map) results.get(0);
                    final String type   = (String) resultMap.get("type");
                    final String inode  = (String) resultMap.get("inode");
                    permissionable = assetType.equals(Identifier.ASSET_TYPE_TEMPLATE)?
                            APILocator.getTemplateAPI().find(inode,user,respectFrontendRoles):
                            InodeFactory.getInode(inode, InodeUtils.getClassByDBType(type));
                }
            }
        }

        if(permissionable == null) {
            // Now trying content type
            permissionable = Try.of(()->APILocator.getContentTypeAPI(user).find(assetId)).getOrNull();
        }

        if ( permissionable == null ) {
            // Now trying with categories
            permissionable = Try.of(()->APILocator.getCategoryAPI().find(assetId, user, respectFrontendRoles)).getOrNull();
        }

        if(permissionable == null || !UtilMethods.isSet(permissionable.getPermissionId())) {

            permissionable = InodeFactory.getInode(assetId, Inode.class);
        }

        return permissionable;
    }

    private boolean filter(final PermissionAPI.Type permissionType, final Permission permission) {

        return null != permissionType?
                permission.getPermission() == permissionType.getType(): true;
    }

    public static PermissionView from(Permission permission) {

        final PermissionView view = new PermissionView(permission.getId(), permission.getInode(), permission.getRoleId(),
                PermissionAPI.Type.findById(permission.getPermission()), permission.isBitPermission(), permission.getType());
        return view;
    }
}
