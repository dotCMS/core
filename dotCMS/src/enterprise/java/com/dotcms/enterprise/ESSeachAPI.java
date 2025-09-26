/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.elasticsearch.action.search.SearchResponse;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * Provides access to low-level ElasticSearch queries. This is an
 * Enterprise-only feature. These kinds of searches are extremely fast given
 * that querying the index is faster and less expensive than performing more
 * complex database queries.
 * 
 * @author Jonathan Gamba
 * @since Feb 17, 2015
 * 
 */
public interface ESSeachAPI {

	/**
	 * Executes a given Elastic Search query.
	 *
	 * @param esQuery
	 *            - The query that will be executed.
	 * @param live
	 *            - If {@code true}, only live content will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param respectFrontendRoles
	 *            -
	 * @return The result object.
	 * @throws DotSecurityException
	 *             The specified user does not have the required permissions to
	 *             perform this action.
	 * @throws DotDataException
	 *             An error occurred when retrieving the data.
	 */
	public <T> ESSearchResults <T> esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException;

	/**
	 * Returns the list of inodes as hits, and does not load the contentlets
	 * from cache.
	 *
	 * @param esQuery
	 *            - The query that will be executed.
	 * @param live
	 *            - If {@code true}, only live content will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param respectFrontendRoles
	 *            -
	 * @return The result object.
	 * @throws DotSecurityException
	 *             The specified user does not have the required permissions to
	 *             perform this action.
	 * @throws DotDataException
	 *             An error occurred when retrieving the data.
	 */
	public SearchResponse esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException;

    /**
     * Returns a list of related content from the index
     * @param contentletIdentifier
     * @param relationshipName
     * @param pullParents
     * @param live
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    SearchResponse esSearchRelated(final String contentletIdentifier, final String relationshipName,
            final boolean pullParents, final boolean live, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    SearchResponse esSearchRelated(Contentlet contentlet,
            String relationshipName, boolean pullParents, boolean live,
            User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException;

    /**
     * Returns a list of related content from the index given a contentlet identifier (using pagination)
     * @param contentletIdentifier
     * @param relationshipName
     * @param pullParents
     * @param live
     * @param user
     * @param respectFrontendRoles
     * @param limit
     * @param offset
     * @param sortBy
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    SearchResponse esSearchRelated(String contentletIdentifier,
            String relationshipName, boolean pullParents, boolean live,
            User user, boolean respectFrontendRoles, int limit, int offset, String sortBy)
            throws DotDataException, DotSecurityException;


    /**
     * Returns a list of related content from the index given a contentlet (using pagination)
     * @param contentlet
     * @param relationshipName
     * @param pullParents
     * @param live
     * @param user
     * @param respectFrontendRoles
     * @param limit
     * @param offset
     * @param sortBy
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    SearchResponse esSearchRelated(Contentlet contentlet,
            String relationshipName, boolean pullParents, boolean live,
            User user, boolean respectFrontendRoles, int limit, int offset, String sortBy)
            throws DotDataException, DotSecurityException;
}
