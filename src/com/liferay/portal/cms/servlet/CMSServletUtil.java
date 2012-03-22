/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.cms.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.util.Validator;

/**
 * <a href="CMSServletUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class CMSServletUtil implements Cachable{

	public static String HOME_INDEX = "HOME-INDEX";

	public static String HOME_PAGE_NOT_FOUND = "HOME-PAGE-NOT-FOUND";

	public void clearCache() {
		_cache.flushGroup(primaryGroup);
	}

	public static String getContent(String articleId) {
		String content = null;

		if (Validator.isNull(articleId)) {
			articleId = HOME_INDEX;
		}
		try {
			content = (String)_cache.get(articleId, primaryGroup);
		} catch (DotCacheException e) {
			Logger.debug(CMSServletUtil.class, "Cache Entry not found", e);
		}
		return content;
	}

	private static String primaryGroup = "ContentletCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};
    
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    public String[] getGroups() {
    	return groupNames;
    }
	
    private static DotCacheAdministrator _cache = CacheLocator.getCacheAdministrator();
    
	private static final Log _log = LogFactory.getLog(CMSServletUtil.class);
}