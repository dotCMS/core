package com.dotmarketing.portlets.containers.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public interface ContainerAPI {

	/**
	 * Copies container to the specified host
	 *
	 * @param source
	 * @param destination
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Container copy(Container source, Host destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns the working container by the id
	 *
	 * @param id
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Container getWorkingContainerById(String identifier, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns the live container by the id
	 *
	 * @param id
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Container getLiveContainerById(String identifier, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 *
	 * Retrieves the children working containers attached to the given template
	 *
	 * @param parentTemplate
	 * @return
	 * @author David H Torres
	 * @throws DotHibernateException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	public List<Container> getContainersInTemplate(Template parentTemplate) throws DotHibernateException, DotStateException, DotDataException, DotSecurityException;

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
 	public List<ContainerStructure> getContainerStructures(Container container) throws DotStateException, DotDataException, DotSecurityException;
 
 	/**
	 *
	 * Retrieves the list of structures related to the given container
	 *
	 * @param container
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	public List<Structure> getStructuresInContainer(Container container) throws DotStateException, DotDataException, DotSecurityException;

 	/**
	 *
	 * saves a list of container-structure relationships
	 *
	 * @param containerStructureList
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	public void saveContainerStructures(List<ContainerStructure> containerStructureList) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 *
	 * deletes the container-structure relationships for the given container
	 *
	 * @param container
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	public void deleteContainerStructuresByContainer(Container container) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * Retrieves all the containers attached to the given host
	 * @param parentPermissionable
	 * @author David H Torres
	 * @return
	 * @throws DotDataException
	 *
	 */
	public List<Container> findContainersUnder(Host parentHost) throws DotDataException;

	/**
	 * Retrieves the list of all containers in the system
	 * @param parentPermissionable
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Container> findAllContainers(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Save container
	 *
	 * @param container
	 * @param structure
	 * @param host
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Container save(Container container, Structure structure, Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Delete the specified container
	 *
	 * @param container
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	public boolean delete(Container container, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;

	/**
	 * Retrieves the parent host of a container
	 * @throws DotSecurityException
	 */
	public Host getParentHost(Container cont, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves a paginated list of containers the user can use
	 * @param user
	 * @param includeArchived
	 * @param params
	 * @param hostId
	 * @param inode
	 * @param identifier
	 * @param parent
	 * @param offset
	 * @param limit
	 * @param orderBy
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Container> findContainers(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;

	/**
	 * Retrieves containers using the specified structure
	 *
	 * @param structureInode
	 * @return
	 * @throws DotDataException
	 */
    List<Container> findContainersForStructure(String structureInode)
            throws DotDataException;

    public int deleteOldVersions(Date assetsOlderThan) throws DotStateException, DotDataException;

}