package com.dotcms.enterprise.priv;

import com.dotcms.enterprise.license.LicenseLevel;
import org.elasticsearch.action.search.SearchResponse;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.enterprise.ParentProxy;
import com.dotmarketing.business.DotStateException;
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
	
}
