package com.dotmarketing.portlets.links.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.links.model.Link;
import com.liferay.portal.model.User;

public interface MenuLinkFactory {
	
	/**
	 * Save menu link into a persistent repository
	 * 
	 * @param menuLink
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws DotStateException 
	 */
	public void save(Link menuLink) throws DotDataException, DotStateException, DotSecurityException;
	
	
    /**
     * Retrieves a paginated list of links the given user can read
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
	public List<Link> findLinks(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;
	
	
	
}