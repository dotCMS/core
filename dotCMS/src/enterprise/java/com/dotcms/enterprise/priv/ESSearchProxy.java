/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
