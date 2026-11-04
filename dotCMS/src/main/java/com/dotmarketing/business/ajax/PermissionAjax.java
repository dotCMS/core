package com.dotmarketing.business.ajax;

import com.dotmarketing.beans.Identifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionableObjectDWR;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.job.ResetPermissionsJob;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 *
 * AJAX controller for permission related operations
 *
 * @author davidtorresv
 *
 */
public class PermissionAjax {

    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();

	/**
	 * Retrieves a list of roles and its associated permissions for the given asset
	 * @param assetId
	 * @return
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 * @throws DotRuntimeException
	 * @throws DotSecurityException
	 */
	public List<Map<String, Object>> getAssetPermissions(String assetId, Long languageId) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException {

		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userWebAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

		PermissionAPI permAPI = APILocator.getPermissionAPI();

		List<Map<String, Object>> toReturn = new ArrayList<>();
		Map<String, Map<String, Object>> roles = new TreeMap<>();

		Permissionable perm = retrievePermissionable(assetId, languageId, user, respectFrontendRoles);

		List<Permission> assetPermissions = permAPI.getPermissions(perm, true);
		for(Permission p : assetPermissions) {
			addPermissionToRoleList(perm, p, roles, false);
		}
		if(perm.isParentPermissionable()) {
			List<Permission> iheritablePermissions = permAPI.getInheritablePermissions(perm, true);
			for(Permission p : iheritablePermissions) {
				addPermissionToRoleList(perm, p, roles, true);
			}
		}

		toReturn.addAll(roles.values());

		Collections.sort(toReturn,new Comparator<Map<String, Object>> () {
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				return ((String)o1.get("name")).compareTo((String)o2.get("name"));
			}
		});

		return toReturn;
	}

	@SuppressWarnings("unchecked")
	private void addPermissionToRoleList(Permissionable perm, Permission p, Map<String, Map<String, Object>> roles, boolean inheritable) throws DotDataException, DotSecurityException {
		Map<String, Permissionable> inodeCache = new HashMap<>();

		RoleAPI roleAPI = APILocator.getRoleAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();

		String roleId = p.getRoleId();
		Map<String, Object> roleMap = roles.get(roleId);
		if(roleMap == null) {
			Role role = roleAPI.loadRoleById(roleId);
			if(role == null)
				return;
			roleMap = role.toMap();
			roles.put(role.getId(), roleMap);
			if(!inheritable) {
				if(p.getInode().equals(perm.getPermissionId())) {
					roleMap.put("inherited", false);
				} else {
					roleMap.put("inherited", true);
					String assetInode = p.getInode();

					//try from the cache
					Permissionable permParent = inodeCache.get(p.getInode());

					if (permParent == null){
						//let's check if it is a folder
						permParent = CacheLocator.getFolderCache().getFolder(p.getInode());
					}

					if(permParent == null) {
						// because identifiers are not Inodes, we need to do a double lookup

						if ( Host.SYSTEM_HOST.equals(assetInode) ) {
							permParent = hostAPI.find(assetInode, systemUser, false);
						} else {
						    permParent = InodeUtils.getInode(assetInode);
						}

						if(permParent !=null && InodeUtils.isSet(permParent.getPermissionId())){
							inodeCache.put(permParent.getPermissionId(), permParent);

						}
						else{
							permParent = APILocator.getIdentifierAPI().find(assetInode);
							if(permParent != null && InodeUtils.isSet(permParent.getPermissionId())){
								inodeCache.put(permParent.getPermissionId(), permParent);
							}
						}
					}
					if(permParent instanceof Folder) {
						roleMap.put("inheritedFromType", "folder");
						roleMap.put("inheritedFromPath", APILocator.getIdentifierAPI().find(((Folder)permParent).getIdentifier()).getPath());
						roleMap.put("inheritedFromId", ((Folder)permParent).getInode());
					} else if (permParent instanceof Structure) {
					    roleMap.put("inheritedFromType", "structure");
                        roleMap.put("inheritedFromPath", ((Structure) permParent).getName());
                        roleMap.put("inheritedFromId", ((Structure) permParent).getInode());
					} else if (permParent instanceof ContentType) {
                        roleMap.put("inheritedFromType", "structure");
                        final Structure contentType = new StructureTransformer(ContentType.class.cast(permParent)).asStructure();
                        this.contentletAPI.refresh(contentType);
                        roleMap.put("inheritedFromPath", contentType.getName());
                        roleMap.put("inheritedFromId", contentType.getInode());
					} else if (permParent instanceof Category) {
						roleMap.put("inheritedFromType", "category");
						roleMap.put("inheritedFromPath", ((Category)permParent).getCategoryName());
						roleMap.put("inheritedFromId", ((Category)permParent).getInode());
					} else if ( permParent instanceof Host ) {
						roleMap.put("inheritedFromType", "host");
						roleMap.put("inheritedFromPath", ((Host) permParent).getHostname());
						roleMap.put("inheritedFromId", ((Host) permParent).getIdentifier());
					} else {
						Host host = hostAPI.find(assetInode, systemUser, false);
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
		Map<String, Object> permissionMap = p.getMap();
		if(!inheritable) {
			permissionMap.put("type", PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
		}
		Logger.info(this, "##=> permissionMap: " + permissionMap.toString());
		rolePermissions.add(permissionMap);
	}

	/**
	 * Replaces the individual permissions of the given asset. An empty list removes all of them, so
	 * that the asset inherits from its parent again. Either way the logged-in user must have
	 * {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS} on the asset.
	 *
	 * @param assetId     the identifier or inode of the asset
	 * @param language    the language of the asset, when it is a contentlet
	 * @param permissions one map per role: the role id plus the permission bits per asset type
	 * @param reset       whether children that inherit from the asset are reset as well
	 * @throws DotSecurityException if the user can't read the asset or can't edit its permissions
	 * @throws DotDataException     if no asset matches the given id
	 */
	public void saveAssetPermissions(String assetId, Long language, List<Map<String, String>> permissions, boolean reset) throws Exception {

		HibernateUtil.startTransaction();

		try {
			UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			HttpServletRequest request = ctx.getHttpServletRequest();

			//Retrieving the current user
			User user = userWebAPI.getLoggedInUser(request);
			boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

			PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			Permissionable asset = retrievePermissionable(assetId, language, user, respectFrontendRoles);
			checkEditPermissions(asset, user);

			List<Permission> newSetOfPermissions = new ArrayList<>();
			for(Map<String, String> permission: permissions) {
				String roleId = permission.get("roleId");
				String individualPermission = permission.get("individualPermission");
				if(individualPermission != null ) {
					newSetOfPermissions.add(new Permission(asset.getPermissionId(), roleId, Integer.parseInt(individualPermission), true));
					//If a structure we need to save permissions inheritable by children content
					if(asset instanceof Structure || asset instanceof ContentType) {
						newSetOfPermissions.add(new Permission(Contentlet.class.getCanonicalName(), asset.getPermissionId(), roleId,
								Integer.parseInt(individualPermission), true));
					}
					//If a category we need to save permissions inheritable by children categories
					if(asset instanceof Category) {
						newSetOfPermissions.add(new Permission(Category.class.getCanonicalName(), asset.getPermissionId(), roleId,
								Integer.parseInt(individualPermission), true));
					}
				}
				String containersPermission = permission.get("containersPermission");
				if(containersPermission != null) {
					newSetOfPermissions.add(new Permission(Container.class.getCanonicalName(), asset.getPermissionId(),
							roleId, Integer.parseInt(containersPermission), true));
				}

				String pagesPermission = permission.get("pagesPermission");
				if(pagesPermission != null) {
					newSetOfPermissions.add(new Permission(IHTMLPage.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(pagesPermission), true));
				}
				String foldersPermission = permission.get("foldersPermission");
				if(foldersPermission != null) {
					newSetOfPermissions.add(new Permission(Folder.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(foldersPermission), true));
				}
				String contentPermission = permission.get("contentPermission");
				if(contentPermission != null) {
					newSetOfPermissions.add(new Permission(Contentlet.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(contentPermission), true));
				}
				String linksPermission = permission.get("linksPermission");
				if(linksPermission != null) {
					newSetOfPermissions.add(new Permission(Link.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(linksPermission), true));
				}
				String templatesPermission = permission.get("templatesPermission");
				if(templatesPermission != null) {
					newSetOfPermissions.add(new Permission(Template.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(templatesPermission), true));
				}
				String templateLayoutsPermission = permission.get("templateLayoutsPermission");
				if(templateLayoutsPermission != null) {
					newSetOfPermissions.add(new Permission(TemplateLayout.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(templateLayoutsPermission), true));
				}
				String structurePermission = permission.get("structurePermission");
				if(structurePermission != null) {
					newSetOfPermissions.add(new Permission(Structure.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(structurePermission), true));
				}
				String categoriesPermissions = permission.get("categoriesPermissions");
				if(categoriesPermissions != null) {
					newSetOfPermissions.add(new Permission(Category.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(categoriesPermissions), true));
				}
                String rulesPermissions = permission.get("rulesPermissions");
				if(rulesPermissions != null) {
					newSetOfPermissions.add(new Permission(Rule.class.getCanonicalName(), asset.getPermissionId(), roleId,
							Integer.parseInt(rulesPermissions), true));
				}
			}

			if(newSetOfPermissions.size() > 0) {
				// NOTE: Method "assignPermissions" is deprecated in favor of "save", which has subtle functional differences. Please take these differences into consideration if planning to replace this method with the "save"
				permissionAPI.assignPermissions(newSetOfPermissions, asset, user, respectFrontendRoles);

				if(reset && asset.isParentPermissionable()) {
					ResetPermissionsJob.triggerJobImmediately(asset);
				}
			} else {
				permissionAPI.removePermissions(asset);
			}
			
			HibernateUtil.closeAndCommitTransaction();
		} catch (DotSecurityException e) {
			// Already written to the security log by the check that refused it
			HibernateUtil.rollbackTransaction();
			throw e;
		} catch (Exception e) {
		    Logger.warn(this, e.getMessage(), e);
			HibernateUtil.rollbackTransaction();
			throw e;
		}
		finally {
		    HibernateUtil.closeSession();
		}
	}

	/**
	 * Removes every permission of the given asset, so that it inherits its permissions from its
	 * parent again. The logged-in user must have {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS}
	 * on the asset.
	 *
	 * @param assetId    the identifier or inode of the asset
	 * @param languageId the language of the asset, when it is a contentlet
	 * @throws DotSecurityException if the user can't read the asset or can't edit its permissions
	 * @throws DotDataException     if no asset matches the given id, or the permissions can't be removed
	 */
	public void resetAssetPermissions (String assetId, Long languageId) throws DotDataException, PortalException, SystemException, DotSecurityException {

		HibernateUtil.startTransaction();

		try {
			UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			HttpServletRequest request = ctx.getHttpServletRequest();

			//Retrieving the current user
			User user = userWebAPI.getLoggedInUser(request);
			boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

			PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			Permissionable asset = retrievePermissionable(assetId, languageId, user, respectFrontendRoles);
			checkEditPermissions(asset, user);
			permissionAPI.removePermissions(asset);

			HibernateUtil.closeAndCommitTransaction();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			throw e;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Fails unless the id resolved to an asset and the user has
	 * {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS} on it. Front-end roles don't count, as in
	 * the REST permission endpoints. Only back-end users reach these DWR methods (front-end sessions
	 * are turned away by the back-end login filter), so the request's {@code respectFrontendRoles}
	 * is already {@code false} here; it is fixed rather than passed through so this check can't
	 * drift from the REST one.
	 *
	 * @param asset the asset whose permissions are about to change
	 * @param user  the logged-in user
	 * @throws DotDataException     if the id didn't resolve to an asset
	 * @throws DotSecurityException if the user can't edit the asset's permissions
	 */
	private void checkEditPermissions(final Permissionable asset, final User user)
			throws DotDataException, DotSecurityException {

		if (asset == null || !UtilMethods.isSet(asset.getPermissionId())) {
			throw new DotDataException("No asset found for the given id");
		}
		requirePermission(asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, "alter permissions on");
	}

	/**
	 * Fails unless the user has the given permission on the asset, front-end roles not counting
	 * (see {@link #checkEditPermissions(Permissionable, User)}). A refusal is written to the security
	 * log with the user id and the asset's permission id only.
	 *
	 * @param asset          the asset to check
	 * @param permissionType a {@code PermissionAPI.PERMISSION_*} bit
	 * @param user           the logged-in user
	 * @param action         what the user tried to do, for the message, e.g. "read"
	 * @throws DotSecurityException if the user doesn't have the permission
	 */
	private void requirePermission(final Permissionable asset, final int permissionType,
			final User user, final String action) throws DotDataException, DotSecurityException {

		if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, permissionType, user, false)) {
			final String message = "User id: " + user.getUserId() + " does not have permission to "
					+ action + " asset " + asset.getPermissionId();
			SecurityLogger.logInfo(PermissionAjax.class, message);
			throw new DotSecurityException(message);
		}
	}

	/**
	 * Finds the asset for the given id: a site, contentlet, folder, template, link, container,
	 * content type or category. Links, containers and any other inode are looked up without a
	 * user, so for them the user's READ permission is checked here, with front-end roles not
	 * counting for the same reason as in {@link #checkEditPermissions(Permissionable, User)};
	 * the other finders check it themselves. When nothing matches, or the id only resolves to a
	 * plain {@link Inode} (e.g. a contentlet by inode), that Inode is returned as is: it has no
	 * permissions of its own, so a read finds none.
	 *
	 * @param assetId              the identifier or inode of the asset
	 * @param language             the language of the asset, when it is a contentlet
	 * @param user                 the logged-in user
	 * @param respectFrontendRoles whether front-end roles count for the user in the finders
	 * @return the asset, or an empty {@link Inode} when nothing matches
	 * @throws DotSecurityException if the user can't read the asset
	 */
	@CloseDBIfOpened
	private Permissionable retrievePermissionable (String assetId, Long language, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		HostAPI hostAPI = APILocator.getHostAPI();
		Permissionable perm = null;
		boolean foundWithoutUser = false;

		//Determining the type
		try{
			//Host?
			perm = hostAPI.find(assetId, user, respectFrontendRoles);
		}
		catch(DotSecurityException e){
			// The id is a site the user can't read
			throw e;
		}
		catch(Exception e){
			// Not a site, keep looking
		}
		
		if(perm == null) {
			//Content?
			ContentletAPI contAPI = APILocator.getContentletAPI();
			
			try {
				if(language == null || language <= 0){
					language=APILocator.getLanguageAPI().getDefaultLanguage().getId();
				}
				perm = contAPI.findContentletByIdentifier(assetId, false, language, user, respectFrontendRoles);
			} catch (DotContentletStateException e) {

			}
		}

		if (perm == null) {
			//we check if it is a folder
			perm = APILocator.getFolderAPI().find(assetId, user,respectFrontendRoles);
		}

		if(perm == null) {

			DotConnect dc = new DotConnect();
			List<Map<String, String>> results;
			String assetType ="";
			dc.setSQL("Select asset_type from identifier where id =?");
			dc.addParam(assetId);
			List<Map<String, String>> assetResult = dc.loadResults();
			
			if(assetResult.size()>0){
                // It could be:
                // 1. contentlet
                // 2. htmlpage
                // 3. template
                // 4. links
                // 5. containers: table has different name: dot_containers
                assetType = assetResult.get(0).get("asset_type");
			}
			
			if(UtilMethods.isSet(assetType)){
				dc.setSQL("select i.inode, type from inode i," +
						Inode.Type.valueOf(assetType.toUpperCase()).getTableName() +
						" a where i.inode = a.inode and a.identifier = ?");
				dc.addParam(assetId);
				results = dc.loadResults();

				if(results.size() > 0) {
					String type = results.get(0).get("type");
					String inode = results.get(0).get("inode");
					if(assetType.equals(Identifier.ASSET_TYPE_TEMPLATE)){
						perm = APILocator.getTemplateAPI().find(inode,user,respectFrontendRoles);
					} else {
						perm = InodeFactory.getInode(inode, InodeUtils.getClassByDBType(type));
						foundWithoutUser = true;
					}
				}
			}
		}
		
		if(perm == null){
			try {
				perm = APILocator.getContentTypeAPI(user).find(assetId);
			} catch (NotFoundInDbException e) {
				// Do nothing as "perm" is left as null
				// Code above is trying to lookup a Content-Type by using an id
				// that might not correspond to a Content-Type (assetId)
			}
		}

		if ( perm == null ) {
			try {
				// Now trying with categories
				perm = APILocator.getCategoryAPI().find(assetId, user, respectFrontendRoles);
			} catch (NotFoundInDbException e) {
				// Do nothing
			}
		}

		if(perm == null || !UtilMethods.isSet(perm.getPermissionId())) {
			perm = InodeFactory.getInode(assetId, Inode.class);
			foundWithoutUser = true;
		}

		// A plain Inode (e.g. a contentlet loaded by inode) has no permissions of its own, so a read
		// returns nothing and a change is still refused by the EDIT_PERMISSIONS check
		if (foundWithoutUser && perm != null && perm.getClass() != Inode.class
				&& UtilMethods.isSet(perm.getPermissionId())) {
			requirePermission(perm, PermissionAPI.PERMISSION_READ, user, "read");
		}

		return perm;
	}

	/**
	 * Gives the asset its own copy of the permissions it inherits from its parent, so they can be
	 * changed on the asset alone. The logged-in user must have
	 * {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS} on the asset.
	 *
	 * @param assetId    the identifier or inode of the asset
	 * @param languageId the language of the asset, when it is a contentlet
	 * @throws DotSecurityException if the user can't read the asset or can't edit its permissions
	 * @throws DotDataException     if no asset matches the given id
	 */
	public void permissionIndividually(String assetId, Long languageId) throws Exception {
		HibernateUtil.startTransaction();
	   	try {
	   		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
	   		WebContext ctx = WebContextFactory.get();
	   		HttpServletRequest request = ctx.getHttpServletRequest();

	   		//Retrieving the current user
	   		User user = userWebAPI.getLoggedInUser(request);
	   		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

	   		PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	   		Permissionable asset = retrievePermissionable(assetId, languageId, user, respectFrontendRoles);
	   		checkEditPermissions(asset, user);
	   		Permissionable parentPermissionable = APILocator.getPermissionAPI().findParentPermissionable(asset);

	   		if(parentPermissionable!=null){
	   			permissionAPI.permissionIndividually(parentPermissionable, asset, user);
	   		}
	   		HibernateUtil.closeAndCommitTransaction();
	   	} catch (Exception e) {
	   		HibernateUtil.rollbackTransaction();
	   		throw e;
	   	} finally {
	   		HibernateUtil.closeSession();
	   	}
	}

    public PermissionableObjectDWR getAsset(String inodeOrIdentifier) throws DotHibernateException {
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		PermissionableObjectDWR asset = new PermissionableObjectDWR();
		PermissionAPI permAPI = APILocator.getPermissionAPI();
		Permissionable p = null;

		asset.setId(inodeOrIdentifier);

		try {
			// Retrieving the current user
			User user = userWebAPI.getLoggedInUser(request);
			Structure hostStrucuture = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");

			if (InodeFactory.isInode(inodeOrIdentifier)) {
				Inode inode = InodeFactory.find(inodeOrIdentifier);
				p = inode;
				asset.setType(((Permissionable) inode).getClass().getName());
			} else {
				ContentletAPI contAPI = APILocator.getContentletAPI();
				Contentlet content = contAPI.find(inodeOrIdentifier, user, false);

				if (UtilMethods.isSet(content) && content.getStructureInode().equals(hostStrucuture.getInode())) {
					p = content;
					asset.setType(Host.class.getName());
				}
			}

			if(p==null) return null;

			asset.setIsFolder(p instanceof Folder);
			asset.setIsHost((p instanceof Host) || ((p instanceof Contentlet) && ((Contentlet)p).getStructureInode().equals(hostStrucuture.getInode())));
			asset.setIsParentPermissionable(p.isParentPermissionable());
			asset.setDoesUserHavePermissionsToEdit(permAPI.doesUserHavePermission(p, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user));
		} catch(DotHibernateException de) {
			throw de;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return asset;
	}


}
