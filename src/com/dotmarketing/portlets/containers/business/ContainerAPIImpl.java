package com.dotmarketing.portlets.containers.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.TemplateContainers;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BaseWebAssetAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class ContainerAPIImpl extends BaseWebAssetAPI implements ContainerAPI {

	protected PermissionAPI permissionAPI;
	protected ContainerFactory containerFactory;
	protected HostAPI hostAPI;

	public ContainerAPIImpl () {
		permissionAPI = APILocator.getPermissionAPI();
		containerFactory = FactoryLocator.getContainerFactory();
		hostAPI = APILocator.getHostAPI();
	}

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

        //Copy the structure relationship
        Structure st = StructureCache.getStructureByInode(source.getStructureInode());
        newContainer.setStructureInode(st.getInode());

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


		/*TreeFactory.saveTree(new Tree(destination.getIdentifier(), newContainer.getInode()));

		//Copy the structure relationship
        Structure st = (Structure) InodeFactory.getParentOfClass(source, Structure.class);
        TreeFactory.saveTree(new Tree(st.getInode(), newContainer.getInode()));*/



		//Copy permissions
		permissionAPI.copyPermissions(source, newContainer);

		//saves to working folder under velocity
		ContainerServices.invalidate(newContainer, newIdentifier, true);

		return newContainer;
	}

	private void save(Container container) throws DotDataException {
		containerFactory.save(container);
	}

	private void save(Container container, String existingId) throws DotDataException {
		containerFactory.save(container, existingId);
	}

	protected void save(WebAsset webAsset) throws DotDataException {
		save((Container) webAsset);
	}

	protected void save(WebAsset webAsset, String existingId) throws DotDataException {
		save((Container) webAsset, existingId);
	}

	@SuppressWarnings("unchecked")
	private String getAppendToContainerTitle(String containerTitle, Host destination) throws DotDataException {
		String temp = new String(containerTitle);
		String result = "";
		HibernateUtil dh = new HibernateUtil(Container.class);
		String sql = "SELECT {containers.*} from containers, inode containers_1_, identifier ident, container_version_info vv "+
					 "where vv.identifier=ident.id and vv.working_inode=containers.inode and containers.inode = containers_1_.inode and " +
					 "containers.identifier = ident.id and host_inode = ? order by title ";
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

	@SuppressWarnings("unchecked")
	public Container getWorkingContainerById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		HibernateUtil dh = new HibernateUtil(Container.class);
		dh.setSQLQuery("select {containers.*} from containers, inode containers_1_, container_version_info vv " +
				"where containers.inode = containers_1_.inode and vv.working_inode=containers.inode and " +
				"vv.identifier = ?");
		dh.setParam(id);
		//dh.setParam(true);
		List<Container> list = dh.list();

		if(list.size() == 0)
			return null;

		Container container = list.get(0);

		if (!permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if(InodeUtils.isSet(container.getInode()))
			return container;
		else
			return null;
	}


	@SuppressWarnings("unchecked")
	public Container getLiveContainerById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		HibernateUtil dh = new HibernateUtil(Container.class);
		dh.setSQLQuery("select {containers.*} from containers, inode containers_1_, container_version_info vv " +
				"where containers.inode = containers_1_.inode and vv.live_inode=containers.inode and " +
				"vv.identifier = ?");
		dh.setParam(id);
		//dh.setParam(true);
		List<Container> list = dh.list();

		if(list.size() == 0)
			return null;

		Container container = list.get(0);

		if (!permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if(InodeUtils.isSet(container.getInode()))
			return container;
		else
			return null;
	}


	/**
	 *
	 * Retrieves the children working containers attached to the given template
	 *
	 * @param parentTemplate
	 * @return
	 * @author David H Torres
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
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


	/**
 	 *
 	 * Retrieves a list of container-structure relationships by container
 	 *
 	 * @param container
 	 * @return
 	 * @throws DotSecurityException
 	 * @throws DotDataException
 	 * @throws DotStateException
 	 *
 	 */
 	@SuppressWarnings("unchecked")
 	public List<ContainerStructures> getContainerStructures(Container container) throws DotStateException, DotDataException, DotSecurityException  {
 
 		HibernateUtil dh = new HibernateUtil(ContainerStructures.class);
 		dh.setSQLQuery("select {container_structures.*} from container_structures where container_structures.container_id = ?");
 		dh.setParam(container.getIdentifier());
 		return dh.list();
 	}

	/**
	 * Retrieves all the containers attached to the given host
	 * @param parentPermissionable
	 * @return
	 * @throws DotDataException
	 *
	 */
	public List<Container> findContainersUnder(Host parentPermissionable) throws DotDataException {
		return containerFactory.findContainersUnder(parentPermissionable);
	}

	@SuppressWarnings("unchecked")
	public Container save(Container container, Structure structure, Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
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

		if((structure != null && !existingInode) && !permissionAPI.doesUserHavePermission(structure, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to use the structure.");
		}

		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write on the given host.");
		}

		String userId = user.getUserId();

		// Associating the current structure
		if ((structure != null) && InodeUtils.isSet(structure.getInode()))
			container.setStructureInode(structure.getInode());
			//TreeFactory.saveTree(new Tree(structure.getInode(), container.getInode()));

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
		if(existingInode)
            save(container, container.getInode());
        else
            save(container);

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

        //Saving the host of the templatecontainers
        //TreeFactory.saveTree(new Tree(host.getIdentifier(), container.getInode()));

		// saves to working folder under velocity
		ContainerServices.invalidate(container, true);

		return container;
	}

	public boolean delete(Container container, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		if(permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			return deleteAsset(container);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}

	}

	public List<Container> findAllContainers(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Container> containers = containerFactory.findAllContainers();
		return permissionAPI.filterCollection(containers, PermissionAPI.PERMISSION_USE, respectFrontendRoles, user);
	}

	public Host getParentHost(Container cont, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return hostAPI.findParentHost(cont, user, respectFrontendRoles);
	}

	public List<Container> findContainers(User user, boolean includeArchived,
			Map<String, Object> params, String hostId,String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {
		return containerFactory.findContainers(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}

	public List<Container> findContainersForStructure(String structureInode) throws DotDataException {
	    return containerFactory.findContainersForStructure(structureInode);
	}

    @Override
    public int deleteOldVersions(Date assetsOlderThan) throws DotStateException, DotDataException {
        return deleteOldVersions(assetsOlderThan,"containers");
    }

}