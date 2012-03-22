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

package com.liferay.portal.lastmodified;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.util.Validator;

/**
 * <a href="LastModifiedCSS.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.183 $
 *
 */
public class LastModifiedCSS implements Cachable{

	public static void clear() {
		_instance._clear();
	}

	public static String get(String key) {
		return _instance._get(key);
	}

	public static String put(String key, String obj) {
		return _instance._put(key, obj);
	}

	public static String remove(String key) {
		return _instance._remove(key);
	}

	private LastModifiedCSS() {
		_cache = CacheLocator.getCacheAdministrator();
	}

	private void _clear() {
		_cache.flushGroup(primaryGroup);
	}

	private String _get(String key) {
		String obj = null;

		try {
			obj = (String)_cache.get(key,primaryGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return obj;
	}

	private String _put(String key, String obj) {
		if (Validator.isNotNull(key)) {
			_cache.put(key, obj,primaryGroup);
		}
		return obj;
	}

	private String _remove(String key) {
		String obj = null;

		try {
			obj = (String)_cache.get(key,primaryGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		_cache.remove(key,primaryGroup);
		return obj;
	}

	private static String primaryGroup = "LastModifiedCSS";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};
    
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    public String[] getGroups() {
    	return groupNames;
    }
	
    private DotCacheAdministrator _cache = CacheLocator.getCacheAdministrator();
	
	private static LastModifiedCSS _instance = new LastModifiedCSS();

	public void clearCache() {
		clear();
	}
}