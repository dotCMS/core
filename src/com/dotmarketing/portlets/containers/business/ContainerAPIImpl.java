package com.dotmarketing.portlets.containers.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.TemplateContainers;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BaseWebAssetAPI;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionedWebAssetUtil;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateFactoryImpl;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * Implementation class of the {@link ContainerAPI}.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class ContainerAPIImpl extends BaseWebAssetAPI implements ContainerAPI {

	protected PermissionAPI permissionAPI;
	protected ContainerFactory containerFactory;
	protected HostAPI hostAPI;

	/**
	 * 
	 */
	public ContainerAPIImpl () {
		permissionAPI = APILocator.getPermissionAPI();
		containerFactory = FactoryLocator.getContainerFactory();
		hostAPI = APILocator.getHostAPI();
	}

	@Override
	public Container copy(Container source, Host destination, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(source, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source container.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to wirte in the destination folder.");
		}

		//gets the new information for the template from the request object
		Container newContainer = new Container();

		newContainer.copy(source);

		String appendToName = getAppendToContainerTitle(source.getTitle(), destination);
       	newContainer.setFriendlyName(source.getFriendlyName() + appendToName);
       	newContainer.setTitle(source.getTitle() + appendToName);

        //creates new identifier for this webasset and persists it
		Identifier newIdentifier = APILocator.getIdentifierAPI().createNew(newContainer, destination);

        newContainer.setIdentifier(newIdentifier.getId());
		//persists the webasset
		save(newContainer);

		if(source.isWorking()){
			APILocator.getVersionableAPI().setWorking(newContainer);
		}
		if(source.isLive()){
			APILocator.getVersionableAPI().setLive(newContainer);
		}

		// issue-2093 Copying multiple structures per container
		if(source.getMaxContentlets()>0) {

			List<ContainerStructure> sourceCS = getContainerStructures(source);
			List<ContainerStructure> newContainerCS = new LinkedList<ContainerStructure>();

			for (ContainerStructure oldCS : sourceCS) {
				ContainerStructure newCS = new ContainerStructure();
				newCS.setContainerId(newContainer.getIdentifier());
                newCS.setContainerInode(newContainer.getInode());
				newCS.setStructureId(oldCS.getStructureId());
				newCS.setCode(oldCS.getCode());
				newContainerCS.add(newCS);
			}

			saveContainerStructures(newContainerCS);

		}

		//Copy permissions
		permissionAPI.copyPermissions(source, newContainer);

		//saves to working folder under velocity
		ContainerServices.invalidate(newContainer, newIdentifier, true);

		return newContainer;
	}

	/**
	 * 
	 * @param container
	 * @throws DotDataException
	 */
	private void save(Container container) throws DotDataException {
		containerFactory.save(container);
	}

	/**
	 * 
	 * @param container
	 * @param existingId
	 * @throws DotDataException
	 */
	private void save(Container container, String existingId) throws DotDataException {
		containerFactory.save(container, existingId);
	}

	@Override
	protected void save(WebAsset webAsset) throws DotDataException {
		save((Container) webAsset);
	}

	/**
	 * 
	 * @param webAsset
	 * @param existingId
	 * @throws DotDataException
	 */
	protected void save(WebAsset webAsset, String existingId) throws DotDataException {
		save((Container) webAsset, existingId);
	}

	/**
	 * 
	 * @param containerTitle
	 * @param destination
	 * @return
	 * @throws DotDataException
	 */
	@SuppressWarnings("unchecked")
	private String getAppendToContainerTitle(String containerTitle, Host destination) throws DotDataException {
		String temp = new String(containerTitle);
		String result = "";
		HibernateUtil dh = new HibernateUtil(Container.class);
		String sql = "SELECT {" + Inode.Type.CONTAINERS.getTableName() + ".*} from " + Inode.Type.CONTAINERS.getTableName() + ", inode dot_containers_1_, identifier ident, container_version_info vv "+
					 "where vv.identifier=ident.id and vv.working_inode=" + Inode.Type.CONTAINERS.getTableName() + ".inode and " + Inode.Type.CONTAINERS.getTableName() + ".inode = dot_containers_1_.inode and " +
			          Inode.Type.CONTAINERS.getTableName() + ".identifier = ident.id and host_inode = ? order by title ";
		dh.setSQLQuery(sql);
		dh.setParam(destination.getIdentifier());

		List<Container> containers = dh.list();

		boolean isContainerTitle = false;

		for (; !isContainerTitle;) {
			isContainerTitle = true;
			temp += result;

			for (Container container: containers) {
				if (container.getTitle().equals(temp)) {
					isContainerTitle = false;
					break;
				}
			}

			if (!isContainerTitle)
				result += " (COPY)";
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Container getWorkingContainerById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		//Trying to get the container from Working cache.
		Container container = CacheLocator.getContainerCache().getWorking(id);
		
		//If it is not in cache.
		if(container == null){
			
			//Get container from DB.
			HibernateUtil dh = new HibernateUtil(Container.class);
			
			dh.setSQLQuery("select {" + Inode.Type.CONTAINERS.getTableName() + ".*} from " + Inode.Type.CONTAINERS.getTableName() + ", inode dot_containers_1_, container_version_info vv " +
					"where " + Inode.Type.CONTAINERS.getTableName() + ".inode = dot_containers_1_.inode and vv.working_inode=" + Inode.Type.CONTAINERS.getTableName() + ".inode and " +
					"vv.identifier = ?");
			
			dh.setParam(id);
			
			List<Container> list = dh.list();

			//If DB return something.
			if(!list.isEmpty()){
				Container containerAux = list.get(0);

				if (!permissionAPI.doesUserHavePermission(containerAux, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
					throw new DotSecurityException("You don't have permission to read the source file.");
				}

				if(InodeUtils.isSet(containerAux.getInode())){
					//container is the one we are going to return.
					container = containerAux;
					//Add to cache.
					CacheLocator.getContainerCache().add(container.getIdentifier(), container);
				}
			}
		}
		
		return container;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Container getLiveContainerById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		//Trying to get the container from Live cache.
		Container container = CacheLocator.getContainerCache().getLive(id);

		//If it is not in cache.
		if(container == null){
			
			//Get container from DB.
			HibernateUtil dh = new HibernateUtil(Container.class);
			
			dh.setSQLQuery("select {" + Inode.Type.CONTAINERS.getTableName() + ".*} from " + Inode.Type.CONTAINERS.getTableName() + ", inode dot_containers_1_, container_version_info vv " +
					"where " + Inode.Type.CONTAINERS.getTableName() + ".inode = dot_containers_1_.inode and vv.live_inode=" + Inode.Type.CONTAINERS.getTableName() + ".inode and " +
					"vv.identifier = ?");
			
			dh.setParam(id);
			
			List<Container> list = dh.list();

			//If DB return something.
			if(!list.isEmpty()){
				Container containerAux = list.get(0);

				if (!permissionAPI.doesUserHavePermission(containerAux, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
					throw new DotSecurityException("You don't have permission to read the source file.");
				}

				if(InodeUtils.isSet(containerAux.getInode())){
					//container is the one we are going to return.
					container = containerAux;
					//Add to cache.
					CacheLocator.getContainerCache().add(container.getIdentifier(), container);
				}
			}
		}
		
		return container;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Container> getContainersInTemplate(Template parentTemplate) throws DotStateException, DotDataException, DotSecurityException  {

		HibernateUtil dh = new HibernateUtil(TemplateContainers.class);
		dh.setSQLQuery("select {template_containers_2_.*} from template_containers, identifier template_containers_1_,identifier template_containers_2_ " +
					   "where template_containers.template_id = template_containers_1_.id and " +
					   "template_containers.container_id = template_containers_2_.id " +
					   "and template_containers.template_id = ? ");
		dh.setParam(parentTemplate.getIdentifier());
		List<Identifier> identifiers = dh.list();
		List<Container> containers = new ArrayList<Container>();
		for(Identifier id : identifiers) {
			Container cont = (Container) APILocator.getVersionableAPI().findWorkingVersion(id,APILocator.getUserAPI().getSystemUser(),false);
			containers.add(cont);
		}
		return containers;
	}

	@Override
	public void saveContainerStructures(List<ContainerStructure> containerStructureList) throws DotStateException, DotDataException, DotSecurityException  {
	    
		if(!containerStructureList.isEmpty()) {
			
			boolean local = false;
			
			try{
				try {
					local = HibernateUtil.startLocalTransactionIfNeeded();
				} catch (DotDataException e1) {
					Logger.error(TemplateFactoryImpl.class,e1.getMessage(),e1);
					throw new DotHibernateException("Unable to start a local transaction " + e1.getMessage(), e1);
				}

				//Get one of the relations to get the container id.
				String containerIdentifier = containerStructureList.get(0).getContainerId();
                String containerInode = containerStructureList.get(0).getContainerInode();

				HibernateUtil.delete("from container_structures in class com.dotmarketing.beans.ContainerStructure " +
                        "where container_id = '" + containerIdentifier + "'" +
                        "and container_inode = '" + containerInode+ "'");

				for(ContainerStructure containerStructure : containerStructureList){
					HibernateUtil.save(containerStructure);
				}
				
				//Add the list to the cache.
				//CacheLocator.getContainerCache().clearCache();
				CacheLocator.getContainerCache().remove(containerIdentifier);
				CacheLocator.getContentTypeCache().addContainerStructures(containerStructureList, containerIdentifier, containerInode);
				
			}catch(DotHibernateException e){
				if(local){
					HibernateUtil.rollbackTransaction();
				}
				throw new DotDataException(e.getMessage());

			}finally{
				if(local){
					HibernateUtil.commitTransaction();
				}
			}
		}
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public List<ContainerStructure> getContainerStructures(Container container) throws DotStateException, DotDataException, DotSecurityException  {
		
		//Gets the list from cache.
		List<ContainerStructure> containerStructures = CacheLocator.getContentTypeCache().getContainerStructures(container.getIdentifier(), container.getInode());
		
		//If there is not cache data for that container, go to the DB.
		if(containerStructures == null){
			
			//Run query directly to DB.
			HibernateUtil dh = new HibernateUtil(ContainerStructure.class);
			dh.setSQLQuery("select {container_structures.*} from container_structures " +
                    "where container_structures.container_id = ? " +
                    "and container_structures.container_inode = ?");
			dh.setParam(container.getIdentifier());
            dh.setParam(container.getInode());
			containerStructures = dh.list();
			
			//Add the list to cache. 
			CacheLocator.getContentTypeCache().addContainerStructures(containerStructures, container.getIdentifier(), container.getInode());
		}
		
		return containerStructures;
	}

	@Override
	public List<Structure> getStructuresInContainer(Container container) throws DotStateException, DotDataException, DotSecurityException  {

		List<ContainerStructure> csList =  getContainerStructures(container);
		List<Structure> structureList = new ArrayList<Structure>();

		for (ContainerStructure cs : csList) {
			Structure st = CacheLocator.getContentTypeCache().getStructureByInode(cs.getStructureId());
			structureList.add(st);
		}

		return structureList;
	}

	@Override
	public List<Container> findContainersUnder(Host parentPermissionable) throws DotDataException {
		return containerFactory.findContainersUnder(parentPermissionable);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Container save(Container container, List<ContainerStructure> containerStructureList, Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Container currentContainer = null;
		List<Template> currentTemplates = null;
		Identifier identifier = null;
		boolean existingId=false;
		boolean existingInode=false;

		if(UtilMethods.isSet(container.getInode())) {
            try {
                Container existing=(Container) HibernateUtil.load(Container.class, container.getInode());
                existingInode = existing==null || !UtilMethods.isSet(existing.getInode());
            }
            catch(Exception ex) {
                existingInode=true;
            }
        }

		if (UtilMethods.isSet(container.getIdentifier())) {
		    identifier = APILocator.getIdentifierAPI().find(container.getIdentifier());
		    if(identifier!=null && UtilMethods.isSet(identifier.getId())) {
		        if(!existingInode) {
        		    currentContainer = getWorkingContainerById(container.getIdentifier(), user, respectFrontendRoles);
        			currentTemplates = InodeFactory.getChildrenClass(currentContainer, Template.class);
		        }
		    }
		    else {
		        existingId=true;
		        identifier=null;
		    }
		}

		if ((identifier != null && !existingInode)  && !permissionAPI.doesUserHavePermission(currentContainer, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write the container.");
		}

		for (ContainerStructure cs : containerStructureList) {
			Structure st = CacheLocator.getContentTypeCache().getStructureByInode(cs.getStructureId());
			if((st != null && !existingInode) && !permissionAPI.doesUserHavePermission(st, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to use the structure. Structure Name: " + st.getName());
			}
		}


		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write on the given host.");
		}

		String userId = user.getUserId();
		container.setModUser(user.getUserId());
		container.setModDate(new Date());

		// it saves or updates the asset
		if (identifier != null) {
			container.setIdentifier(identifier.getId());
		} else {
		    Identifier ident= (existingId) ?
		           APILocator.getIdentifierAPI().createNew(container, host, container.getIdentifier()) :
			       APILocator.getIdentifierAPI().createNew(container, host);
			container.setIdentifier(ident.getId());
		}
		if(existingInode){
            save(container, container.getInode());
		}
        else{
            save(container);
        }

		APILocator.getVersionableAPI().setWorking(container);

		// Get templates of the old version so you can update the working
		// information to this new version.
		if (currentTemplates != null) {
			Iterator<Template> it = currentTemplates.iterator();

			// update templates to new version
			while (it.hasNext()) {
				Template parentInode = (Template) it.next();
				TreeFactory.saveTree(new Tree(parentInode.getInode(), container.getInode()));
			}
		}

		// save the container-structure relationships , issue-2093
		for (ContainerStructure cs : containerStructureList) {
			cs.setContainerId(container.getIdentifier());
            cs.setContainerInode(container.getInode());
		}
		saveContainerStructures(containerStructureList);
		// saves to working folder under velocity
		ContainerServices.invalidate(container, true);

		return container;
	}

	@Override
	public boolean delete(Container container, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		if(permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			deleteContainerStructuresByContainer(container);
			return deleteAsset(container);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}

	}

	@Override
	public List<Container> findAllContainers(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		RoleAPI roleAPI = APILocator.getRoleAPI();
		if ((user != null)
				&& roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole())) {
			return containerFactory.findAllContainers();
		} else {
			return PermissionedWebAssetUtil.findContainersForLimitedUser(null,
					null, true, "title", 0, -1, PermissionAPI.PERMISSION_READ,
					user, false);
		}
	}

	@Override
	public Host getParentHost(Container cont, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return hostAPI.findParentHost(cont, user, respectFrontendRoles);
	}

	@Override
	public List<Container> findContainers(User user, boolean includeArchived,
			Map<String, Object> params, String hostId,String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {
		return containerFactory.findContainers(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}

	@Override
	public List<Container> findContainersForStructure(String structureInode) throws DotDataException {
	    return containerFactory.findContainersForStructure(structureInode);
	}

    @Override
    public int deleteOldVersions(Date assetsOlderThan) throws DotStateException, DotDataException {
        return deleteOldVersions(assetsOlderThan, Inode.Type.CONTAINERS.getValue());
    }

	@Override
	public void deleteContainerStructuresByContainer(Container container)
			throws DotStateException, DotDataException, DotSecurityException {

		if(container != null && UtilMethods.isSet(container.getIdentifier()) && UtilMethods.isSet(container.getInode())){
			HibernateUtil.delete("from container_structures in class com.dotmarketing.beans.ContainerStructure " +
                    "where container_id = '" + container.getIdentifier() + "'");
			
			//Remove the list from cache.
			CacheLocator.getContentTypeCache().removeContainerStructures(container.getIdentifier(), container.getInode());
		}
	}

	@Override
	public void deleteContainerContentTypesByContainerInode(final Container container) throws DotStateException, DotDataException {
		if (container != null && UtilMethods.isSet(container.getIdentifier()) && UtilMethods.isSet(container.getInode())) {
			DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM container_structures WHERE container_inode = ? AND container_id = ?");
			dc.addParam(container.getInode());
			dc.addParam(container.getIdentifier());
			dc.loadResult();
			CacheLocator.getContentTypeCache().removeContainerStructures(container.getIdentifier(), container.getInode());
		}
	}

	/**
	 * Method will replace user references of the given userId in containers 
	 * with the replacement user id   
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException{
		containerFactory.updateUserReferences(userId, replacementUserId);
	}
}
