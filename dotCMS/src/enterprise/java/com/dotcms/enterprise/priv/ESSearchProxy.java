/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.elasticsearch.action.search.SearchResponse;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.enterprise.ParentProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * Provides the developer a single entry point to interact with plain
 * ElasticSearch queries. This proxy class will hide any implementation details
 * related to our ES Search API.
 * 
 * @author Jonathan Gamba
 * @since Feb 17, 2015
 */
public class ESSearchProxy extends ParentProxy implements ESSeachAPI {

	private ESSeachAPI esSearchAPI = null;

    private static int[] allowedVersionLevels = { LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
			LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level };

    @Override
    protected int[] getAllowedVersions () {
        return allowedVersionLevels;
    }

    /**
	 * Returns a single instance of the {@link ESSeachAPI} class.
	 * 
	 * @return A single instance of the ES Search API.
	 */
    private ESSeachAPI instance() {
    	if (null == this.esSearchAPI) {
    		this.esSearchAPI = new ESSearchAPIImpl();
    	}
    	return this.esSearchAPI;
    }

	@Override
	public ESSearchResults esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {
		return this.instance().esSearch(esQuery, live, user, respectFrontendRoles);
	}

    @Override
	public SearchResponse esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {
		return this.instance().esSearchRaw(esQuery, live, user, respectFrontendRoles);
	}

    @Override
    public SearchResponse esSearchRelated(final String contentletIdentifier,
            final String relationshipName, final boolean pullParents, final boolean live,
            final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return this.instance().esSearchRelated(contentletIdentifier, relationshipName, pullParents, live, user,
                respectFrontendRoles);
    }

    @Override
    public SearchResponse esSearchRelated(Contentlet contentlet, String relationshipName,
            boolean pullParents, boolean live, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return this.instance().esSearchRelated(contentlet, relationshipName, pullParents, live, user,
                respectFrontendRoles);
    }

    @Override
    public SearchResponse esSearchRelated(String contentletIdentifier, String relationshipName,
            boolean pullParents, boolean live, User user, boolean respectFrontendRoles, int limit,
            int offset, String sortBy) throws DotDataException, DotSecurityException {
        return this.instance().esSearchRelated(contentletIdentifier, relationshipName, pullParents, live, user,
                respectFrontendRoles, limit, offset, sortBy);
    }

    @Override
    public SearchResponse esSearchRelated(Contentlet contentlet, String relationshipName,
            boolean pullParents, boolean live, User user, boolean respectFrontendRoles, int limit,
            int offset, String sortBy) throws DotDataException, DotSecurityException {
        return this.instance().esSearchRelated(contentlet, relationshipName, pullParents, live, user,
                respectFrontendRoles, limit, offset, sortBy);
    }

}
