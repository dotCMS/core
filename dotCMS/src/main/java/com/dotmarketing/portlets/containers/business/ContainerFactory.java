package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

/**
 * Persistence component for Containers
 */
public interface ContainerFactory {
	
	/**
	 * Save container into a persistent repository
	 * 
	 * @param container
	 * @throws DotDataException
	 */
	void save(Container container) throws DotDataException;

    /**
     * Save an existing container
     * @param container
     * @param existingId
     * @throws DotDataException
     */
	void save(Container container, String existingId) throws DotDataException;
	
    /**
     * Finds all containers attached to a host
     * @param parentPermissionable
     * @return
     * @throws DotDataException
     */
    List<Container> findContainersUnder(Host parentPermissionable) throws DotDataException;

    /**
     * Retrieves all registered containers in the system
	 * @param currentHost {@link Host} the host is needed since the file asset container on the same host should not include the host in the path, the rest will required it
     * @return List of Container
     * @throws DotDataException 
     */
    List<Container> findAllContainers(final Host currentHost) throws DotDataException;
	
	/**
	 * Retrieves a paginated list of containers the user can use
	 * It will retrieve first the db container and them the folder containers.
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
	 * @return List of Containers
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	List<Container> findContainers(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;

	/**
	 * Get a container based on a folder (non-db)
	 * A Folder could be consider as a container if:
	 * 1) is inside the /application/containers
	 * 2) has a file asset called container.vtl
	 * @param host {@link Host}
	 * @param folder {@link Folder}
	 * @param user   {@link User}
	 * @param showLive {@link Boolean}
	 * @param includeHostOnPath {@link Boolean} true if wants to include the host on the container path
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getContainerByFolder(final Host host, final Folder folder, final User user, final boolean showLive, final boolean includeHostOnPath) throws DotSecurityException, DotDataException;

	/**
	 * Get working container by folder path
	 * @param path {@link String} p
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param respectFrontEndPermissions {@link Boolean}
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getWorkingContainerByFolderPath(final String path, final Host host, final User user,
													 final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException;


	/**
	 * Get live container by folder path
	 * @param path {@link String} p
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param respectFrontEndPermissions {@link Boolean}
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getLiveContainerByFolderPath(String path, Host host, User user,
										   boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException;

    /**
     * Find container associated to the structure
     * @param structureIdentifier String
     * @return List
     * @throws DotDataException
     */
	List<Container> findContainersForStructure(String structureIdentifier) throws DotDataException;

	/**
	 * Search Containers associated with a specific Structure
	 * @param structureIdentifier
	 * @param workingOrLiveOnly True to filter the Containers if the version associated with the Structure is live or Working only
	 * @return list of container
	 * @throws DotDataException
	 */
	List<Container> findContainersForStructure(String structureIdentifier, boolean workingOrLiveOnly) throws DotDataException;
	
    /**
	 * Method will replace user references of the given userId in containers 
	 * with the replacement user id   
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
    void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException;

    /**
     * Finds a container by Inode
     * @param inode {@link String}
     * @return Container
     * @throws DotDataException
     * @throws DotSecurityException
     */
	Container find(String inode) throws DotDataException, DotSecurityException;
}