package com.dotmarketing.business.ajax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionableObjectDWR;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
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
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.job.ResetPermissionsJob;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
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
	public List<Map<String, Object>> getAssetPermissions(String assetId) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException {
		
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		
		//Retrieving the current user
		User user = userWebAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

		PermissionAPI permAPI = APILocator.getPermissionAPI();

		List<Map<String, Object>> toReturn = new ArrayList<Map<String,Object>>();
		Map<String, Map<String, Object>> roles = new TreeMap<String, Map<String, Object>>();
		
		Permissionable perm = retrievePermissionable(assetId, user, respectFrontendRoles); 
		
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
		Map<String, Permissionable> inodeCache = new HashMap<String, Permissionable>();
		
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
					if(permParent == null) {
						// because identifiers are not Inodes, we need to do a double lookup
						permParent = InodeFactory.getInode(assetInode, Inode.class);
						if(permParent !=null || InodeUtils.isSet(permParent.getPermissionId())){
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
						roleMap.put("inheritedFromPath", APILocator.getIdentifierAPI().find((Folder)permParent).getPath());
						roleMap.put("inheritedFromId", ((Folder)permParent).getInode());
					} else if (permParent instanceof Structure) {
						roleMap.put("inheritedFromType", "structure");
						roleMap.put("inheritedFromPath", ((Structure)permParent).getName());
						roleMap.put("inheritedFromId", ((Structure)permParent).getInode());
					} else if (permParent instanceof Category) {
						roleMap.put("inheritedFromType", "category");
						roleMap.put("inheritedFromPath", ((Category)permParent).getCategoryName());
						roleMap.put("inheritedFromId", ((Category)permParent).getInode());
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
			rolePermissions = new ArrayList<Map<String,Object>>();
			roleMap.put("permissions", rolePermissions);
		}
		Map<String, Object> permissionMap = p.getMap();
		if(!inheritable) {
			permissionMap.put("type", PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
		}
		Logger.info(this, "##=> permissionMap: " + permissionMap.toString());
		rolePermissions.add(permissionMap);		
	}
	
	public void saveAssetPermissions (String assetId, List<Map<String, String>> permissions, boolean reset) throws DotDataException, DotSecurityException, SystemException, PortalException  {
		
		HibernateUtil.startTransaction();
		
		try {
			UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			HttpServletRequest request = ctx.getHttpServletRequest();
			
			//Retrieving the current user
			User user = userWebAPI.getLoggedInUser(request);
			boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);
			
			PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			Permissionable asset = retrievePermissionable(assetId, user, respectFrontendRoles); 
			
			List<Permission> newSetOfPermissions = new ArrayList<Permission>();
			for(Map<String, String> permission: permissions) {
				String roleId = permission.get("roleId");
				String individualPermission = permission.get("individualPermission");
				if(individualPermission != null ) {
					newSetOfPermissions.add(new Permission(asset.getPermissionId(), roleId, Integer.parseInt(individualPermission), true));
					//If a structure we need to save permissions inheritable by children content
					if(asset instanceof Structure) {
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
				String filesPermission = permission.get("filesPermission");
				if(filesPermission != null) {
					newSetOfPermissions.add(new Permission(File.class.getCanonicalName(), asset.getPermissionId(), roleId, 
							Integer.parseInt(filesPermission), true));
				}
				String pagesPermission = permission.get("pagesPermission");
				if(pagesPermission != null) {
					newSetOfPermissions.add(new Permission(HTMLPage.class.getCanonicalName(), asset.getPermissionId(), roleId, 
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
			}
			
			if(newSetOfPermissions.size() > 0) {
				permissionAPI.assignPermissions(newSetOfPermissions, asset, user, respectFrontendRoles);
			
				if(reset && asset.isParentPermissionable()) {
					ResetPermissionsJob.triggerJobImmediately(asset);
				}					
			} else {
				permissionAPI.removePermissions(asset);
			}
			
		} catch (DotDataException e) {
			HibernateUtil.rollbackTransaction();
			throw e;
		} catch (DotSecurityException e) {
			HibernateUtil.rollbackTransaction();
			throw e;
		} catch (PortalException e) {
			HibernateUtil.rollbackTransaction();
			throw e;
		} catch (SystemException e) {
			HibernateUtil.rollbackTransaction();
			throw e;
		}

		HibernateUtil.commitTransaction();

	}
	
	public void resetAssetPermissions (String assetId) throws DotDataException, PortalException, SystemException, DotSecurityException {
		HibernateUtil.startTransaction();
		try {
			
			UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			HttpServletRequest request = ctx.getHttpServletRequest();
			
			//Retrieving the current user
			User user = userWebAPI.getLoggedInUser(request);
			boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

			PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			Permissionable asset = retrievePermissionable(assetId, user, respectFrontendRoles);
			permissionAPI.removePermissions(asset);
			
		} catch (DotDataException e) {
			HibernateUtil.rollbackTransaction();
			throw e;
		}
		HibernateUtil.commitTransaction();
	}
	
	@SuppressWarnings("unchecked")
	private Permissionable retrievePermissionable (String assetId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		HostAPI hostAPI = APILocator.getHostAPI();
		
		Permissionable perm = null;
		
		//Determining the type
		
		try{
		//Host?
			perm = hostAPI.find(assetId, user, respectFrontendRoles);
		}
		catch(Exception e){
			
		}
		if(perm == null) {
			//Content?
			ContentletAPI contAPI = APILocator.getContentletAPI();
			try {
				perm = contAPI.findContentletByIdentifier(assetId, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, respectFrontendRoles);
			} catch (DotContentletStateException e) {
			}
		}
		
		if(perm == null) {
			
			DotConnect dc = new DotConnect();
			ArrayList results = new ArrayList();
			String assetType ="";
			dc.setSQL("Select asset_type from identifier where id =?");
			dc.addParam(assetId);
			ArrayList assetResult = dc.loadResults();
			if(assetResult.size()>0){
				assetType = (String) ((Map)assetResult.get(0)).get("asset_type");
			}
			if(UtilMethods.isSet(assetType)){
				dc.setSQL("select i.inode, type from inode i,"+assetType+" a where i.inode = a.inode and a.identifier = ?");
				dc.addParam(assetId);
				results = dc.loadResults();
			}
			if(results.size() > 0) {
				String type =  (String) ((Map)results.get(0)).get("type");
				String inode = (String) ((Map)results.get(0)).get("inode");
				perm = InodeFactory.getInode(inode, InodeUtils.getClassByDBType(type));
			}
			
		}

		if(perm == null || !UtilMethods.isSet(perm.getPermissionId())) {
			perm = InodeFactory.getInode(assetId, Inode.class);
		}
		return perm;
	}
	
	  public void permissionIndividually(String assetId) throws Exception {
			HibernateUtil.startTransaction();
	    	try {
	    		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
	    		WebContext ctx = WebContextFactory.get();
	    		HttpServletRequest request = ctx.getHttpServletRequest();

	    		//Retrieving the current user
	    		User user = userWebAPI.getLoggedInUser(request);
	    		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

	    		PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	    		Permissionable asset = retrievePermissionable(assetId, user, respectFrontendRoles);
	    		Permissionable parentPermissionable = APILocator.getPermissionAPI().findParentPermissionable(asset);
	    		
	    		if(parentPermissionable!=null){
	    			permissionAPI.permissionIndividually(parentPermissionable, asset, user, respectFrontendRoles);
	    		}
	    		HibernateUtil.commitTransaction();
	    	} catch (Exception e) {
	    		HibernateUtil.rollbackTransaction();
	    		throw e;
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
			Structure hostStrucuture = StructureCache.getStructureByVelocityVarName("Host");
			
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
