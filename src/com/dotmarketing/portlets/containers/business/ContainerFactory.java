package com.dotmarketing.portlets.containers.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.liferay.portal.model.User;

public interface ContainerFactory {
	
	/**
	 * Save container into a persistent repository
	 * 
	 * @param container
	 * @throws DotDataException
	 */
	public void save(Container container) throws DotDataException;
	public void save(Container container, String existingId) throws DotDataException;
	
    /**
     * Finds all containers attached to a host
     * @param parentPermissionable
     * @return
     * @throws DotDataException
     */
    public List<Container> findContainersUnder(Host parentPermissionable) throws DotDataException;

    /**
     * Retrieves all registered containers in the system
     * @return
     * @throws DotDataException 
     */
	public List<Container> findAllContainers() throws DotDataException;
	
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

    public List<Container> findContainersForStructure(String structureInode) throws DotDataException;
	
    /**
	 * Method will replace user references of the given userId in containers 
	 * with the replacement user id   
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException;
	
}